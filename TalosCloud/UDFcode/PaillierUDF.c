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
#define strmov(a,b) stpcpy(a,b)
#define bzero(a,b) memset(a,0,b)
#define memcpy_fixed(a,b,c) memcpy(a,b,c)
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
#include <openssl/bn.h>

char* encode(BIGNUM *b) 
{
    char *res = BN_bn2hex(b);
    return res;
}

BIGNUM* decode(char* in, size_t length)
{
    char* temp = malloc(length+1);
    memcpy (temp, in, length);
    temp[length] = '\0';
    BIGNUM* out = BN_new();
    BN_hex2bn(&out, temp);
    free(temp);
    return out;
}


typedef struct {
    BIGNUM *sum;
    BIGNUM *pubKey;
    char *resultBuff;
} pailagg_state;


my_bool Paillier_agr_init(UDF_INIT *initid, UDF_ARGS *args,
                char *error)
{
    pailagg_state* state;

    if (args->arg_count != 2 ||
        args->arg_type[0] != STRING_RESULT ||  args->arg_type[1] != STRING_RESULT)
    {
        strcpy(error, "Usage: Paillier_agr(string col, string pubkey)");
    return 1;
    }

    state = malloc(sizeof(pailagg_state));
    state->sum = BN_new();
    state->pubKey = decode((char*) args->args[1], (size_t) args->lengths[1]);
    state->resultBuff = NULL;
    initid->ptr = (char*) state;
    return 0;
}

my_bool Paillier_agr_deinit(UDF_INIT *initid)
{
    pailagg_state* state = (pailagg_state*) initid->ptr;
    BN_free(state->sum);
    BN_free(state->pubKey);
    free(state->resultBuff);
    free(state);
    return 0;
} 

void Paillier_agr_clear(UDF_INIT *initid, char *is_null, char *error)
{
    pailagg_state* state = (pailagg_state*) initid->ptr;
    BN_one(state->sum);
}
 
my_bool Paillier_agr_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                char *error)
{
    BIGNUM *num;
    BN_CTX *ctx = BN_CTX_new();
    pailagg_state* state = (pailagg_state*) initid->ptr;
    if(args->args[0] == NULL) {
        num = BN_new();
        BN_one(num);
    } else {
        num = decode((char*) args->args[0], (size_t) args->lengths[0]);
    }

    BN_mod_mul(state->sum, state->sum, num, state->pubKey, ctx);
    BN_CTX_free(ctx);
    BN_free(num);
    return 0;
}
 
char* Paillier_agr(UDF_INIT *initid, UDF_ARGS *args, char *result,
            unsigned long *length, char * s_null, char *error)
{
    pailagg_state* state = (pailagg_state*) initid->ptr;
    state->resultBuff = encode(state->sum);
    *length = (unsigned long) strlen(state->resultBuff);
    return state->resultBuff;
}  

#endif /* HAVE_DLOPEN */