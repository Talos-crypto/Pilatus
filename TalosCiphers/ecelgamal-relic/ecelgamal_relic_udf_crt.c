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

/**
 * Implements a MySQL udf for database aggregation of ec-elgamal ciphers with CRT optimization
 */


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


#ifdef HAVE_DLOPEN

#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_SOLARIS_STYLE_GETHOST)
#endif

#include "ecelgamal-relic.h"

typedef struct {
    uint16_t is_first;
    uint16_t num_partitions;
    gamal_ciphertext_t *cur_sum;
} sum_state;

//Encodes crt ciphers
my_bool encode_crt_ciph(char **out, int *size_out, gamal_ciphertext_t *ciphs, int num_partitions) {
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

//Decodes crt ciphers
my_bool decode_crt_ciph(gamal_ciphertext_t **ciphs, int *num_partitions, char *in, int size_in) {
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

my_bool ECG_SUM_CRT_init(UDF_INIT *initid, UDF_ARGS *args,
                     char *error) {
    sum_state *state;

    if (args->arg_count != 1 ||
        args->arg_type[0] != STRING_RESULT) {
        strcpy(error, "Usage: ECG_SUM(encoded cipher)");
        return 1;
    }

    gamal_init();

    state = (sum_state*) malloc(sizeof(sum_state));
    state->is_first=1;
    state->num_partitions=0;
    initid->ptr = (char *) state;
    return 0;
}


my_bool ECG_SUM_CRT_deinit(UDF_INIT *initid) {
    int iter=0;
    sum_state *state = (sum_state *) initid->ptr;
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

void ECG_SUM_CRT_clear(UDF_INIT *initid, char *is_null, char *error) {
    int iter=0;
    sum_state *state = (sum_state *) initid->ptr;
    for(iter=0; iter<state->num_partitions; iter++) {
        ec_set_infty(((state->cur_sum)[iter])->C1);
        ec_set_infty(((state->cur_sum)[iter])->C2);
    }
}

my_bool ECG_SUM_CRT_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                    char *error) {
    sum_state *state = (sum_state *) initid->ptr;
    size_t encodedLength = (size_t) args->lengths[0];
    char *encoded = (char *) args->args[0];
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
            strcpy(error, "Corrupted data, crt partition missmach");
            return 1;
        }
    }

    for(iter=0; iter<state->num_partitions; iter++) {
        gamal_add((state->cur_sum)[iter], (state->cur_sum)[iter], in[iter]);
        gamal_cipher_clear(in[iter]);
    }
    free(in);
    return 0;
}

char *ECG_SUM_CRT(UDF_INIT *initid, UDF_ARGS *args, char *result,
              unsigned long *length, char *s_null, char *error) {
    char *res;
    int resSize;
    sum_state *state = (sum_state *) initid->ptr;
    encode_crt_ciph(&res, &resSize, state->cur_sum, state->num_partitions);
    *length = (unsigned long) resSize;
    return res;
}


#endif /* HAVE_DLOPEN */