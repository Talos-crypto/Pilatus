//
// Created by Lukas Burkhalter on 15.07.16.
//

#include "key_hom.h"
#include <string.h>

int KEY_HOM_BLOCKSIZE = 30;

int KEY_HOM_KEYSIZE = 16;

size_t combine_buffers(unsigned char **out, unsigned char *in1, size_t length_in1, unsigned char *in2,
                       size_t length_in2) {
    size_t tot = length_in1 + length_in2;
    unsigned char *temp = (unsigned char *) malloc(tot);
    *out = temp;
    temp = temp + length_in1;
    memcpy(*out, in1, length_in1);
    memcpy(temp, in2, length_in2);
    return tot;
}

int EC_POINT_multiply_const(EC_GROUP *curve_group, const EC_POINT *in, const BIGNUM *n, EC_POINT *res) {
    BIGNUM *bn1;
    BN_CTX *ctx;
    bn1 = BN_new();
    ctx = BN_CTX_new();
    BN_zero(bn1);
    EC_POINT_mul(curve_group, res, bn1, in, n, ctx);
    BN_free(bn1);
    BN_CTX_free(ctx);
    return 0;
}

int EC_GROUP_get_prime(EC_GROUP *group, BIGNUM *prime) {
    BIGNUM *a, *b;
    BN_CTX *ctx;
    ctx = BN_CTX_new();
    a = BN_new();
    b = BN_new();
    EC_GROUP_get_curve_GFp(group, prime, a, b, ctx);
    BN_free(a);
    BN_free(b);
    BN_CTX_free(ctx);
    return 0;
}

int EC_GROUP_get_order_from_group(EC_GROUP *group, BIGNUM *order) {
    BN_CTX *ctx;
    ctx = BN_CTX_new();
    EC_GROUP_get_order(group, order, ctx);
    BN_CTX_free(ctx);
    return 0;
}

int compute_rand_offset(EC_GROUP *group, int digest_len) {
    int primeBytes;
    BIGNUM *prime = BN_new();
    EC_GROUP_get_prime(group, prime);
    primeBytes = BN_num_bytes(prime);
    BN_free(prime);
    return primeBytes - digest_len - 1;
}


/**
 * Maps binary msg to a Point
 * i.e looks for Point with x-Cord (rand|message)
 */
int msg2GfpECPoint(EC_GROUP *group, unsigned char *msg, size_t msg_len, int num_rand, EC_POINT *result) {
    BN_CTX *ctx = BN_CTX_new();
    RC4_KEY rc4_key;
    unsigned char *rand = NULL, *tot;
    size_t size_tot;
    BIGNUM *temp_bn = BN_new();
    rand = (unsigned char *) calloc((size_t) num_rand, sizeof(unsigned char));
    //RC4 used too look for possible points -> rand bits before msg
    RC4_set_key(&rc4_key, (int) msg_len, msg);
    while (1) {
        RC4(&rc4_key, (size_t) num_rand, rand, rand);
        if(rand[0]==0) {
            rand[0] = 0xFF;
        }
        size_tot = combine_buffers(&tot, rand, (size_t) num_rand, msg, msg_len);
        BN_bin2bn(tot, (int) size_tot, temp_bn);
        if (EC_POINT_set_compressed_coordinates_GFp(group, result, temp_bn, 0, ctx)) {
            free(tot);
            break;
        }
        free(tot);
    }
    free(rand);
    BN_free(temp_bn);
    BN_CTX_free(ctx);
    return 0;
}

/**
 * Maps EC-Point back to a messagge, given the random offset of the x-cordinate
 * x-cord is rand | message
 */
int GfpECPoint2msg(EC_GROUP *group, int num_rand, EC_POINT *point, unsigned char **msg, size_t *msg_len) {
    size_t tot_len;
    int iter;
    unsigned char *temp;
    BN_CTX *ctx = BN_CTX_new();
    BIGNUM *x = BN_new(), *y = BN_new();
    EC_POINT_get_affine_coordinates_GFp(group, point, x, y, ctx);
    tot_len = BN_num_bytes(x) * sizeof(unsigned char);
    temp = (unsigned char *) malloc(tot_len);
    tot_len = (size_t) BN_bn2bin(x, temp);
    *msg_len = tot_len - num_rand;
    assert(*msg_len > 0);
    *msg = (unsigned char *) malloc(*msg_len);
    for (iter = 0; iter < *msg_len; iter++) {
        (*msg)[iter] = temp[num_rand + iter];
    }
    free(temp);
    BN_free(x);
    BN_free(y);
    BN_CTX_free(ctx);
    return 0;
}

/**
 * Implements the hash function H(x) (sha-224)
 */
size_t hash_message(unsigned char *in, size_t in_len, unsigned char **digest) {
    *digest = (unsigned char *) malloc(SHA224_DIGEST_LENGTH * sizeof(unsigned char));
    SHA256_CTX ctx;
    SHA224_Init(&ctx);
    SHA224_Update(&ctx, in, in_len);
    SHA224_Final(*digest, &ctx);
    return SHA224_DIGEST_LENGTH;
}

/**
 * Implememnts the F(k,x) = f(H(x))*k, where f maps the hash to an EC-Point
 */
int func_PRF(KHC_Key *key, int negate_key, unsigned char *x, size_t x_len, EC_POINT *res) {
    unsigned char *digest;
    BIGNUM *key_bn = BN_dup(key->key);
    EC_POINT *cur = EC_POINT_new(key->group);
    size_t digest_len;

    //digest=H(x)
    digest_len = hash_message(x, x_len, &digest);
    // cur = f(H(x))
    msg2GfpECPoint(key->group, digest, digest_len, compute_rand_offset(key->group, digest_len), cur);
    free(digest);
    if (negate_key) {
        BN_set_negative(key_bn, 1);
    }

    // res=f(H(x))*k
    EC_POINT_multiply_const(key->group, cur, key_bn, res);
    BN_free(key_bn);
    EC_POINT_free(cur);
    return 0;
}

int KHC_Cipher_clear(KHC_Cipher *cipher) {
    int iter = 0;
    for (; iter < cipher->num_points; iter++) {
        EC_POINT_free((cipher->points)[iter]);
    }
    free(cipher->points);
    free(cipher->iv);
    free(cipher);
    return 0;
}

int KHC_Cipher_encode(EC_GROUP *group, KHC_Cipher *cipher, unsigned char **buffer, size_t *len) {
    if(cipher->num_points<=0) {
        return 1;
    }
    int iter;
    unsigned char *temp_ptr;
    uint16_t iv_len, num_pts;
    BN_CTX *ctx = BN_CTX_new();
    size_t len_point = EC_POINT_point2oct(group, (cipher->points)[0], POINT_CONVERSION_COMPRESSED, NULL, NULL, ctx);
    *len = 4 + cipher->iv_length + ((cipher->num_points) * len_point);
    (*buffer) = (unsigned char *) malloc(*len);

    iv_len = (uint16_t) cipher->iv_length;
    num_pts = (uint16_t) cipher->num_points;
    (*buffer)[0] = (unsigned char) ((iv_len>>8) & 0xFF);
    (*buffer)[1] = (unsigned char) (iv_len & 0xFF);
    (*buffer)[2] = (unsigned char) ((num_pts>>8) & 0xFF);
    (*buffer)[3] = (unsigned char) (num_pts & 0xFF);
    temp_ptr = (*buffer) + 4;

    memcpy(temp_ptr, cipher->iv, cipher->iv_length);
    temp_ptr += cipher->iv_length;

    for(iter=0; iter<cipher->num_points; iter++) {
        EC_POINT_point2oct(group, (cipher->points)[iter], POINT_CONVERSION_COMPRESSED, temp_ptr, len_point, ctx);
        temp_ptr += len_point;
    }

    BN_CTX_free(ctx);
    return 0;
}

int KHC_Cipher_decode(EC_GROUP *group, unsigned char *buffer, size_t len, KHC_Cipher **cipher) {
    if(len<=4) {
        return 1;
    }
    int iter;
    unsigned char *temp_ptr;
    uint16_t iv_len, num_pts;
    size_t len_point;
    BN_CTX *ctx = BN_CTX_new();

    iv_len = ((uint8_t)buffer[0] << 8) | ((uint8_t)buffer[1]);
    num_pts = ((uint8_t)buffer[2] << 8) | ((uint8_t)buffer[3]);
    len_point = (len - 4 - iv_len) / num_pts;
    temp_ptr = buffer + 4;

    (*cipher) = (KHC_Cipher *) malloc(sizeof(KHC_Cipher));
    (*cipher)->iv_length = iv_len;
    (*cipher)->num_points = num_pts;
    (*cipher)->iv = (unsigned char *) calloc(iv_len, sizeof(unsigned char));
    memcpy((*cipher)->iv, temp_ptr, iv_len);
    temp_ptr+=iv_len;
    (*cipher)->points = (EC_POINT **) malloc(sizeof(EC_POINT *) * num_pts);

    for(iter=0; iter<num_pts; iter++) {
        ((*cipher)->points)[iter] = EC_POINT_new(group);
        EC_POINT_oct2point(group, ((*cipher)->points)[iter], temp_ptr, len_point, ctx);
        temp_ptr+=len_point;
    }

    BN_CTX_free(ctx);
    return 0;
}

KHC_Cipher *KHC_Cipher_create(size_t num_iv_bytes, int use_constant) {
    KHC_Cipher *temp = (KHC_Cipher *) malloc(sizeof(KHC_Cipher));
    temp->iv = (unsigned char *) calloc(num_iv_bytes, sizeof(unsigned char));
    if (!use_constant) {
        RAND_bytes(temp->iv, (int) num_iv_bytes);
    }
    temp->iv_length = num_iv_bytes;
    return temp;
}

KHC_Key *KHC_new_key() {
    return KHC_new_key_params(EC_GROUP_new_by_curve_name(NID_X9_62_prime256v1), KEY_HOM_KEYSIZE);
}

KHC_Key *KHC_new_key_params(EC_GROUP *group, int num_bytes) {
    KHC_Key *key = (KHC_Key *) malloc(sizeof(KHC_Key));
    unsigned char *temp = malloc(num_bytes * sizeof(unsigned char));
    key->key = BN_new();
    key->group = group;
    RAND_bytes(temp, num_bytes);
    BN_bin2bn(temp, num_bytes, key->key);
    free(temp);
    return key;
}

int KHC_Key_clear(KHC_Key *key) {
    BN_free(key->key);
    EC_GROUP_free(key->group);
    free(key);
    return 0;
}

int KHC_Key_encode(KHC_Key *key, unsigned char **buffer, size_t *len) {
    unsigned char *temp_ptr;
    uint16_t curve;
    *len = (size_t) BN_num_bytes(key->key) + 2;
    *buffer = (unsigned char *) malloc(*len);
    curve = (uint16_t) EC_GROUP_get_curve_name(key->group);
    (*buffer)[0] = (unsigned char) ((curve>>8) & 0xFF);
    (*buffer)[1] = (unsigned char) (curve & 0xFF);
    temp_ptr = (*buffer) + 2;
    BN_bn2bin(key->key, temp_ptr);

    return 0;
}

int KHC_Key_decode(unsigned char *buffer, size_t len, KHC_Key **key) {
    if(len<=2) {
        return 1;
    }
    BIGNUM * key_bn = BN_new();
    uint16_t curve = (((uint8_t)buffer[0]) << 8) | ((uint8_t)buffer[1]);
    EC_GROUP *group = EC_GROUP_new_by_curve_name((int) curve);
    unsigned char *temp_ptr;

    *key = (KHC_Key *) malloc(sizeof(KHC_Key));
    temp_ptr = buffer + 2;
    BN_bin2bn(temp_ptr, (int) len-2, key_bn);
    (*key)->key = key_bn;
    (*key)->group = group;
    return 0;
}


int KHC_encrypt(unsigned char *pt, size_t pt_len, KHC_Key *key, int use_const_iv, KHC_Cipher **res) {
    BN_CTX *ctx = BN_CTX_new();
    KHC_Cipher *result = KHC_Cipher_create((size_t) KEY_HOM_KEYSIZE, use_const_iv);
    BIGNUM *nonce_bn = BN_new();
    EC_POINT *ec_block, *hash_res;
    unsigned char *cur_block_pos, *buffer;
    size_t bn_len;
    int blocksize = KEY_HOM_BLOCKSIZE, num_blocks, cur_block, reminder;
    int p_fit = 1;

    // compute number of blocks m_j
    num_blocks = ((int) pt_len) / blocksize;
    reminder = ((int) pt_len) % blocksize;
    if (reminder > 0) {
        num_blocks += 1;
        p_fit = 0;
    }

    result->num_points = num_blocks;
    result->points = (EC_POINT **) malloc(sizeof(EC_POINT *) *num_blocks);

    cur_block_pos = pt;
    BN_bin2bn(result->iv, (int) result->iv_length, nonce_bn);
    for (cur_block = 0; cur_block < num_blocks; cur_block++) {
        if (!p_fit && cur_block == num_blocks - 1) {
            blocksize = ((int) pt_len) % blocksize;
        }
        ec_block = EC_POINT_new(key->group);
        hash_res = EC_POINT_new(key->group);
        //map m_j to EC-Point ec_block
        msg2GfpECPoint(key->group, cur_block_pos, (size_t) blocksize, compute_rand_offset(key->group, KEY_HOM_BLOCKSIZE), ec_block);
        bn_len = BN_num_bytes(nonce_bn) * sizeof(unsigned char);
        buffer = (unsigned char *) malloc(bn_len);
        bn_len = (size_t) BN_bn2bin(nonce_bn, buffer);
        // compute hash_res = F(key, iv + j)
        func_PRF(key, 0, buffer, bn_len, hash_res);
        // ec_block = ec_block + hash_res i.e. c_j = M + F(key, iv + j)
        EC_POINT_add(key->group, ec_block, ec_block, hash_res, ctx);
        (result->points)[cur_block] = ec_block;

        EC_POINT_free(hash_res);
        free(buffer);

        cur_block_pos += blocksize;
        BN_add_word(nonce_bn, 1);
    }
    BN_CTX_free(ctx);
    BN_free(nonce_bn);
    *res = result;
    return 0;
}

int KHC_decrypt(KHC_Cipher *cipher, KHC_Key *key, unsigned char **res,
                size_t *res_len) {
    BN_CTX *ctx = BN_CTX_new();
    int blocksize = KEY_HOM_BLOCKSIZE, num_blocks, cur_block;
    size_t bn_len, temp_res_len;
    BIGNUM *nonce_bn = BN_new();
    EC_POINT *temp;
    unsigned char *buffer, *cur_block_ptr, *temp_res;

    num_blocks = cipher->num_points;
    *res = (unsigned char *) malloc((size_t) num_blocks * blocksize);
    *res_len = 0;
    cur_block_ptr = *res;
    BN_bin2bn(cipher->iv, (int) cipher->iv_length, nonce_bn);

    for (cur_block = 0; cur_block < num_blocks; cur_block++) {
        temp = EC_POINT_new(key->group);
        bn_len = BN_num_bytes(nonce_bn) * sizeof(unsigned char);
        buffer = (unsigned char *) malloc(bn_len);
        bn_len = (size_t) BN_bn2bin(nonce_bn, buffer);
        // temp = F(-key, iv +j)
        func_PRF(key, 1, buffer, bn_len, temp);
        // temp = C_j + temp i.e M_j = C_j +  F(-key, iv +j)
        EC_POINT_add(key->group, temp, (cipher->points)[cur_block], temp, ctx);
        //temp_res = f^-1(temp) -> m_j = f-1(M_j)
        GfpECPoint2msg(key->group, compute_rand_offset(key->group, KEY_HOM_BLOCKSIZE), temp, &temp_res, &temp_res_len);

        memcpy(cur_block_ptr, temp_res, temp_res_len);
        free(temp_res);
        free(buffer);
        EC_POINT_free(temp);

        cur_block_ptr += temp_res_len;
        *res_len += temp_res_len;
        BN_add_word(nonce_bn, 1);
    }
    BN_CTX_free(ctx);
    BN_free(nonce_bn);
    return 0;
}

int KHC_re_encrypt(KHC_Cipher *cipher, KHC_Key *re_key, KHC_Cipher **res_cipher) {
    BN_CTX *ctx = BN_CTX_new();
    int num_blocks, cur_block;
    size_t bn_len;
    BIGNUM *nonce_bn = BN_new();
    EC_POINT *temp;
    unsigned char *buffer;
    KHC_Cipher *result;

    num_blocks = cipher->num_points;
    result = (KHC_Cipher *) malloc(sizeof(KHC_Cipher));
    result->num_points = num_blocks;
    result->points = (EC_POINT **) malloc(sizeof(EC_POINT *) * num_blocks);
    result->iv = (unsigned char *) malloc(cipher->iv_length * sizeof(unsigned char));
    memcpy(result->iv, cipher->iv, cipher->iv_length);
    result->iv_length = cipher->iv_length;
    BN_bin2bn(cipher->iv, (int) cipher->iv_length, nonce_bn);

    for (cur_block = 0; cur_block < num_blocks; cur_block++) {
        temp = EC_POINT_new(re_key->group);
        bn_len = BN_num_bytes(nonce_bn) * sizeof(unsigned char);
        buffer = (unsigned char *) malloc(bn_len);
        bn_len = (size_t) BN_bn2bin(nonce_bn, buffer);
        // temp = F(re-key, iv + j)
        func_PRF(re_key, 0, buffer, bn_len, temp);
        // temp = C_j + temp -> C_j,new = C_j,old + F(re-key, iv + j)
        EC_POINT_add(re_key->group, temp, (cipher->points)[cur_block], temp, ctx);
        (result->points)[cur_block] = temp;
        free(buffer);
        BN_add_word(nonce_bn, 1);
    }

    BN_CTX_free(ctx);
    BN_free(nonce_bn);
    *res_cipher = result;
    return 0;
}

int KHC_create_re_enc_token(KHC_Key *from_key, KHC_Key *to_key, KHC_Key **re_key) {
    *re_key = (KHC_Key *) malloc(sizeof(KHC_Key));
    BIGNUM *temp = BN_dup(from_key->key), *order = BN_new();
    BN_CTX *ctx = BN_CTX_new();
    BN_set_negative(temp, 1);
    EC_GROUP_get_order_from_group(from_key->group, order);
    (*re_key)->key = BN_new();
    // re-key = -from_key + to_key mod group_order -> avoid neg numbers
    BN_mod_add((*re_key)->key, temp, to_key->key, order, ctx);
    (*re_key)->group = from_key->group;
    //printf("From: %s to %s result %s\n",BN_bn2dec(from_key->key), BN_bn2dec(to_key->key), BN_bn2dec((*re_key)->key));
    BN_free(temp);
    BN_free(order);
    BN_CTX_free(ctx);
    return 0;
}
