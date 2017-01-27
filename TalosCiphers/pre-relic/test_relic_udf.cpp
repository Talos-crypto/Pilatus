//
// Created by lubu on 25.01.17.
//



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


//
// DEBUG for UDF
//

#include <iostream>
#include <gmpxx.h>
#include <chrono>
#include <iomanip>

extern "C" {
#include "pre-hom.h"
}

static int USE_HARDCODED_CRT_SIZE=-1;
//
// User #includes go here
//

typedef struct {
    uint16_t is_first;
    uint16_t num_ciphers;
    pre_ciphertext_t *cur_sum;
    char group;
} sum_state;

void print_bytes(const char* pBytes, const uint32_t nBytes) {
    char cur;
    int i;
    for ( uint32_t j = 0; j < nBytes; j++ ) {
        cur = pBytes[j];
        for (i = 0; i < 8; i++) {
            printf("%d", !!((cur << i) & 0x80));
        }
        printf(" ");
    }
    printf("\n");
}

void init_sum_state(sum_state *state, int num_ciphers, char group) {
    int iter=0;
    state->is_first = 0;
    state->num_ciphers = num_ciphers;
    state->group = group;
    state->cur_sum = (pre_ciphertext_t *) malloc(num_ciphers * sizeof(pre_ciphertext_t));
    for(;iter<num_ciphers;iter++) {
        gt_new((state->cur_sum)[iter]->C1);
        gt_set_unity((state->cur_sum)[iter]->C1);
        if(group==PRE_REL_CIPHERTEXT_IN_G_GROUP) {
            g1_new((state->cur_sum)[iter]->C2_G1);
            g1_set_infty((state->cur_sum)[iter]->C2_G1);
        } else {
            gt_new((state->cur_sum)[iter]->C2_GT);
            gt_set_unity((state->cur_sum)[iter]->C2_GT);
        }
        (state->cur_sum)[iter]->group=group;
    }
}

void reset_points_sum_state(sum_state *state) {
    int iter=0, num_ciphers=(state->num_ciphers);
    for(;iter<num_ciphers;iter++) {
        gt_set_unity((state->cur_sum)[iter]->C1);
        if((state->cur_sum)[iter]->group == PRE_REL_CIPHERTEXT_IN_G_GROUP) {
            g1_set_infty((state->cur_sum)[iter]->C2_G1);
        } else {
            gt_set_unity((state->cur_sum)[iter]->C2_GT);
        }
    }
}

void free_cipher_array(pre_ciphertext_t *cipher, int num_ciphers) {
    int iter=0;
    for(;iter<num_ciphers;iter++) {
        pre_ciphertext_clear(cipher[iter]);
    }
    free(cipher);
}

void free_sum_state(sum_state *state) {
    if(state->num_ciphers>0) {
        free_cipher_array(state->cur_sum, state->num_ciphers);
    }
    free(state);
}

int decode_ciphers(char *encoded, size_t encoded_len, pre_ciphertext_t **cipher, int *num_ciphers) {
    //hack
    int iter=0;
    char *cur_position = encoded;
    int cur_len = 0, temp;
    if(USE_HARDCODED_CRT_SIZE<=0) {
        *num_ciphers = (int) encoded[0];
        cur_position = encoded + 1;
    } else {
        *num_ciphers = USE_HARDCODED_CRT_SIZE;
    }

    *cipher = (pre_ciphertext_t *) malloc((*num_ciphers) * sizeof(pre_ciphertext_t));
    for(;iter<*num_ciphers;iter++) {

        if(decode_cipher((*cipher)[iter], cur_position, (int) encoded_len)==STS_ERR) {
            return 1;
        }
        temp = get_encoded_cipher_size((*cipher)[iter]);
        cur_len += temp;
        if(cur_len>encoded_len) {
            return 1;
        }
        cur_position+=temp;
    }
    return 0;
}

int encode_ciphers(char **encoded, size_t *encoded_len, pre_ciphertext_t *cipher, int num_ciphers) {
    //hack
    int iter=0;
    char *cur_position;
    int cur_len = 0, temp;

    for(;iter<num_ciphers;iter++) {
        cur_len+=get_encoded_cipher_size(cipher[iter]);
    }

    if(USE_HARDCODED_CRT_SIZE<=0) {
        cur_len++;
    }

    *encoded = (char *) malloc((size_t) cur_len);

    if(USE_HARDCODED_CRT_SIZE<=0) {
        cur_position = (*encoded) + 1;
        (*encoded)[0] = (char) num_ciphers;
    } else {
        cur_position = *encoded;
    }

    for(iter=0 ;iter<num_ciphers;iter++) {
        temp = get_encoded_cipher_size(cipher[iter]);
        if(encode_cipher(cur_position, temp, cipher[iter])==STS_ERR) {
            return 1;
        }
        cur_position += temp;
    }
    *encoded_len = (size_t) cur_len;
    return 0;
}


int PRE_REL_SUM_init(sum_state **state_in) {
    pre_init();
    *state_in = (sum_state*) malloc(sizeof(sum_state));
    (*state_in)->num_ciphers=0;
    (*state_in)->is_first=1;
    return 0;
}

int PRE_REL_SUM_deinit(sum_state **state_in) {
    sum_state *state = *state_in;
    free_sum_state(state);
    pre_deinit();
    return 0;
}

void PRE_REL_SUM_clear(sum_state **state_in) {
    sum_state *state = *state_in;
    if((state->num_ciphers) > ((uint16_t) 0)) {
        reset_points_sum_state(state);
    }
}

void addCiphers(pre_ciphertext_t *cur, pre_ciphertext_t *updated, int num) {
    int iter=0;
    for(;iter<num;iter++) {
        pre_homo_add(NULL, cur[iter], cur[iter], updated[iter], 0);
    }
}



int PRE_REL_SUM_add( char *encoded,  size_t encodedLength, sum_state **state_in) {
    pre_ciphertext_t *in;
    int in_num;
    sum_state *state = *state_in;

    decode_ciphers(encoded, encodedLength, &in, &in_num);

    if(state->is_first) {
        init_sum_state(state, in_num, in[0]->group);
    } else {
        if(in_num!=state->num_ciphers) {
            //nOK
        }
    }
    addCiphers(state->cur_sum, in, in_num);

    free_cipher_array(in, in_num);
    return 0;
}

char *PRE_REL_SUM(unsigned long *length, sum_state **state_in) {
    char *res;
    size_t resSize;
    sum_state *state = *state_in;
    encode_ciphers(&res, &resSize, state->cur_sum, state->num_ciphers);
    *length = (unsigned long) resSize;
    return res;
}

size_t create_cipher_crt(char** temp, uint64_t msg, pre_keys_t key, int num_partitions) {
    size_t res_size = 0;
    pre_ciphertext_t cipher[num_partitions];
    char* res;
    for(int i=0; i<num_partitions; i++) {
        pre_encrypt(cipher[i], key, msg);
    }
    encode_ciphers(temp, &res_size, cipher, num_partitions);
    for(int i=0; i<num_partitions; i++) {
        pre_cipher_clear(cipher[i]);
    }
    return res_size;
}

size_t create_cipher_crt_level2(char** temp, uint64_t msg, pre_keys_t key, pre_re_token_t  token, int num_partitions) {
    size_t res_size = 0;
    pre_ciphertext_t cipher[num_partitions];
    char* res;
    for(int i=0; i<num_partitions; i++) {
        pre_ciphertext_t temp;
        pre_encrypt(temp, key, msg);
        pre_re_apply(token, cipher[i], temp);
        pre_cipher_clear(temp);
    }
    encode_ciphers(temp, &res_size, cipher, num_partitions);
    for(int i=0; i<num_partitions; i++) {
        pre_cipher_clear(cipher[i]);
    }
    return res_size;
}

void test_udf(int num_add, int use_lvl2, int num_partitions) {
    sum_state *state;
    char *temp, *res;
    size_t tmp_size;
    unsigned long res_len;
    int iter = 0;
    pre_keys_t key, key_to;
    pre_re_token_t  token;

    PRE_REL_SUM_init(&state);

    pre_generate_keys(key);
    pre_generate_keys(key_to);
    pre_generate_re_token(token, key, key_to->pk_2);

    PRE_REL_SUM_clear(&state);

    for(iter=0; iter<num_add; iter ++) {
        if(use_lvl2) {
            tmp_size = create_cipher_crt_level2(&temp, (uint64_t) iter, key,  token, num_partitions);
            print_bytes(temp, tmp_size);
            PRE_REL_SUM_add(temp, tmp_size, &state);
            free(temp);
        } else {
            tmp_size = create_cipher_crt(&temp, (uint64_t) iter, key, num_partitions);
            print_bytes(temp, tmp_size);
            PRE_REL_SUM_add(temp, tmp_size, &state);
            free(temp);
        }
    }

    res = PRE_REL_SUM(&res_len, &state);

    PRE_REL_SUM_clear(&state);

    PRE_REL_SUM_deinit(&state);
}


int main(int argc, char* argv[]) {
    test_udf(10, 0, 3);
    return 0;
}