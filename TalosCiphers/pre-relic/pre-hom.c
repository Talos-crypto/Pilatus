/*
 * Copyright (c) 2016, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Author:
 *       Lukas Burkhalter <lubu@student.ethz.ch>
 *       Hossein Shafagh <shafagh@inf.ethz.ch>
 *       Pascal Fischli <fischlip@student.ethz.ch>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "pre-hom.h"
#include "ap_base64.h"
#include <limits.h>

//______BSGS______
KHASH_MAP_INIT_STR(HashTable, int)


struct bsgs_table_s {
    khash_t(HashTable) *table;
    gt_t mG, mG_inv;
    uint64_t tablesize;
};
typedef struct bsgs_table_s *bsgs_table_ptr;
typedef struct bsgs_table_s bsgs_table_t[1];

//const char *hex_table = "0123456789ABCDEF";

/*
void bin_to_strhex(char * in, size_t insz, char * out, size_t outsz) {
    unsigned char *pin = in;
    char * pout = out;
    if (2*insz + 1 > outsz){
        return;
    }
    for(; pin < in+insz; pout +=2, pin++){
        pout[0] = hex_table[(*pin>>4) & 0xF];
        pout[1] = hex_table[ *pin     & 0xF];
    }
    out[2*insz] = '\0';
}*/

char* gt_to_str(gt_t e) {
    char *temp, *res;
    int len = 1;
    if(!gt_is_unity(e)) {
        len = gt_size_bin(e,1);
        temp = (char*)calloc((size_t) len, 1);
        gt_write_bin((uint8_t*)temp, len, e, 1);
    } else {
        temp = (char*)calloc((size_t) len, 1);
    }
    res = (char*)malloc((size_t) AP_Base64encode_len(len));
    //bin_to_strhex(temp, len, res, 2*len+1);
    AP_Base64encode(res, temp, len);
    free(temp);
    return res;
}

int bsgs_table_init(uint64_t t_size, gt_t G, bsgs_table_t table) {
    int i, maxit = (int) t_size;
    int ret;
    char* str;
    khiter_t k;
    gt_t cur;
    bn_t table_size_bn;
    int result = STS_ERR;

    gt_null(cur);
    gt_null(table->mG);
    gt_null(table->mG_inv);

    TRY {

        bn_new(table_size_bn);
        gt_new(cur);
        gt_new(table->mG);
        gt_new(table->mG_inv);

        table->tablesize = t_size;

        // Pre-Calculate mG, mG_inv
        bn_set_dig(table_size_bn, t_size);
        gt_exp(table->mG, G, table_size_bn);
        gt_inv(table->mG_inv, table->mG);

        table->table = kh_init(HashTable);

        gt_set_unity(cur);
        /* Create Table with Baby Steps */
        for (i = 0; i < (int) t_size; i++) {
            str = gt_to_str(cur);
            kh_set(HashTable, table->table, str, i);
            gt_mul(cur, cur, G);
        }

        result = STS_OK;
    }
    CATCH_ANY {
        gt_null(table->mG);
        gt_null(table->mG_inv);
        bn_null(table->tablesize);
        kh_destroy(HashTable, table->table);

        result = STS_ERR;
    }
    FINALLY {
        gt_free(cur);
        bn_free(table_size_bn);
    }

    return result;
}

size_t bsgs_table_get_size(bsgs_table_t bsgs_table) {
    size_t s = 0;
    if (bsgs_table->table != NULL) {
        khiter_t k;
        for (k = kh_begin(table); k != kh_end(bsgs_table->table); ++k) {
            if (kh_exist(bsgs_table->table, k)) {
                s += strlen((char*) kh_key(bsgs_table->table, k))+1;
            }
        }
    }
    return s;
}

void bsgs_table_free(bsgs_table_t bsgs_table) {
    if (bsgs_table->table != NULL) {
        khiter_t k;
        for (k = kh_begin(table); k != kh_end(bsgs_table->table); ++k) {
            if (kh_exist(bsgs_table->table, k)) {
                free((char*) kh_key(bsgs_table->table, k));
            }
        }
        kh_destroy(HashTable, bsgs_table->table);
        bsgs_table->table = NULL;
    }
    bn_free(bsgs_table->tablesize);

    gt_free(bsgs_table->mG);
    gt_free(bsgs_table->mG_inv);
}

int bsgs_table_baby_step_giant_step (gt_t M, bn_t x, const dig_t max_it,
                          const char do_signed, bsgs_table_t bsgs_table) {
    dig_t i;
    dis_t i_neg;
    khiter_t k;
    gt_t cur, cur_neg;
    bn_t table_size;
    char* str;
    int result = STS_ERR;
    int found_res;

    gt_null(cur);
    gt_null(cur_neg);

    TRY {

        gt_new(cur);
        gt_new(cur_neg);
        bn_new(table_size);

        /* Giant Steps */
        gt_copy(cur, M);
        gt_copy(cur_neg, M);

        bn_set_dig(table_size, bsgs_table->tablesize);

        for (i = 0, i_neg = 0; i <= max_it; i++, i_neg--){
            str = gt_to_str(cur);//, bsgs_table->str_base);
            k = kh_get(HashTable, bsgs_table->table, str);
            free(str);
            if (k != kh_end(bsgs_table->table)) {
                bn_mul_dig(x, table_size, i);
                found_res = kh_val(bsgs_table->table, k);
                bn_add_dig(x, x, (dig_t) found_res);
                break;
            }
            gt_mul(cur, cur, bsgs_table->mG_inv);
            if (do_signed) {
                //TODO
            }
        }

        if (i >= max_it) {
            printf("bsgs failed\n");
        } else {
            result = STS_OK;
        }
    }
    CATCH_ANY {
        result = STS_ERR;
    }
    FINALLY {
        gt_free(cur);
        gt_free(cur_neg);
    }
    return result;
}

//________________


// Finds the mod inverse of a modulo m
int mod_inverse(bn_t res, bn_t a, bn_t m)
{
    bn_t tempGcd, temp;
    dig_t one = 1;
    int result = STS_ERR;

    bn_null(tempGcd);
    bn_null(temp);

    TRY {

        bn_new(tempGcd);
        bn_new(temp);

        bn_gcd_ext(tempGcd, res, temp, a, m);
        if(bn_sign(res)==BN_NEG) {
            bn_add(res, res, m);
        }
        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
    }
    FINALLY {
        bn_free(tempGcd);
        bn_free(temp);
    }

    return result;
}

// Finds the value x with brute force s.t. M=xG
int solve_dlog_brute(gt_t M, gt_t G, bn_t x, dig_t max_it) {
    gt_t cur;
    bn_t max;
    int result = STS_ERR;

    gt_null(cur);
    bn_null(x);
    bn_null(max);

    TRY {

        gt_new(cur);
        bn_new(max);

        bn_zero(x);
        bn_zero(max);
        bn_add_dig(max, max, max_it);
        gt_set_unity(cur);
        if(gt_is_unity(M)) {
            bn_zero(x);
            result = STS_OK;
        } else {
            for (; bn_cmp(x, max) == CMP_LT; bn_add_dig(x, x, 1)) {
                //gt_exp(cur, G, x);
                gt_mul(cur, cur, G);

                if (gt_cmp(cur, M) == CMP_EQ) {
                    bn_add_dig(x, x, 1);
                    result = STS_OK;
                    break;
                }
            }
            if (bn_cmp(x, max) == CMP_LT) {
                result = STS_OK;
            }
        }

    }
    CATCH_ANY {
        result = STS_ERR;
    }
    FINALLY {
        gt_free(cur);
    }

    return result;
}

int get_gt_generator(gt_t gen) {
    g1_t g1;
    g2_t g2;
    g1_new(g1);
    g2_new(g2);
    g1_get_gen(g1);
    g2_get_gen(g2);
    pc_map(gen, g1, g2);
    g1_free(g1);
    g2_free(g2);
    return STS_OK;
}

// TABLE DEFINITION FOR BSGS
bsgs_table_t *BSGS_TABLE_CACHE = NULL;

int pre_init() {
    if (core_init() != STS_OK) {
        core_clean();
        return STS_ERR;
    }

    if (pc_param_set_any() != STS_OK) {
        THROW(ERR_NO_CURVE);
        core_clean();
        return STS_ERR;
    }
    pc_param_print();
}

int pre_deinit() {
    core_clean();
    if(BSGS_TABLE_CACHE!=NULL) {
        bsgs_table_free(*BSGS_TABLE_CACHE);
        free(BSGS_TABLE_CACHE);
        BSGS_TABLE_CACHE=NULL;
    }
}

int pre_init_bsgs_table(uint64_t size){
    gt_t generator;
    gt_null(generator);
    gt_new(generator);
    get_gt_generator(generator);
    if(BSGS_TABLE_CACHE!=NULL) {
        bsgs_table_free(*BSGS_TABLE_CACHE);
        free(BSGS_TABLE_CACHE);
        BSGS_TABLE_CACHE=NULL;
    }
    BSGS_TABLE_CACHE = (bsgs_table_t*) malloc(sizeof(bsgs_table_t));
    bsgs_table_init(size, generator, *BSGS_TABLE_CACHE);
    gt_free(generator);
}

// Finds a positive random value with a max nr of bits that is smaller than the given max value
void bn_rand_mod_(bn_t res, bn_t max, int bits) {
    //bn_print_in_str(max, "Max: %s\n");

    // The smaller one is taken since being bigger than max makes no sense due to modulo max
    int m_bits = bn_bits(max);
    int min_bits = m_bits < bits ? m_bits : bits;
    do {
        bn_rand(res, BN_POS, min_bits);

        //bn_print_in_str(res, "Res: %s\n");
    }
    while (bn_cmp(res, max) != CMP_LT);
}

// Finds a positive random prime value with a max nr of bits that is smaller than the given max value
void bn_rand_prime_mod(bn_t res, bn_t max, int bits) {
    //bn_print_in_str(max, "Max: %s\n");

    // The smaller one is taken since being bigger than max makes no sense due to modulo max
    int m_bits = bn_bits(max);
    int min_bits = m_bits < bits ? m_bits : bits;
    do {
        bn_gen_prime_basic(res, min_bits);

        //bn_print_in_str(res, "Res: %s\n");
    }
    while (bn_cmp(res, max) != CMP_LT);
}

// cleanup and free sub-structures in 'keys'
int pre_keys_clear(pre_keys_t keys) {
    int result = STS_ERR;

    TRY {
        assert(keys);

        gt_free(keys->Z);
        g1_free(keys->g);
        g1_free(keys->pk);
        g2_free(keys->g2);
        g2_free(keys->pk_2);

        if (keys->type == PRE_REL_KEYS_TYPE_SECRET) {
            bn_free(keys->sk);
        }

        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
    }

    return result;
}

// cleanup and free sub-structures in 'ciphertext' (only if already initialized)
int pre_ciphertext_clear(pre_ciphertext_t ciphertext) {
    int result = STS_ERR;

    TRY {
        assert(ciphertext);
        assert(ciphertext->C1);
        assert(ciphertext->C2_G1 || ciphertext->C2_GT);

        if (ciphertext->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) { // test to detect if the structure is already initialized); could it fail?
            gt_free(ciphertext->C1);
            g1_free(ciphertext->C2_G1);
            ciphertext->group='\0';
        } else  if (ciphertext->group == PRE_REL_CIPHERTEXT_IN_GT_GROUP) {
            gt_free(ciphertext->C1);
            gt_free(ciphertext->C2_GT);
            ciphertext->group='\0';
        }
        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
    }

    return result;
}

/* generate suitable pairing parameters and a pair of public/secret keys: they are stored in the structure 'keys'
 * if composite>0, then it results to n = q1q2, where q1 and q2 are primes with lower orders
 * if composite=0, then n has the same order as the prime q1. This curve is single prime
 */
int pre_generate_keys(pre_keys_t keys) {
    bn_t ord;
    int result = STS_ERR;

    bn_null(ord);
    bn_null(keys->n);
    bn_null(keys->sk);
    g1_null(keys->g);
    g1_null(keys->pk);
    g2_null(keys->g2);
    g2_null(keys->pk_2);
    //g2_null(keys->re_token);
    gt_null(keys->Z);

    TRY {
        bn_new(ord);
        bn_new(keys->sk);
        g1_new(keys->g);
        g1_new(keys->pk);
        g2_new(keys->g2);
        g2_new(keys->pk_2);
        //g2_new(keys->re_token);
        gt_new(keys->Z);

        keys->type=PRE_REL_KEYS_TYPE_SECRET;

        g1_get_ord(ord);

        /* random generator random????*/
        g1_get_gen(keys->g);
        g2_get_gen(keys->g2); // If symmetric, these two generators should be the same

        /* define a random value as secret key and compute the public key as pk = g^sk*/
        bn_rand_mod(keys->sk, ord);

        g1_mul_gen(keys->pk, keys->sk);
        g2_mul_gen(keys->pk_2, keys->sk);

        /* pairing Z = e(g,g)*/
        pc_map(keys->Z, keys->g, keys->g2);

        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;

        bn_null(keys->n);
        bn_null(keys->sk);
        g1_null(keys->g);
        g1_null(keys->pk);
        g2_null(keys->g2);
        g2_null(keys->pk_2);
        //g2_null(keys->re_token);
        gt_null(keys->Z);
    } FINALLY {
        bn_free(ord);
    };

    return result;
}

/* Since we are not storing the Settings, we have borrowed them from the first instance
 * Compute the secret and public key for follow-up users!
 */
int pre_generate_secret_key(pre_keys_t keys) {
    int result = STS_ERR;
    bn_t ord;

    TRY {
        assert(keys);
        bn_new(ord);
        g1_get_ord(ord);

        /* define a random value as secret key and compute the public key as pk = g^sk*/
        bn_rand_mod(keys->sk, ord);

        g1_mul_gen(keys->pk, keys->sk);
        g2_mul_gen(keys->pk_2, keys->sk);

        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
    } FINALLY {
        bn_free(ord);
    };

    return result;
}

/* Generate the re-encryption token towards Bob by means of his public key_b*/
int pre_generate_re_token(pre_re_token_t token, pre_keys_t keys, g2_t pk_2_b) {
    bn_t t, ord;
    int result = STS_ERR;

    bn_null(t);
    TRY {
        assert(keys);
        bn_new(ord);
        //assert(!element_is0(pk_pp_b));

        g1_get_ord(ord);
        g2_new(keys->re_token);
        bn_new(t);

        /* 1/a mod n */
        mod_inverse(t, keys->sk, ord);

        /* g^b ^ 1/a*/
        g2_mul(token->re_token, pk_2_b, t);

        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
        g2_null(keys->re_token);
    }
    FINALLY {
        bn_free(t);
        bn_free(ord);
    }

    return result;
}

/* encrypt the given plaintext (a number) using the public-key in 'keys';
 * the number of bits in the plaintext can be specified of autodetected (plaintext_bits=0)
 * TODO: Currently we are only encrypting into level 2 which allows one re-encryption
 */
int pre_encrypt(pre_ciphertext_t ciphertext, pre_keys_t keys, uint64_t plaintext) {
    bn_t r, m ,ord;
    gt_t t;
    int result = STS_ERR;

    bn_null(r);
    bn_null(m);
    gt_null(t);

    TRY {

        bn_new(r);
        bn_new(ord);
        bn_new(m);
        gt_new(t);

        gt_new(ciphertext->C1);
        g1_new(ciphertext->C2_G1);
        g1_get_ord(ord);

        assert(ciphertext);
        assert(keys);
        assert((keys->type == PRE_REL_KEYS_TYPE_SECRET) || (keys->type == PRE_REL_KEYS_TYPE_ONLY_PUBLIC));

        ciphertext->group=PRE_REL_CIPHERTEXT_IN_G_GROUP;

        /*
         * First Level Encryption
         * c = (c1, c2)     c1, c2 \in G
         *      c1 = Z^ar = e(g,g)^ar = e(g^a,g^r) = e(pk_a, g^r)
         *      c2 = m·Z^r
         */
        assert((int64_t)plaintext >= 0);
        /* Compute C1 part: MZ^r*/
        /* random r in Zn  (re-use r) */
        bn_rand_mod(r, ord);
        if(bn_is_zero(r)) {
            bn_add_dig(r, r, 1);
        }
        /* Z^r */
        gt_exp(ciphertext->C1, keys->Z, r);
        /* Z^m */
        bn_set_dig(m, (dig_t)plaintext);
        if(plaintext == 0) {
            gt_set_unity(t);
        } else {
            gt_exp(t, keys->Z, m);
        }

        /* Z^m+r */
        gt_mul(ciphertext->C1, ciphertext->C1, t);

        /* Compute C2 part: G^ar = pk ^r*/
        /* g^ar = pk^r */
        g1_mul(ciphertext->C2_G1, keys->pk, r);

        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
        gt_null(ciphertext->C1);
        g1_null(ciphertext->C2_G1);
    }
    FINALLY {
        bn_free(r);
        bn_free(m);
        bn_free(ord);
        gt_free(t);
    }

    return result;
}


int pre_decrypt(dig_t *res, pre_keys_t keys, pre_ciphertext_t ciphertext, const char use_bsgs) {
    g2_t t1;
    gt_t t0, t2;
    bn_t t3, t11, ord;
    dig_t max_it;
    int result = STS_ERR;

    bn_null(t3);
    bn_null(t11);
    gt_null(t0);
    g2_null(t1);
    gt_null(t2);

    TRY {

        bn_new(t3);
        bn_new(t11);
        gt_new(t0);
        g2_new(t1);
        gt_new(t2);
        bn_new(ord);

        assert(keys);
        assert(keys->type == PRE_REL_KEYS_TYPE_SECRET);
        assert(ciphertext);
        assert((ciphertext->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) || (ciphertext->group == PRE_REL_CIPHERTEXT_IN_GT_GROUP));

        g1_get_ord(ord);
        /*
         * M = (M.Z^r) / e(G^ar, G^1/a)
         * M = C1 / e(C2, G^1/a)
         */

        /* 1/a mod n */
        mod_inverse(t11, keys->sk, ord);

        if (ciphertext->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
            /* g ^ 1/a*/
            g2_mul(t1, keys->g2, t11);

            /* e(g^ar, g^-a) = Z^r */
            pc_map(t2, ciphertext->C2_G1, t1);
            //pbc_pmesg(3, "Z^r: %B\n", t2);
        } else {
            /* C2 = Z^ar
             * Compute: Z^ar^(1/a)*/
            if(bn_is_zero(t11))  {
                gt_set_unity(t2);
            } else {
                gt_exp(t2, ciphertext->C2_GT, t11);
            }
        }

        /* C1 / e(C2, g^a^-1) or C1/C2^(1/a) */
        gt_inv(t0, t2);
        gt_mul(t2, ciphertext->C1, t0);
        max_it = ULLONG_MAX;
        if(use_bsgs) {
            if(BSGS_TABLE_CACHE==NULL) {
                BSGS_TABLE_CACHE = (bsgs_table_t*) malloc(sizeof(struct bsgs_table_s));
                bsgs_table_init(1L<<16, keys->Z, *BSGS_TABLE_CACHE);
            }
            //TODO change max_it
            if(bsgs_table_baby_step_giant_step(t2, t3, (dig_t) 65536, 0, *BSGS_TABLE_CACHE) == STS_OK) {
                result = STS_OK;
            }
        } else {
            if (solve_dlog_brute(t2, keys->Z, t3, max_it) != STS_OK) {
            } else {
                result = STS_OK;
            }

        }

        bn_get_dig(res, t3);
    }
    CATCH_ANY {
        result = STS_ERR;
    }
    FINALLY {
        gt_free(t0);
        g1_free(t1);
        bn_free(t11);
        bn_free(ord);
        gt_free(t2);
        bn_free(t3);
    }

    return result;
}

/*
 * Given two ciphertexts, homomorphically add them up. No need to
 * differentiate between Lev1 or Lev2 ciphertexts!
 */
int pre_homo_add(pre_keys_t keys, pre_ciphertext_t res, pre_ciphertext_t ciphertext1, pre_ciphertext_t ciphertext2, char re_rand) {
    g1_t tmp_1;
    gt_t tmp;
    bn_t t, ord;
    int result = STS_OK;

    gt_null(tmp);
    bn_null(t);

    TRY {

        //assert(keys);
        //assert((keys->type == PRE_REL_KEYS_TYPE_SECRET) || (keys->type = PRE_REL_KEYS_TYPE_ONLY_PUBLIC));
        assert(res);
        assert(ciphertext1);
        assert(ciphertext2);
        //assert((ciphertext1->group == PRE_REL_CIPHERTEXT_IN_G_GROUP && ciphertext2->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) ||
               //(ciphertext1->group == PRE_REL_CIPHERTEXT_IN_GT_GROUP && ciphertext2->group == PRE_REL_CIPHERTEXT_IN_GT_GROUP));

        //pre_ciphertext_clear(res);

        //gt_new(res->C1);
        bn_new(ord);

        g1_get_ord(ord);

        /*
         * C_a = C_a1, C_a2
         * C_b = C_b1, C_b2
         * C_a+b = C_a1C_b1, C_a2C_b2
         * C_a+b = Z^(m_1 + m_2 + r1 + r2), g^(ar1 + ar2)
         * C_a+b = Z^(m_1 + m_2 + r1 + r2), Z^(ar1 + ar2)
         */
        gt_mul(res->C1, ciphertext1->C1, ciphertext2->C1);
        if (ciphertext1->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
            //g1_new(res->C2_G1);
            g1_add(res->C2_G1, ciphertext1->C2_G1, ciphertext2->C2_G1);
        } else {
            //gt_new(res->C2_GT);
            gt_mul(res->C2_GT, ciphertext1->C2_GT, ciphertext2->C2_GT);
        }
        res->group=ciphertext1->group;

        /* re-randomize
         * random t in Zn
         * C_a+b = Z^(m_1 + m_2 + r1 + r2 + t), g^(ar1 + ar2 + at)
         * C_a+b = Z^(m_1 + m_2 + r1 + r2 + t), Z^(ar1 + ar2 + at)
         */
        if (re_rand) {
            g1_new(tmp_1);
            gt_new(tmp);
            bn_new(t);

            bn_rand_mod(t, ord);
            if(bn_is_zero(t)) {
                bn_add_dig(t, t, 1);
            }

            gt_exp(tmp, keys->Z, t); // (Z^t)
            gt_mul(res->C1, res->C1, tmp);

            if (res->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
                g1_mul(tmp_1, keys->pk, t); // (g^a)^t
                g1_add(res->C2_G1, res->C2_G1, tmp_1);
            } else {
                pc_map(tmp, keys->pk, keys->g2); // e(g^a, g) = e(g, g)^a = Z^a
                gt_exp(tmp, tmp, t); // (Z^a)^t
                gt_mul(res->C2_GT, res->C2_GT, tmp);
            }
        }

        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
        gt_null(res->C1);
        g1_null(res->C2_G1);
        gt_null(res->C2_GT);
    }
    FINALLY {
        if (re_rand) {
            g1_free(tmp_1);
            gt_free(tmp);
            bn_free(t);
        }
        bn_free(ord);
    }

    return result;
}


int pre_re_apply(pre_re_token_t token, pre_ciphertext_t res, pre_ciphertext_t ciphertext) {
    int result;
    TRY {

        assert(token);
        assert(res);
        assert(ciphertext);
        assert(ciphertext->group == PRE_REL_CIPHERTEXT_IN_G_GROUP);
        pre_ciphertext_clear(res);

        gt_new(res->C1);
        gt_new(res->C2_GT);

        gt_copy(res->C1, ciphertext->C1);

        /* level2: C2 = g^ar */
        /* level1: C2 = e(g^ar, g^(b/a) = Z^br*/
        pc_map(res->C2_GT, ciphertext->C2_G1, token->re_token);

        res->group = PRE_REL_CIPHERTEXT_IN_GT_GROUP;
        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
        gt_null(res->C1);
        gt_null(res->C2_GT);
    }

    return result;
}

int get_encoded_token_size(pre_re_token_t token) {
    return g2_size_bin(token->re_token, 1);
}
int encode_token(char* buff, int size, pre_re_token_t token) {
    int size_type = get_encoded_token_size(token);
    if(size<size_type) {
        return STS_ERR;
    }
    g2_write_bin((uint8_t*) buff, size_type, token->re_token, 1);
    return STS_OK;
}
int decode_token(pre_re_token_t token, char* buff, int size) {
    g2_new(token->re_token);
    g2_read_bin(token->re_token,(uint8_t*) buff, size);
    return STS_OK;
}

int get_encoded_key_size(pre_keys_t key) {
    int total_size = 1;
    total_size+= gt_size_bin(key->Z, 1) + ENCODING_SIZE;
    total_size+= g1_size_bin(key->g, 1) + ENCODING_SIZE;
    total_size+= g1_size_bin(key->pk, 1) + ENCODING_SIZE;
    total_size+= g2_size_bin(key->g2, 1) + ENCODING_SIZE;
    total_size+= g2_size_bin(key->pk_2, 1) + ENCODING_SIZE;
    if(key->type==PRE_REL_KEYS_TYPE_SECRET) {
        total_size+=bn_size_bin(key->sk) + ENCODING_SIZE;
    }
    return total_size;
}

void write_size(char* buffer, int size) {
    buffer[0] = (char) ((size>>8) & 0xFF);
    buffer[1] = (char) (size & 0xFF);
}

int read_size(char* buffer) {
    return ((uint8_t) buffer[0]<<8) | ((uint8_t) buffer[1]);
}

int encode_key(char* buff, int size, pre_keys_t key) {
    int size_type = get_encoded_key_size(key), temp_size;
    char* cur_ptr = buff + 1;
    if(size<size_type) {
        return STS_ERR;
    }

    buff[0] = key->type;

    temp_size = gt_size_bin(key->Z, 1);
    write_size(cur_ptr, (u_int16_t) temp_size);
    cur_ptr += ENCODING_SIZE;
    gt_write_bin((uint8_t*) cur_ptr, temp_size, key->Z, 1);
    cur_ptr+=temp_size;

    temp_size = g1_size_bin(key->g, 1);
    write_size(cur_ptr, (u_int16_t) temp_size);
    cur_ptr += ENCODING_SIZE;
    g1_write_bin((uint8_t*) cur_ptr, temp_size, key->g, 1);
    cur_ptr += temp_size;

    temp_size = g1_size_bin(key->pk, 1);
    write_size(cur_ptr, (u_int16_t) temp_size);
    cur_ptr += ENCODING_SIZE;
    g1_write_bin((uint8_t*) cur_ptr, temp_size, key->pk, 1);
    cur_ptr += temp_size;

    temp_size = g2_size_bin(key->g2, 1);
    write_size(cur_ptr, (u_int16_t) temp_size);
    cur_ptr += ENCODING_SIZE;
    g2_write_bin((uint8_t*) cur_ptr, temp_size, key->g2, 1);
    cur_ptr += temp_size;

    temp_size = g2_size_bin(key->pk_2, 1);
    write_size(cur_ptr, (u_int16_t) temp_size);
    cur_ptr += ENCODING_SIZE;
    g2_write_bin((uint8_t*) cur_ptr, temp_size, key->pk_2, 1);
    cur_ptr += temp_size;

    if(key->type==PRE_REL_KEYS_TYPE_SECRET) {
        temp_size = bn_size_bin(key->sk);
        write_size(cur_ptr, temp_size);
        cur_ptr += ENCODING_SIZE;
        bn_write_bin((uint8_t*) cur_ptr, temp_size, key->sk);
    }

    return STS_OK;
}
int decode_key(pre_keys_t key, char* buff, int size) {
    int temp_size, dyn_size = 1;
    char* cur_ptr = buff+1;
    key->type = buff[0];
    if(size<4) {
        return STS_ERR;
    }

    g1_new(key->g);
    g1_new(key->pk);
    g2_new(key->g2);
    g2_new(key->pk_2);
    gt_new(key->Z);

    temp_size = read_size(cur_ptr);
    dyn_size += temp_size + ENCODING_SIZE;
    if(size<dyn_size) {
        return STS_ERR;
    }
    cur_ptr += ENCODING_SIZE;
    gt_read_bin(key->Z, (uint8_t*) cur_ptr, temp_size);
    cur_ptr += temp_size;

    temp_size = read_size(cur_ptr);
    dyn_size += temp_size + ENCODING_SIZE;
    if(size<dyn_size) {
        return STS_ERR;
    }
    cur_ptr += ENCODING_SIZE;
    g1_read_bin(key->g, (uint8_t*) cur_ptr, temp_size);
    cur_ptr += temp_size;

    temp_size = read_size(cur_ptr);
    dyn_size += temp_size + ENCODING_SIZE;
    if(size<dyn_size) {
        return STS_ERR;
    }
    cur_ptr += ENCODING_SIZE;
    g1_read_bin(key->pk, (uint8_t*) cur_ptr, temp_size);
    cur_ptr += temp_size;

    temp_size = read_size(cur_ptr);
    dyn_size += temp_size + ENCODING_SIZE;
    if(size<dyn_size) {
        return STS_ERR;
    }
    cur_ptr += ENCODING_SIZE;
    g2_read_bin(key->g2, (uint8_t*) cur_ptr, temp_size);
    cur_ptr += temp_size;

    temp_size = read_size(cur_ptr);
    dyn_size += temp_size + ENCODING_SIZE;
    if(size<dyn_size) {
        return STS_ERR;
    }
    cur_ptr += ENCODING_SIZE;
    g2_read_bin(key->pk_2, (uint8_t*) cur_ptr, temp_size);
    cur_ptr += temp_size;

    if(key->type==PRE_REL_KEYS_TYPE_SECRET) {
        bn_new(key->sk);
        temp_size = read_size(cur_ptr);
        cur_ptr += ENCODING_SIZE;
        bn_read_bin(key->sk, (uint8_t*) cur_ptr, temp_size);
    }
    return STS_OK;
}
/*
struct pre_ciphertext_s {
    gt_t C1;
    g1_t C2_G1;
    gt_t C2_GT;
    char group;
}*/

int get_encoded_cipher_size(pre_ciphertext_t cipher) {
    int size = 1;
    size += gt_size_bin(cipher->C1, 1) + ENCODING_SIZE;
    if(cipher->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
        size += g1_size_bin(cipher->C2_G1, 1) + ENCODING_SIZE;
    } else {
        size += gt_size_bin(cipher->C2_GT, 1) + ENCODING_SIZE;
    }
    return size;
}
int encode_cipher(char* buff, int size, pre_ciphertext_t cipher){
    int size_type = get_encoded_cipher_size(cipher), temp_size;
    char* cur_ptr = buff+1;
    if(size<size_type) {
        return STS_ERR;
    }
    buff[0] = cipher->group;

    temp_size = gt_size_bin(cipher->C1, 1);
    write_size(cur_ptr, (u_int16_t) temp_size);
    cur_ptr += ENCODING_SIZE;
    gt_write_bin((uint8_t*) cur_ptr, temp_size, cipher->C1, 1);
    cur_ptr += temp_size;

    if(cipher->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
        temp_size = g1_size_bin(cipher->C2_G1, 1);
        write_size(cur_ptr, (u_int16_t) temp_size);
        cur_ptr += ENCODING_SIZE;
        g1_write_bin((uint8_t*) cur_ptr, temp_size, cipher->C2_G1, 1);
    } else {
        temp_size = gt_size_bin(cipher->C2_GT, 1);
        write_size(cur_ptr, (u_int16_t) temp_size);
        cur_ptr += ENCODING_SIZE;
        gt_write_bin((uint8_t*) cur_ptr, temp_size, cipher->C2_GT, 1);
    }
}
int decode_cipher(pre_ciphertext_t cipher, char* buff, int size){
    int temp_size, dyn_size = 1;
    char* cur_ptr = buff+1;
    if(size < 4) {
        return STS_ERR;
    }

    gt_new(cipher->C1);

    cipher->group = buff[0];

    temp_size = read_size(cur_ptr);
    dyn_size += temp_size + ENCODING_SIZE;
    if(size < dyn_size) {
        return STS_ERR;
    }
    cur_ptr += ENCODING_SIZE;
    gt_read_bin(cipher->C1, (uint8_t*) cur_ptr, temp_size);
    cur_ptr += temp_size;

    if(cipher->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
        g1_new(cipher->C2_G1);
        temp_size = read_size(cur_ptr);
        dyn_size += temp_size + ENCODING_SIZE;
        if(size < dyn_size) {
            return STS_ERR;
        }
        cur_ptr += ENCODING_SIZE;
        g1_read_bin(cipher->C2_G1, (uint8_t*) cur_ptr, temp_size);
    } else {
        gt_new(cipher->C2_GT);
        temp_size = read_size(cur_ptr);
        dyn_size += temp_size + ENCODING_SIZE;
        if(size < dyn_size) {
            return STS_ERR;
        }
        cur_ptr += ENCODING_SIZE;
        gt_read_bin(cipher->C2_GT, (uint8_t*) cur_ptr, temp_size);
    }

    return STS_OK;
}

int pre_cipher_clear(pre_ciphertext_t cipher) {
    gt_free(cipher->C1);
    if(cipher->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
        g1_free(cipher->C2_G1);
    } else {
        gt_free(cipher->C2_GT);
    }
    return STS_OK;
}
int pre_token_clear(pre_re_token_t token) {
    g2_free(token->re_token);
    return STS_OK;
}

int pre_ciphertext_init(pre_ciphertext_t ciphertext, char group) {
    gt_new(ciphertext->C1);
    if (group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
        g1_new(ciphertext->C2_G1);
    } else {
        gt_new(ciphertext->C2_GT);
    }
    ciphertext->group = group;
}