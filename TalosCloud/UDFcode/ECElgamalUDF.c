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
#include <openssl/ec.h>
#include <openssl/bn.h>
#include <openssl/objects.h>    

static int defaultCurve = NID_X9_62_prime192v1;

EC_POINT *convertToPoint(const char* pointHex, EC_GROUP* curveGroup) 
{
  BN_CTX *ctx;
  EC_POINT *res;
  res =  EC_POINT_new(curveGroup);
  ctx = BN_CTX_new();
  EC_POINT_hex2point(curveGroup, pointHex, res, ctx);
  BN_CTX_free(ctx);
  return res;
}

char *convertToString(const EC_POINT *point, EC_GROUP* curveGroup) 
{
  BN_CTX *ctx;
  char *s;
  point_conversion_form_t form = POINT_CONVERSION_COMPRESSED;
  ctx = BN_CTX_new();
  s = EC_POINT_point2hex(curveGroup, point, form, ctx);
  BN_CTX_free(ctx);
  return s;
}

char* encode(EC_POINT *R, EC_POINT *S, EC_GROUP* curveGroup) 
{
  char *p1,*p2;
  char size1,size2;
  char * buff;
  size_t s1,s2,tot;
  p1 = convertToString(R,curveGroup);
  p2 = convertToString(S,curveGroup);  
  s1 = strlen(p1);
  s2 = strlen(p2);
  tot = s1+s2+2;
  buff = (char*) malloc(tot);
  snprintf(buff,tot,"%s%s%s",p1,"?",p2);
  free(p1);
  free(p2);
  return buff;
}

int decode(EC_POINT **R, EC_POINT **S, char* encoded, EC_GROUP* curveGroup, size_t length) 
{
  char *rS,*sS;
  char delimiter[] = "?";
  char* temp = malloc(length+1);
  memcpy (temp, encoded, length);
  temp[length] = '\0';
  rS = strtok(temp, delimiter);
  sS = strtok(NULL, delimiter);
  *R = convertToPoint(rS, curveGroup);
  *S = convertToPoint(sS, curveGroup);
  return 0;
}


typedef struct {
  EC_GROUP *group;
  EC_POINT *curSumR;
  EC_POINT *curSumS;
} ecagg_state;


my_bool ECElGamal_Agr_init(UDF_INIT *initid, UDF_ARGS *args,
                char *error)
{
  EC_POINT* initR;
  EC_POINT* initS;
  ecagg_state* state;

  if (args->arg_count != 1 ||
      args->arg_type[0] != STRING_RESULT )
  {
      strcpy(error, "Usage: ECElGamal_Agr(string ecCipherString)");
      return 1;
  }

  state = malloc(sizeof(ecagg_state));
  state->group = EC_GROUP_new_by_curve_name(defaultCurve);
  initR = EC_POINT_new(state->group);
  initS = EC_POINT_new(state->group);
  EC_POINT_set_to_infinity(state->group, initR);
  EC_POINT_set_to_infinity(state->group, initS);
  state->curSumR = initR;
  state->curSumS = initS;
  initid->ptr = (char*) state;
  return 0;
}

my_bool ECElGamal_Agr_deinit(UDF_INIT *initid)
{
  ecagg_state* state = (ecagg_state*) initid->ptr;
  EC_GROUP_clear_free(state->group);
  EC_POINT_free(state->curSumR);
  EC_POINT_free(state->curSumS);
  free(state);
  return 0;
} 

void ECElGamal_Agr_clear(UDF_INIT *initid, char *is_null, char *error)
{
  ecagg_state* state = (ecagg_state*) initid->ptr;
  EC_POINT_set_to_infinity(state->group, state->curSumR);
  EC_POINT_set_to_infinity(state->group, state->curSumS);
}
 
my_bool ECElGamal_Agr_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                char *error)
{
   // For each row, the current value is added to the sum
  EC_POINT *R,*S;
  BN_CTX *ctx = BN_CTX_new();
  ecagg_state* state = (ecagg_state*) initid->ptr;
  EC_GROUP *curve = state->group;
  char* encoded = (char*) args->args[0];
  R = EC_POINT_new(curve);
  S = EC_POINT_new(curve);
  decode(&R,&S,encoded,curve, (size_t) args->lengths[0]);
  EC_POINT_add(curve,state->curSumR,state->curSumR,R,ctx);
  EC_POINT_add(curve,state->curSumS,state->curSumS,S,ctx);
  BN_CTX_free(ctx);
  return 0;
}
 
char* ECElGamal_Agr(UDF_INIT *initid, UDF_ARGS *args, char *result,
            unsigned long *length, char * s_null, char *error)
{
  char * res;
  ecagg_state* state = (ecagg_state*) initid->ptr;
  result = encode(state->curSumR, state->curSumS, state->group);
  *length = (unsigned long) strlen(result);
  return result;
}  

                                                                  

#endif /* HAVE_DLOPEN */