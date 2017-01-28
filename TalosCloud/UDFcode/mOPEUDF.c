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

typedef ulonglong bigint;

typedef struct {
  bigint opeVal;
  bigint detVal;
  int first;
} mOPE_state;


my_bool mOPE_MAX_init(UDF_INIT *initid, UDF_ARGS *args,
                char *error)
{
  mOPE_state* state;

  if (args->arg_count != 2 ||
      args->arg_type[0] != INT_RESULT ||
      args->arg_type[1] != INT_RESULT )
  {
      strcpy(error, "Usage: mOPE_MAX(detCol, opeCol)");
      return 1;
  }
  state = malloc(sizeof(mOPE_state));
  state->first = 1;
  state->opeVal = 0;
  state->detVal = 0;
  initid->ptr = (char*) state;
  return 0;
}

my_bool mOPE_MAX_deinit(UDF_INIT *initid)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  free(state);
  return 0;
} 

void mOPE_MAX_clear(UDF_INIT *initid, char *is_null, char *error)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  state->first = 1;
  state->opeVal = 0;
  state->detVal = 0;
}
 
my_bool mOPE_MAX_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                char *error)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  bigint detValarg = *((bigint*) args->args[0]);
  bigint opeValarg = *((bigint*) args->args[1]);
  if(state->first) {
    state->opeVal = opeValarg;
    state->detVal = detValarg;
    state->first = 0;
  } else {
    if((state->opeVal)<opeValarg) {
      state->opeVal = opeValarg;
      state->detVal = detValarg;
    }
  }
  return 0;
}
 
bigint mOPE_MAX(UDF_INIT *initid, UDF_ARGS *args, char *result,
            unsigned long *length, char * is_null, char *error)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  if(state->first) {
    *is_null = 1;
  }
  return state->detVal;
}  


my_bool mOPE_MIN_init(UDF_INIT *initid, UDF_ARGS *args,
                char *error)
{
  mOPE_state* state;

  if (args->arg_count != 2 ||
      args->arg_type[0] != INT_RESULT ||
      args->arg_type[1] != INT_RESULT )
  {
      strcpy(error, "Usage: mOPE_MIN(detCol, opeCol)");
      return 1;
  }
  state = malloc(sizeof(mOPE_state));
  state->first = 1;
  state->opeVal = 0;
  state->detVal = 0;
  initid->ptr = (char*) state;
  return 0;
}

my_bool mOPE_MIN_deinit(UDF_INIT *initid)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  free(state);
  return 0;
} 

void mOPE_MIN_clear(UDF_INIT *initid, char *is_null, char *error)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  state->first = 1;
  state->opeVal = 0;
  state->detVal = 0;
}
 
my_bool mOPE_MIN_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
                char *error)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  bigint detValarg = *((bigint*) args->args[0]);
  bigint opeValarg = *((bigint*) args->args[1]);
  if(state->first) {
    state->opeVal = opeValarg;
    state->detVal = detValarg;
    state->first = 0;
  } else {
    if((state->opeVal)>opeValarg) {
      state->opeVal = opeValarg;
      state->detVal = detValarg;
    }
  }
  return 0;
}
 
bigint mOPE_MIN(UDF_INIT *initid, UDF_ARGS *args, char *result,
            unsigned long *length, char * is_null, char *error)
{
  mOPE_state* state = (mOPE_state*) initid->ptr;
  if(state->first) {
    *is_null = 1;
  }
  return state->detVal;
}  
                                                                  

#endif /* HAVE_DLOPEN */