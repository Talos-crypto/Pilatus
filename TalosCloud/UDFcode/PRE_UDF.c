/*
 * Copyright (c) 2016, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Author:
 *       Hossein Shafagh <shafagh@inf.ethz.ch>
 *       Pascal Fischli <fischlip@student.ethz.ch>
 *       Lukas Burkhlter <lubu@student.ethz.ch>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must r eproduce the above copyright
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

#define strmov(a, b) stpcpy(a,b)
#define bzero(a, b) memset(a,0,b)
#define memcpy_fixed(a, b, c) memcpy(a,b,c)
#endif
#endif

#include <mysql.h>
#include <ctype.h>



#include <gmp.h>
#include <pbc/pbc.h>

#ifdef HAVE_DLOPEN

//
// User #includes go here
//

#ifndef HEADER_PRE_H
#define HEADER_PRE_H


#define PRE_CIPHERTEXT_IN_G_GROUP '1'
#define PRE_CIPHERTEXT_IN_GT_GROUP '2'

struct pre_ciphertext_s {
	element_t C1;						// cyphertext part 1
	element_t C2;						// cyphertext part 2
	unsigned int plaintext_bits;		// an upperbound to the bit length of the plaintext
	char group;							// flag to indicate the working group
};
typedef struct pre_ciphertext_s *pre_ciphertext_ptr;
typedef struct pre_ciphertext_s pre_ciphertext_t[1];

#endif /*HEADER_PRE_H*/

#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_SOLARIS_STYLE_GETHOST)
static pthread_mutex_t LOCK_hostname;
#endif


typedef struct {
	pairing_t pairing;
	pre_ciphertext_t curSumCipher;
	int16_t isFirst;
} sum_state;

// cleanup and free sub-structures in 'ciphertext' (only if already initialized)
void pre_ciphertext_clear(pre_ciphertext_t ciphertext) {
	assert(ciphertext);
	assert(ciphertext->C1);
	assert(ciphertext->C2);

	if ((ciphertext->group == PRE_CIPHERTEXT_IN_G_GROUP) || (ciphertext->group == PRE_CIPHERTEXT_IN_GT_GROUP) ) { // test to detect if the structure is already initialized); could it fail?
		element_clear(ciphertext->C1);
		element_clear(ciphertext->C2);
		ciphertext->group='\0';
	}
	return;
}

size_t pre_encode(pre_ciphertext_t cipher, char **res)
{
	char *buff;
	unsigned char *c1, *c2;
	unsigned long l1, l2;
	size_t si, s1, s2, s3, s4, tot;

	l1 = (unsigned long)element_length_in_bytes(cipher->C1);
	l2 = (unsigned long)element_length_in_bytes(cipher->C2);
	c1 = (unsigned char*)malloc((size_t)l1);
	c2 = (unsigned char*)malloc((size_t)l2);

	si = snprintf(NULL, 0, "%lu", l1);
	s1 = (size_t)element_to_bytes(c1, cipher->C1);
	s2 = (size_t)element_to_bytes(c2, cipher->C2);
	s3 = sizeof cipher->group;
	s4 = sizeof cipher->plaintext_bits;

	// l1 | \0 | c1 | l2 | \0 | c2 | group | ? | plaintext_bits | \0
	tot = 2*si + s1 + s2 + s3 + s4 + 4;
	buff = (char*) malloc(tot);

	int idx = 0;

	snprintf(buff, si + 1, "%lu", l1);
	idx += si + 1;

	memcpy(buff + idx, c1, s1);
	idx += s1;

	snprintf(buff + idx, si + 1, "%lu", l2);
	idx += si + 1;

	memcpy(buff + idx, c2, s2);
	idx += s2;

	snprintf(buff + idx, s3 + s4 + 2,"%c%s%u",cipher->group, "?", cipher->plaintext_bits);

	*res = buff;

	free(c1);
	free(c2);

	return tot;
}

int pre_decode(pre_ciphertext_t cipher, char* encoded, pairing_t pairing)
{
	unsigned long size, ul;
	char *p_end, *rest;
	unsigned char *c1, *c2, *c3;
	char *c4;
	char delimiter[] = "?";
	size_t rest_size;

	size = strtoul(encoded, &p_end, 10);
	c1 = (unsigned char*)malloc(size + 1);
	memcpy(c1, p_end + 1, size);
	c1[size] = '\0';

	size = strtoul(p_end + size + 1, &p_end, 10);
	c2 = (unsigned char*)malloc(size + 1);
	memcpy(c2, p_end + 1, size);
	c2[size] = '\0';

	rest_size = strlen(p_end + size + 1);
	rest = (char*)malloc(rest_size + 1);
	memcpy(rest, p_end + size + 1, rest_size);
	rest[rest_size] = '\0';
	c3 = (unsigned char*)strtok(rest, delimiter);
	c4 = strtok(NULL, delimiter);

	if (c3[0] == PRE_CIPHERTEXT_IN_G_GROUP) {
		element_init_GT(cipher->C1, pairing);
		element_init_G1(cipher->C2, pairing);
	} else {
		element_init_GT(cipher->C1, pairing);
		element_init_GT(cipher->C2, pairing);
	}

	element_from_bytes(cipher->C1, c1);
	element_from_bytes(cipher->C2, c2);
	cipher->group = c3[0];
	ul = strtoul(c4, NULL, 0);
	cipher->plaintext_bits = ul;

	free(rest);
	free(c1);
	free(c2);

	return 0;
}

/*
 * init function
 * pass pairing as second arg
 */
my_bool PRE_SUM_init(UDF_INIT *initid, UDF_ARGS *args,
		char *error)
{
	pbc_param_t	params;
	sum_state* state;
	char* pairing_str;
	size_t pairing_str_len;

	if (args->arg_count != 2 ||
			args->arg_type[0] != STRING_RESULT ||
			args->arg_type[1] != STRING_RESULT)
	{
		strcpy(error, "Usage: PRE_SUM(string ecCipherString, string pairing)");
		return 1;
	}

	state = malloc(sizeof(sum_state));
	pairing_str = (char*) args->args[1];
	pairing_str_len = (size_t) args->lengths[1];

	//pbc_param_init_a_gen(params, 160, 1024);
	//pairing_init_pbc_param(state->pairing, params);
	//pbc_param_clear(params);
	pairing_init_set_buf(state->pairing, pairing_str, pairing_str_len);

	state->isFirst = 1;

	initid->ptr = (char*) state;
	return 0;
}

/*
 * deinit function
 */
my_bool PRE_SUM_deinit(UDF_INIT *initid)
{
	sum_state* state = (sum_state*) initid->ptr;
	pre_ciphertext_clear(state->curSumCipher);
	pairing_clear(state->pairing);
	free(state);
	return 0;
}

/*
 * Reset the current aggregate value but do not insert the argument as the initial aggregate value for a new group.
 */
void PRE_SUM_clear(UDF_INIT *initid, char *is_null, char *error)
{
	sum_state* state = (sum_state*) initid->ptr;
	if(!state->isFirst) {
		element_init_same_as(state->curSumCipher->C1, state->curSumCipher->C1);
		element_init_same_as(state->curSumCipher->C2, state->curSumCipher->C2);
		state->curSumCipher->plaintext_bits = 0;
		state->curSumCipher->group = '\0';
	}
}

/*
 * Add the argument to the current aggregate value.
 */
my_bool PRE_SUM_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
		char *error)
{
	// For each row, the current value is added to the sum
	pre_ciphertext_t cipher;

	sum_state* state = (sum_state*) initid->ptr;

	char *encoded = (char*) args->args[0];
	if (state->isFirst) {
		pre_decode(state->curSumCipher, encoded, state->pairing);
		state->isFirst = 0;
	} else {
		pre_decode(cipher, encoded, state->pairing);

		/*
		 * C_a = C_a1, C_a2
		 * C_b = C_b1, C_b2
		 * C_a+b = C_a1C_b1, C_a2C_b2
		 * C_a+b = Z^(m_1 + m_2 + r1 + r2), g^(ar1 + ar2)
		 * C_a+b = Z^(m_1 + m_2 + r1 + r2), Z^(ar1 + ar2)
		 */
		element_mul(state->curSumCipher->C1, state->curSumCipher->C1, cipher->C1);
		element_mul(state->curSumCipher->C2, state->curSumCipher->C2, cipher->C2);

		state->curSumCipher->plaintext_bits=min(64, max(state->curSumCipher->plaintext_bits, cipher->plaintext_bits)+1);

		pre_ciphertext_clear(cipher);
	}

	return 0;
}

/*
 * get the result for the aggregate when the group changes or after the last row has been processed.
 */
char *PRE_SUM(UDF_INIT *initid, UDF_ARGS *args, char *result,
		unsigned long *length, char * s_null, char *error)
{
	sum_state* state = (sum_state*) initid->ptr;
	*length = pre_encode(state->curSumCipher, &result);
	return result;
}


// Cached SUM func
static pairing_t *pairing_cache = NULL;
/*
 * init function
 */
my_bool PRE_SUM_CACHED_init(UDF_INIT *initid, UDF_ARGS *args,
		char *error)
{
	pbc_param_t	params;
	sum_state* state;

	if (args->arg_count != 1 ||
			args->arg_type[0] != STRING_RESULT)
	{
		strcpy(error, "Usage: PRE_SUM_CACHED(string ecCipherString)");
		return 1;
	}

	state = malloc(sizeof(sum_state));

	if (pairing_cache == NULL)
	{
		pairing_cache = malloc(sizeof(pairing_t));
		pbc_param_init_a_gen(params, 160, 1024);
		pairing_init_pbc_param(*pairing_cache, params);
		pbc_param_clear(params);
	}

	//state->pairing = pairing_cache;
	state->isFirst = 1;

	initid->ptr = (char*) state;
	return 0;
}

/*
 * deinit function
 */
my_bool PRE_SUM_CACHED_deinit(UDF_INIT *initid)
{
	sum_state* state = (sum_state*) initid->ptr;
	pre_ciphertext_clear(state->curSumCipher);
	free(state);
	return 0;
}

/*
 * Reset the current aggregate value but do not insert the argument as the initial aggregate value for a new group.
 */
void PRE_SUM_CACHED_clear(UDF_INIT *initid, char *is_null, char *error)
{
	sum_state* state = (sum_state*) initid->ptr;
	if(!state->isFirst) {
		element_init_same_as(state->curSumCipher->C1, state->curSumCipher->C1);
		element_init_same_as(state->curSumCipher->C2, state->curSumCipher->C2);
		state->curSumCipher->plaintext_bits = 0;
		state->curSumCipher->group = '\0';
	}
}

/*
 * Add the argument to the current aggregate value.
 */
my_bool PRE_SUM_CACHED_add(UDF_INIT *initid, UDF_ARGS *args, char *is_null,
		char *error)
{
	// For each row, the current value is added to the sum
	pre_ciphertext_t cipher;

	sum_state* state = (sum_state*) initid->ptr;

	char *encoded = (char*) args->args[0];
	if (state->isFirst) {
		pre_decode(state->curSumCipher, encoded, *pairing_cache);
		state->isFirst = 0;
	} else {
		pre_decode(cipher, encoded, *pairing_cache);

		/*
		 * C_a = C_a1, C_a2
		 * C_b = C_b1, C_b2
		 * C_a+b = C_a1C_b1, C_a2C_b2
		 * C_a+b = Z^(m_1 + m_2 + r1 + r2), g^(ar1 + ar2)
		 * C_a+b = Z^(m_1 + m_2 + r1 + r2), Z^(ar1 + ar2)
		 */
		element_mul(state->curSumCipher->C1, state->curSumCipher->C1, cipher->C1);
		element_mul(state->curSumCipher->C2, state->curSumCipher->C2, cipher->C2);

		state->curSumCipher->plaintext_bits=min(64, max(state->curSumCipher->plaintext_bits, cipher->plaintext_bits)+1);

		pre_ciphertext_clear(cipher);
	}

	return 0;
}

/*
 * get the result for the aggregate when the group changes or after the last row has been processed.
 */
char *PRE_SUM_CACHED(UDF_INIT *initid, UDF_ARGS *args, char *result,
		unsigned long *length, char * s_null, char *error)
{
	sum_state* state = (sum_state*) initid->ptr;
	*length = pre_encode(state->curSumCipher, &result);
	return result;
}


#endif /* HAVE_DLOPEN */
