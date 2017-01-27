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

#include <iostream>
#include <gmpxx.h>
extern "C" {
    #include "ecelgamal-relic.h"
}

//For debugging and testing

typedef struct {
    uint16_t is_first;
    uint16_t num_partitions;
    gamal_ciphertext_t *cur_sum;
} sum_state;

int encode_crt_ciph(char **out, int *size_out, gamal_ciphertext_t *ciphs, int num_partitions) {
    int iter=0;
    int lengths[num_partitions];
    int tot_len = 1;
    char *store;

    for(iter=0; iter<num_partitions; iter++) {
        lengths[iter] = get_encoded_ciphertext_size(ciphs[iter]);
        tot_len += lengths[iter];
    }

    *out = (char *) malloc(1 + tot_len);
    (*out)[0] = (char) num_partitions;
    store = *out;
    store += 1;

    for(iter=0; iter<num_partitions; iter++) {
        encode_ciphertext(store, lengths[iter], ciphs[iter]);
        store += lengths[iter];
    }
    *size_out = tot_len;
    return 0;
}

int decode_crt_ciph(gamal_ciphertext_t **ciphs, int *num_partitions, char *in, int size_in) {
    int iter=0;
    char *store;
    int temp_partitions;

    temp_partitions = (int) in[0];
    store = in;
    store += 1;

    *ciphs = (gamal_ciphertext_t *) malloc(temp_partitions * sizeof(gamal_ciphertext_t));

    for(iter=0; iter<temp_partitions; iter++) {
        decode_ciphertext((*ciphs)[iter], store, (int) size_in);
        store += get_encoded_ciphertext_size((*ciphs)[iter]);
    }
    *num_partitions = temp_partitions;
    return 0;
}

sum_state* ECG_SUM_CRT_init(sum_state *state) {


    gamal_init();

    state = (sum_state*) malloc(sizeof(sum_state));
    state->is_first=1;
    state->num_partitions=0;
    return state;
}


int ECG_SUM_CRT_deinit(sum_state *state) {
    int iter=0;
    if(state->num_partitions>0) {
        for(iter=0; iter<state->num_partitions; iter++) {
            gamal_cipher_clear((state->cur_sum)[iter]);
        }
        free(state->cur_sum);
    }
    free(state);
    gamal_deinit();
    return 0;
}

void ECG_SUM_CRT_clear(sum_state *state) {
    int iter=0;
    for(iter=0; iter<state->num_partitions; iter++) {
        ec_set_infty(((state->cur_sum)[iter])->C1);
        ec_set_infty(((state->cur_sum)[iter])->C2);
    }
}

sum_state * ECG_SUM_CRT_add(char *encoded, size_t encodedLength, sum_state *state) {
    gamal_ciphertext_t *in;
    int num_partitions = 0;
    int iter = 0;

    decode_crt_ciph(&in, &num_partitions, encoded, (int) encodedLength);

    if(state->is_first) {
        state->num_partitions = num_partitions;
        state->cur_sum = (gamal_ciphertext_t *) malloc(num_partitions * sizeof(gamal_ciphertext_t));
        for(iter=0; iter<state->num_partitions; iter++) {
            ec_set_infty(((state->cur_sum)[iter])->C1);
            ec_set_infty(((state->cur_sum)[iter])->C2);
        }
        state->is_first=0;
    } else {
        if(state->num_partitions != num_partitions) {

        }
    }

    for(iter=0; iter<state->num_partitions; iter++) {
        gamal_add((state->cur_sum)[iter], (state->cur_sum)[iter], in[iter]);
        gamal_cipher_clear(in[iter]);
    }
    free(in);
    return state;
}

char *ECG_SUM_CRT(sum_state *state) {
    char *res;
    int resSize;
    encode_crt_ciph(&res, &resSize, state->cur_sum, state->num_partitions);
    return res;
}


void test_basic() {
    gamal_key_t key, key_after;
    gamal_ciphertext_t cipher, cipher_after;
    dig_t plain = 14, res, res2;
    char *buff1, *buff2;
    int sz;



    gamal_init();
    gamal_generate_keys(key);
    gamal_encrypt(cipher, key, plain);
    gamal_decrypt(&res, key, cipher, 0);
    std::cout << "before: " << plain << " after; " << res << std::endl;

    sz = get_encoded_key_size(key);
    buff1 = (char*) malloc((size_t) sz);
    encode_key(buff1, sz, key);
    decode_key(key_after, buff1, sz);

    if(ec_cmp(key->Y, key_after->Y)!=CMP_EQ || bn_cmp(key->secret, key_after->secret)!=CMP_EQ) {
        std::cout << "key encode/decode failed "  << std::endl;
    } else {
        std::cout << "key encode/decode ok :) "  << std::endl;
    }

    sz = get_encoded_ciphertext_size(cipher);
    buff2 = (char*) malloc((size_t) sz);
    encode_ciphertext(buff2, sz, cipher);
    decode_ciphertext(cipher_after, buff2, sz);

    if(ec_cmp(cipher->C1, cipher_after->C1)!=CMP_EQ || ec_cmp(cipher->C2, cipher_after->C2)!=CMP_EQ) {
        std::cout << "ciphertext encode/decode failed "  << std::endl;
    } else {
        std::cout << "ciphertext encode/decode ok :) "  << std::endl;
    }

    gamal_init_bsgs_table(1<<16);

    gamal_decrypt(&res2, key, cipher, 1);

    std::cout << "before: " << plain << " after; " << res2 << std::endl;
    gamal_key_clear(key);
    gamal_key_clear(key_after);
    gamal_cipher_clear(cipher);
    gamal_cipher_clear(cipher_after);
}

void test_crt_encode() {
    gamal_key_t key;
    int iter = 0;
    gamal_ciphertext_t cipher[3];
    gamal_ciphertext_t  *cipher_res;
    int res_partitions;
    char *buff;
    int buff_size;
    dig_t tmp = 0;

    gamal_init();
    gamal_generate_keys(key);

    for(iter=0; iter<3; iter++) {
        gamal_encrypt(cipher[iter], key, iter);
    }

    encode_crt_ciph(&buff, &buff_size, cipher, 3);
    decode_crt_ciph(&cipher_res, &res_partitions, buff, buff_size);

    for(iter=0; iter<3; iter++) {
        gamal_decrypt(&tmp, key, cipher_res[iter], 0);
        if(tmp == iter) {
            std::cout << "ok" << std::endl;
        } else {
            std::cout << "not ok" << std::endl;
        }
    }
}

int main() {
    test_basic();
    return 0;
}