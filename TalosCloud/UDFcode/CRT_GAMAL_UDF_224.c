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
// Created by Lukas Burkhalter on 04.12.15.
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
#include <ctype.h>

#ifdef HAVE_DLOPEN

#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_SOLARIS_STYLE_GETHOST)
static pthread_mutex_t LOCK_hostname;
#endif

//
// User #includes go here
//
#include <openssl/ec.h>
#include <openssl/bn.h>
#include <openssl/objects.h>

static int defaultCurve = NID_secp224k1;
static const int32_t ENCODED_POINT_SIZE = 29;

typedef struct {
    EC_GROUP *group;
    int16_t isFirst;
    int16_t numCiphers;
    EC_POINT **curSumR;
    EC_POINT **curSumS;
} sum_state;

void alloc_points_sum_state(sum_state *state) {
    int i;
    state->curSumR = (EC_POINT**) malloc(state->numCiphers * sizeof(EC_POINT *));
    state->curSumS = (EC_POINT**) malloc(state->numCiphers * sizeof(EC_POINT *));
    for (i = 0; i < state->numCiphers; i++) {
        (state->curSumR)[i] = EC_POINT_new(state->group);
        EC_POINT_set_to_infinity(state->group, (state->curSumR)[i]);
        (state->curSumS)[i] = EC_POINT_new(state->group);
        EC_POINT_set_to_infinity(state->group, (state->curSumS)[i]);
    }
}

void init_sum_state(sum_state *state, int numCiphers) {
    state->group = EC_GROUP_new_by_curve_name(defaultCurve);
    state->isFirst = 0;
    state->numCiphers = numCiphers;
    alloc_points_sum_state(state);
}

void reset_points_sum_state(sum_state *state) {
    int i;
    for (i = 0; i < state->numCiphers; i++) {
        EC_POINT_set_to_infinity(state->group, (state->curSumR)[i]);
        EC_POINT_set_to_infinity(state->group, (state->curSumS)[i]);
    }
}

void free_points(EC_POINT **curR, EC_POINT **curS, int numCiphers) {
    int i;
    for (i = 0; i < numCiphers; i++) {
        EC_POINT_free(curR[i]);
        EC_POINT_free(curS[i]);
    }
    free(curR);
    free(curS);
}

void free_sum_state(sum_state *state) {
    if(state->numCiphers>0) {
        EC_GROUP_free(state->group);
        free_points(state->curSumR, state->curSumS, state->numCiphers);
    }

    free(state);
}


int add_points_up(EC_GROUP *curveGroup, EC_POINT *resR[], EC_POINT *resS[], EC_POINT *src1R[], EC_POINT *src1S[],
                  EC_POINT *src2R[], EC_POINT *src2S[], int numPoints) {
    int ind;
    BN_CTX *ctx = BN_CTX_new();
    for (ind = 0; ind < numPoints; ind++) {
        EC_POINT_add(curveGroup, resR[ind], src1R[ind], src2R[ind], ctx);
        EC_POINT_add(curveGroup, resS[ind], src1S[ind], src2S[ind], ctx);
    }
    BN_CTX_free(ctx);
}

char *get_oct_rep(EC_GROUP *curveGroup, EC_POINT *p) {
    BN_CTX *ctx;
    size_t resSize;
    unsigned char *buf, *res;
    point_conversion_form_t form = POINT_CONVERSION_COMPRESSED;
    ctx = BN_CTX_new();
    buf = (unsigned char*) malloc(100);
    resSize = EC_POINT_point2oct(curveGroup, p, form, buf, 100, ctx);
    res = (unsigned char*) malloc(resSize);
    memcpy(res, buf, resSize);
    free(buf);
    BN_CTX_free(ctx);
    if (resSize != ENCODED_POINT_SIZE)
        return -1;
    return res;
}

EC_POINT *get_EcPoint_oct(EC_GROUP *curveGroup, unsigned char *in, size_t size) {
    BN_CTX *ctx;
    EC_POINT *res;
    ctx = BN_CTX_new();
    res = EC_POINT_new(curveGroup);
    EC_POINT_oct2point(curveGroup, res, in, size, ctx);
    BN_CTX_free(ctx);
    return res;
}

int encode_points(EC_GROUP *curveGroup, EC_POINT *resR[], EC_POINT *resS[], int numPoints, char **res,
                  size_t *resSize) {
    int i;
    size_t finalSize;
    EC_POINT *cur;
    char *temp, *curPos, *octetStr;

    finalSize = numPoints * ENCODED_POINT_SIZE * 2;
    temp = (char*) malloc(finalSize);
    curPos = temp;
    for (i = 0; i < numPoints; i++) {
        cur = resR[i];
        octetStr = get_oct_rep(curveGroup, cur);
        memcpy(curPos, octetStr, ENCODED_POINT_SIZE);
        curPos += ENCODED_POINT_SIZE;
        free(octetStr);

        cur = resS[i];
        octetStr = get_oct_rep(curveGroup, cur);
        memcpy(curPos, octetStr, ENCODED_POINT_SIZE);
        curPos += ENCODED_POINT_SIZE;
        free(octetStr);
    }

    *resSize = finalSize;
    *res = temp;
    return 0;
}

int decode_points(EC_GROUP *curveGroup, char *in, size_t size, int32_t numCiphers, EC_POINT ***resR, EC_POINT ***resS) {
    int i;
    EC_POINT **pR, **pS;
    char *curPos = in;

    pR = (EC_POINT**) malloc(sizeof(*pR) * numCiphers);
    pS = (EC_POINT**) malloc(sizeof(*pR) * numCiphers);

    if (size < (numCiphers * ENCODED_POINT_SIZE * 2))
        return -1;

    for (i = 0; i < numCiphers; i++) {
        pR[i] = get_EcPoint_oct(curveGroup, curPos, ENCODED_POINT_SIZE);
        curPos += ENCODED_POINT_SIZE;
        pS[i] = get_EcPoint_oct(curveGroup, curPos, ENCODED_POINT_SIZE);
        curPos += ENCODED_POINT_SIZE;
    }

    *resR = pR;
    *resS = pS;
    return 0;
}

int get_num_points(size_t size) {
    return size / (2 * ENCODED_POINT_SIZE);
}

int check_encoding_size(size_t size) {
    if(size%(ENCODED_POINT_SIZE*2)!=0)
        return -1;
    return 0;
}


my_bool CRT_GAMAL_SUM_224_init(UDF_INIT *initid, UDF_ARGS *args,
                           char *error) {
    sum_state *state;

    if (args->arg_count != 1 ||
        args->arg_type[0] != STRING_RESULT) {
        strcpy(error, "Usage: CRT_GAMAL_SUM_224(encoded cipher)");
        return 1;
    }

    state = (sum_state*) malloc(sizeof(sum_state));
    state->numCiphers=0;
    state->isFirst=1;
    initid->ptr = (char *) state;
    return 0;
}

my_bool CRT_GAMAL_SUM_224_deinit(UDF_INIT *initid) {
    sum_state *state = (sum_state *) initid->ptr;
    free_sum_state(state);
    return 0;
}

void CRT_GAMAL_SUM_224_clear(UDF_INIT *initid, char *is_null, char *error) {
    sum_state *state = (sum_state *) initid->ptr;
    if(state->numCiphers>0) {
        reset_points_sum_state(state);
    }
}

my_bool CRT_GAMAL_SUM_224_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                          char *error) {
    // For each row, the current value is added to the sum
    EC_POINT **curR, **curS;
    int numCiphers;
    sum_state *state = (sum_state *) initid->ptr;
    size_t encodedLength = (size_t) args->lengths[0];
    char *encoded = (char *) args->args[0];

    if(check_encoding_size(encodedLength)) {
        strcpy(error, "Wrong Cipher Size");
        return 1;
    }

    numCiphers = get_num_points(encodedLength);

    if(state->isFirst) {
        init_sum_state(state, numCiphers);
    } else {
        if(numCiphers!=state->numCiphers) {
            strcpy(error, "Number of ciphers missmatch");
            return 1;
        }
    }

    if(decode_points(state->group, encoded, encodedLength, numCiphers, &curR, &curS)) {
        strcpy(error, "Error while decoding");
        return 1;
    }

    add_points_up(state->group,state->curSumR, state->curSumS, state->curSumR, state->curSumS, curR, curS, state->numCiphers);

    free_points(curR, curS, numCiphers);
    return 0;
}

char *CRT_GAMAL_SUM_224(UDF_INIT *initid, UDF_ARGS *args, char *result,
                    unsigned long *length, char *s_null, char *error) {
    char *res;
    size_t resSize;
    sum_state *state = (sum_state *) initid->ptr;
    encode_points(state->group, state->curSumR, state->curSumS, state->numCiphers, &res, &resSize);
    *length = (unsigned long) resSize;
    return res;
}


#endif /* HAVE_DLOPEN */
