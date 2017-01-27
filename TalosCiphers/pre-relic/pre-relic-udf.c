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
// Implements the MySQL UDF for aggregating CRT Ciphers
//

#ifdef STANDARD
/* STANDARD is defined, don't use any mysql functions */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#ifdef __WIN__
typedef unsigned __int64 ulonglong;/* Microsofts 64 bit types */
typedef __int64 longlong;
#else
typedef unsigned long long ulonglong;
typedef long long longlong;
#endif /*__WIN__*/
#else

#include <my_global.h>
#include <my_sys.h>

#if defined(MYSQL_SERVER)
#include <m_string.h>/* To get strmov() */
#else
/* when compiled as standalone */
#include <string.h>

#define strmov(a, b) stpcpy(a,b)
#define bzero(a, b) memset(a,0,b)
#define memcpy_fixed(a, b, c) memcpy(a,b,c)
#endif
#endif

#include <mysql.h>
#include "test_pre_udf.h"


#ifdef HAVE_DLOPEN

#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_SOLARIS_STYLE_GETHOST)
#endif

#include "pre-hom.h"

static int USE_HARDCODED_CRT_SIZE=-1;
//
// User #includes go here
//

/*typedef struct {
    uint16_t is_first;
    int16_t num_ciphers;
    pre_ciphertext_t *cur_sum;
    char group;
} sum_state;*/

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
        if(decode_cipher((*cipher)[iter], cur_position, encoded_len)==STS_ERR) {
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


my_bool PRE_REL_SUM_init(UDF_INIT *initid, UDF_ARGS *args,
                           char *error) {
    sum_state *state;

    if (args->arg_count != 1 ||
        args->arg_type[0] != STRING_RESULT) {
        strcpy(error, "Usage: PRE_REL_SUM(encoded cipher)");
        return 1;
    }

    pre_init();

    state = (sum_state*) malloc(sizeof(sum_state));
    state->num_ciphers=0;
    state->is_first=1;
    initid->ptr = (char *) state;
    return 0;
}

my_bool PRE_REL_SUM_deinit(UDF_INIT *initid) {
    sum_state *state = (sum_state *) initid->ptr;
    free_sum_state(state);
    pre_deinit();
    return 0;
}

void PRE_REL_SUM_clear(UDF_INIT *initid, char *is_null, char *error) {
    sum_state *state = (sum_state *) initid->ptr;
    if(state->num_ciphers>0) {
        reset_points_sum_state(state);
    }
}

void addCiphers(pre_ciphertext_t *cur, pre_ciphertext_t *updated, int num) {
	int iter=0;
    for(;iter<num;iter++) {
        pre_homo_add(NULL, cur[iter], cur[iter], updated[iter], 0);
    }
}

my_bool PRE_REL_SUM_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                          char *error) {
    sum_state *state = (sum_state *) initid->ptr;
    size_t encodedLength = (size_t) args->lengths[0];
    char *encoded = (char *) args->args[0];
    pre_ciphertext_t *in;
    int in_num;

    decode_ciphers(encoded, encodedLength, &in, &in_num);

    if(state->is_first) {
    	init_sum_state(state, in_num, in[0]->group);
    } else {
    	if(in_num!=state->num_ciphers) {
            strcpy(error, "Number of ciphers missmatch");
            return 1;
        }
    }
    addCiphers(state->cur_sum, in, in_num);

    free_cipher_array(in, in_num);
    return 0;
}

char *PRE_REL_SUM(UDF_INIT *initid, UDF_ARGS *args, char *result,
                    unsigned long *length, char *s_null, char *error) {
    char *res;
    size_t resSize;
    sum_state *state = (sum_state *) initid->ptr;
    encode_ciphers(&res, &resSize, state->cur_sum, state->num_ciphers);
    *length = (unsigned long) resSize;
    return res;
}

//Reenchrypt udf

my_bool PRE_REL_REENC_init(UDF_INIT *initid, UDF_ARGS *args,
                           char *error) {
    if (args->arg_count != 2||
        args->arg_type[0] != STRING_RESULT||
        args->arg_type[1] != STRING_RESULT) {
        strcpy(error, "Usage: PRE_REL_SUM(encoded cipher, token)");
        return 1;
    }
    pre_init();
    return 0;
}

char *PRE_REL_REENC(UDF_INIT *initid, UDF_ARGS *args,
                    char *result, unsigned long *length,
                    char *is_null, char *error) {
    size_t cipher_encoded_length = (size_t) args->lengths[0];
    char *cipher_encoded = (char *) args->args[0];
    size_t token_encoded_length = (size_t) args->lengths[1];
    char *token_encoded = (char *) args->args[1];
    pre_re_token_t token;
    pre_ciphertext_t *in, *out;
    int num_ciphers, iter=0;
    size_t resSize;
    char* res;

    decode_token(token, token_encoded, (int) token_encoded_length);
    decode_ciphers(cipher_encoded, cipher_encoded_length, &in, &num_ciphers);
    out = (pre_ciphertext_t *) malloc(num_ciphers*sizeof(pre_ciphertext_t));

    for(iter=0; iter<num_ciphers; iter++) {
        pre_re_apply(token, out[iter], in[iter]);
    }

    encode_ciphers(&res, &resSize, out, num_ciphers);
    *length = (unsigned long) resSize;

    free_cipher_array(in, num_ciphers);
    free_cipher_array(out, num_ciphers);
    pre_token_clear(token);
    return res;
}

my_bool PRE_REL_REENC_deinit(UDF_INIT *initid) {
    pre_deinit();
    return 0;
}

#endif /* HAVE_DLOPEN */
