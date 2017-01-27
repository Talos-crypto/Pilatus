/*
 * Copyright (c) 2016, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Author:
 *       Lukas Burkhalter <lubu@student.ethz.ch>
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

#include "ap_base64.h"
#include "ecelgamal-relic.h"


KHASH_MAP_INIT_STR(HashTable, int)

struct bsgs_table_s {
    khash_t(HashTable) *table;
    ec_t mG, mG_inv;
    dig_t tablesize;
};
typedef struct bsgs_table_s *bsgs_table_ptr;
typedef struct bsgs_table_s bsgs_table_t[1];

char* ec_to_str(ec_t e) {
    char *temp, *res;
    int len = 1;
    if(!ec_is_infty(e)) {
        len = ec_size_bin(e,1);
        temp = (char*)calloc((size_t) len, 1);
        ec_write_bin(temp, len, e, 1);
    } else {
        temp = (char*)calloc((size_t) len, 1);
    }
    res = (char*)malloc((size_t) AP_Base64encode_len(len));
    AP_Base64encode(res, temp, len);
    free(temp);
    return res;
}

int bsgs_table_init(dig_t t_size, bsgs_table_t table) {
    int i, maxit = (int) t_size;
    int ret;
    char* str;
    khiter_t k;
    ec_t cur, G;
    bn_t table_size_bn;
    int result = STS_ERR;

    ec_null(cur);
    ec_null(G);
    ec_null(table->mG);
    ec_null(table->mG_inv);

    TRY {

        bn_new(table_size_bn);
        ec_new(cur);
        ec_new(table->mG);
        ec_new(table->mG_inv);
        ec_new(G);

        ec_curve_get_gen(G);

        table->tablesize = t_size;

        // Pre-Calculate mG, mG_inv
        bn_set_dig(table_size_bn, t_size);
        ec_mul_gen(table->mG,table_size_bn);
        ec_neg(table->mG_inv, table->mG);

        table->table = kh_init(HashTable);

        ec_set_infty(cur);
        /* Create Table with Baby Steps */
        for (i = 0; i < (int) t_size; i++) {
            str = ec_to_str(cur);
            kh_set(HashTable, table->table, str, i);
            ec_add(cur, cur, G);
        }

        result = STS_OK;
    }
    CATCH_ANY {
        ec_null(table->mG);
        ec_null(table->mG_inv);
        ec_null(G);
        bn_null(table->tablesize);
        kh_destroy(HashTable, table->table);

        result = STS_ERR;
    }
    FINALLY {
        ec_free(G);
        ec_free(cur);
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

    ec_free(bsgs_table->mG);
    ec_free(bsgs_table->mG_inv);
}

int bsgs_table_baby_step_giant_step (ec_t M, bn_t x, const dig_t max_it,
                                     const char do_signed, bsgs_table_t bsgs_table) {
    dig_t i;
    dis_t i_neg;
    khiter_t k;
    ec_t cur, cur_neg;
    bn_t table_size;
    char* str;
    int result = STS_ERR;
    int found_res;

    ec_null(cur);
    ec_null(cur_neg);

    TRY {

        ec_new(cur);
        ec_new(cur_neg);
        bn_new(table_size);

        /* Giant Steps */
        ec_copy(cur, M);
        ec_copy(cur_neg, M);

        bn_set_dig(table_size, bsgs_table->tablesize);

        for (i = 0, i_neg = 0; i <= max_it; i++, i_neg--){
            str = ec_to_str(cur);//, bsgs_table->str_base);
            k = kh_get(HashTable, bsgs_table->table, str);
            free(str);
            if (k != kh_end(bsgs_table->table)) {
                bn_mul_dig(x, table_size, i);
                found_res = kh_val(bsgs_table->table, k);
                bn_add_dig(x, x, (dig_t) found_res);
                break;
            }
            ec_add(cur, cur, bsgs_table->mG_inv);
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
        ec_free(cur);
        ec_free(cur_neg);
    }
    return result;
}

// Finds the value x with brute force s.t. M=xG
int solve_dlog_brute(ec_t M, bn_t x, dig_t max_it) {
    ec_t cur, G;
    bn_t max;
    int result = STS_ERR;

    ec_null(cur);
    ec_null(G);
    bn_null(x);
    bn_null(max);

    TRY {

        ec_new(cur);
        ec_new(G);
        bn_new(max);

        bn_zero(x);
        bn_set_dig(max, max_it);
        ec_set_infty(cur);
        ec_curve_get_gen(G);

        if(ec_is_infty(M)) {
            bn_zero(x);
            result = STS_OK;
        } else {
            for (; bn_cmp(x, max) == CMP_LT; bn_add_dig(x, x, 1)) {
                ec_add(cur, cur, G);

                if (ec_cmp(cur, M) == CMP_EQ) {
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
        ec_null(cur);
        ec_null(G);
        bn_null(x);
        bn_null(max);
    }
    FINALLY {
        ec_free(cur);
        ec_free(G);
        bn_free(max);
    }

    return result;
}

// TABLE DEFINITION FOR BSGS
bsgs_table_t *BSGS_TABLE_CACHE = NULL;

int gamal_init() {
    if (core_init() != STS_OK) {
        core_clean();
        return STS_ERR;
    }
    if (ec_param_set_any() != STS_OK) {
        THROW(ERR_NO_CURVE);
        core_clean();
        return STS_ERR;
    }
    ec_param_print();
}
int gamal_deinit() {
    core_clean();
    if(BSGS_TABLE_CACHE!=NULL) {
        bsgs_table_free(*BSGS_TABLE_CACHE);
        free(BSGS_TABLE_CACHE);
        BSGS_TABLE_CACHE=NULL;
    }
}

int gamal_init_bsgs_table(dig_t size) {
    if(BSGS_TABLE_CACHE!=NULL) {
        bsgs_table_free(*BSGS_TABLE_CACHE);
        free(BSGS_TABLE_CACHE);
        BSGS_TABLE_CACHE=NULL;
    }
    BSGS_TABLE_CACHE = (bsgs_table_t*) malloc(sizeof(bsgs_table_t));
    bsgs_table_init(size, *BSGS_TABLE_CACHE);
}

int gamal_key_clear(gamal_key_t keys) {
    ec_free(keys->Y);
    if(!keys->is_public)
        bn_free(keys->secret);
}
int gamal_cipher_clear(gamal_ciphertext_t cipher) {
    ec_free(cipher->C1);
    ec_free(cipher->C2);
}

int gamal_generate_keys(gamal_key_t key) {
    int result;
    bn_t ord;
    bn_new(ord);
    TRY {
        ec_curve_get_ord(ord);
        key->is_public = 0;
        bn_rand_mod(key->secret, ord);
        ec_mul_gen(key->Y, key->secret);
        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
        bn_null(ord);
    } FINALLY {
        bn_free(ord);
    };
    return result;
}

int gamal_encrypt(gamal_ciphertext_t ciphertext, gamal_key_t key, dig_t plaintext) {
    int result;
    bn_t bn_plain, ord, rand;
    ec_t temp;

    bn_null(bn_plain);
    bn_null(ord);
    bn_null(rand);

    TRY {
        bn_new(bn_plain);
        bn_new(ord);
        bn_new(rand);
        ec_new(temp);
        ec_new(ciphertext->C1);
        ec_new(ciphertext->C2);

        ec_curve_get_ord(ord);
        bn_rand_mod(rand, ord);
        bn_set_dig(bn_plain, (dig_t)plaintext);
        ec_mul_gen(temp, bn_plain);
        //C1 = rG
        ec_mul_gen(ciphertext->C1, rand);
        //C2 = mG + rY
        ec_mul_sim_gen(ciphertext->C2, bn_plain, key->Y, rand);

        result = STS_OK;
    }
    CATCH_ANY {
        result = STS_ERR;
        bn_null(bn_plain);
        bn_null(ord);
        bn_null(rand);
        ec_null(M);
    } FINALLY {
        bn_free(ord);
        bn_free(bn_plain);
        bn_free(rand);
    };
    return result;
}

int gamal_decrypt(dig_t *res, gamal_key_t key, gamal_ciphertext_t ciphertext, const char use_bsgs) {
    int result;
    ec_t M;
    bn_t temp;

    ec_null(M);
    bn_null(temp);
    TRY {
        ec_new(M);
        bn_new(temp);

        // M = -x*C1+1*C2 = -x*r*G + M + r*x*G
        ec_mul(M, ciphertext->C1, key->secret);
        ec_sub(M, ciphertext->C2, M);

        if(use_bsgs) {
            if(BSGS_TABLE_CACHE==NULL) {
                BSGS_TABLE_CACHE = (bsgs_table_t*) malloc(sizeof(struct bsgs_table_s));
                bsgs_table_init(1L<<16, *BSGS_TABLE_CACHE);
            }
            if (bsgs_table_baby_step_giant_step(M, temp, (dig_t) 65536, 0, *BSGS_TABLE_CACHE) == STS_OK) {
                bn_get_dig(res, temp);
                result = STS_OK;
            } else {
                result = STS_ERR;
            }
        } else
            if(solve_dlog_brute(M, temp, (dig_t)1<<31) == STS_OK) {
                bn_get_dig(res, temp);
                result = STS_OK;
            } else {
                result = STS_ERR;
            }


    }
    CATCH_ANY {
        result = STS_ERR;
        ec_null(M);
        bn_null(temp);

    } FINALLY {
        ec_free(M);
        bn_free(temp);
    };

    return result;
}

int gamal_cipher_new(gamal_ciphertext_t cipher) {
    ec_new(ciphertext->C1);
    ec_new(ciphertext->C2);
    return STS_OK;
}

int gamal_add(gamal_ciphertext_t res, gamal_ciphertext_t ciphertext1, gamal_ciphertext_t ciphertext2) {
    ec_add(res->C1, ciphertext1->C1, ciphertext2->C1);
    ec_add(res->C2, ciphertext1->C2, ciphertext2->C2);
    return STS_OK;
}

void write_size(char* buffer, int size) {
    buffer[0] = (char) ((size>>8) & 0xFF);
    buffer[1] = (char) (size & 0xFF);
}

int read_size(char* buffer) {
    return ((uint8_t) buffer[0]<<8) | ((uint8_t) buffer[1]);
}

int get_encoded_key_size(gamal_key_t key) {
    int size = 3;
    if(!key->is_public) {
        size += bn_size_bin(key->secret) + 2;
    }
    size += ec_size_bin(key->Y, 1);
    return size;
}
int encode_key(char* buff, int size, gamal_key_t key) {
    char* cur_ptr = buff+1;
    int size_cur;
    if(size<get_encoded_key_size(key)) {
        return STS_ERR;
    }
    buff[0] = key->is_public;

    size_cur = ec_size_bin(key->Y, 1);
    write_size(cur_ptr, size_cur);
    cur_ptr += 2;
    ec_write_bin(cur_ptr, size_cur, key->Y, 1);
    cur_ptr += size_cur;

    if(!key->is_public) {
        size_cur = bn_size_bin(key->secret);
        write_size(cur_ptr, size_cur);
        cur_ptr += 2;
        bn_write_bin(cur_ptr, size_cur, key->secret);
    }
    return STS_OK;
}
int decode_key(gamal_key_t key, char* buff, int size) {
    int dyn_size = 3, cur_size;
    char* cur_ptr = buff + 1;
    if(size<dyn_size)
        return STS_ERR;
    key->is_public = buff[0];
    cur_size = read_size(cur_ptr);
    dyn_size += cur_size;
    cur_ptr += 2;

    if(size<dyn_size)
        return STS_ERR;

    ec_null(key->Y);
    ec_new(key->Y);
    ec_read_bin(key->Y, cur_ptr, cur_size);
    cur_ptr += cur_size;

    if(!key->is_public) {
        dyn_size += 2;
        if(size<dyn_size)
            return STS_ERR;
        cur_size = read_size(cur_ptr);
        cur_ptr += 2;
        dyn_size += cur_size;
        if(size<dyn_size)
            return STS_ERR;
        bn_null(key->secret);
        bn_new(key->secret);
        bn_read_bin(key->secret, cur_ptr, cur_size);
    }
    return STS_OK;
}

int get_encoded_ciphertext_size(gamal_ciphertext_t ciphertext) {
    return ec_size_bin(ciphertext->C1, 1) + ec_size_bin(ciphertext->C2, 1) + 4;
}

int encode_ciphertext(char* buff, int size, gamal_ciphertext_t ciphertext) {
    char* cur_ptr = buff;
    int size_cur;
    if(size < get_encoded_ciphertext_size(ciphertext))
        return STS_ERR;

    size_cur = ec_size_bin(ciphertext->C1, 1);
    write_size(cur_ptr, size_cur);
    cur_ptr += 2;
    ec_write_bin(cur_ptr, size_cur, ciphertext->C1, 1);
    cur_ptr += size_cur;

    size_cur = ec_size_bin(ciphertext->C2, 1);
    write_size(cur_ptr, size_cur);
    cur_ptr += 2;
    ec_write_bin(cur_ptr, size_cur, ciphertext->C2, 1);
    return STS_OK;
}

int decode_ciphertext(gamal_ciphertext_t ciphertext, char* buff, int size) {
    int dyn_size = 4, cur_size;
    char* cur_ptr = buff ;
    if(size<dyn_size)
        return STS_ERR;

    cur_size = read_size(cur_ptr);
    cur_ptr += 2;
    dyn_size += cur_size;

    if(size<dyn_size)
        return STS_ERR;

    ec_null(ciphertext->C1);
    ec_new(ciphertext->C1);
    ec_read_bin(ciphertext->C1, cur_ptr, cur_size);
    cur_ptr += cur_size;

    cur_size = read_size(cur_ptr);
    cur_ptr += 2;
    dyn_size += cur_size;

    if(size<dyn_size)
        return STS_ERR;

    ec_null(ciphertext->C2);
    ec_new(ciphertext->C2);
    ec_read_bin(ciphertext->C2, cur_ptr, cur_size);

    return STS_OK;
}

