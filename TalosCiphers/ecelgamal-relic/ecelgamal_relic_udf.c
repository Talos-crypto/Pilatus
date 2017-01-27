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
 * Implements a MySQL udf for database aggregation of ec-elgamal ciphers
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
    gamal_ciphertext_t cur_sum;
} sum_state;

my_bool ECG_SUM_init(UDF_INIT *initid, UDF_ARGS *args,
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
    gamal_cipher_new(state->cur_sum);
    initid->ptr = (char *) state;
    return 0;
}

my_bool ECG_SUM_deinit(UDF_INIT *initid) {
    sum_state *state = (sum_state *) initid->ptr;
    gamal_cipher_clear(state->cur_sum);
    free(state);
    gamal_deinit();
    return 0;
}

void ECG_SUM_clear(UDF_INIT *initid, char *is_null, char *error) {
    sum_state *state = (sum_state *) initid->ptr;
    ec_set_infty((state->cur_sum)->C1);
    ec_set_infty((state->cur_sum)->C2);

}

my_bool ECG_SUM_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                        char *error) {
    sum_state *state = (sum_state *) initid->ptr;
    size_t encodedLength = (size_t) args->lengths[0];
    char *encoded = (char *) args->args[0];
    gamal_ciphertext_t in;

    decode_ciphertext(in, encoded, (int) encodedLength);

    if(state->is_first) {
        ec_set_infty((state->cur_sum)->C1);
        ec_set_infty((state->cur_sum)->C2);
        state->is_first=0;
    }
    gamal_add(state->cur_sum, state->cur_sum, in);
    gamal_cipher_clear(in);
    return 0;
}

char *ECG_SUM(UDF_INIT *initid, UDF_ARGS *args, char *result,
                  unsigned long *length, char *s_null, char *error) {
    char *res;
    int resSize;
    sum_state *state = (sum_state *) initid->ptr;
    resSize = get_encoded_ciphertext_size(state->cur_sum);
    res = (char*) malloc((size_t) resSize);
    encode_ciphertext(res, resSize, state->cur_sum);
    *length = (unsigned long) resSize;
    return res;
}


#endif /* HAVE_DLOPEN */