/*
 * Copyright (c) 2016, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 *
 * Author:
 *       Lukas Burkhalter <lubu@student.ethz.ch>
 *       Hossein Shafagh <shafagh@inf.ethz.ch>
 *       Pascal Fischli <fischlip@student.ethz.ch>
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

#ifndef PRE_REL_ENC_H_
#define PRE_REL_ENC_H_

#include <assert.h>
#include "khash.h"
#include <relic/relic_types.h>
#include <relic/relic_bench.h>
#include <relic/relic_core.h>

#include <relic/relic_bn.h>
#include <relic/relic_ec.h>
#include <relic/relic_pc.h>


#define PRE_REL_KEYS_TYPE_SECRET 's'
#define PRE_REL_KEYS_TYPE_ONLY_PUBLIC 'p'

#define PRE_REL_CIPHERTEXT_IN_G_GROUP '1'
#define PRE_REL_CIPHERTEXT_IN_GT_GROUP '2'

#define ENCODING_SIZE 2;

/**
 * HashMap for bsgs
 * */
#define kh_set(kname, hash, key, val) ({int ret; k = kh_put(kname, hash,key,&ret); kh_value(hash,k) = val; ret;})

/**
 *  Represents a PRE Key
 */
struct pre_keys_s {
    bn_t sk;							// secret factor a
    gt_t Z;								// Z = e(g,g)
    g1_t g, pk;							// generator, public key g^a
    g2_t g2, pk_2;		 	// generator, public key g_2^a, re-encryption token
    char type;							// flag to indicate the presence of the secret key
};
typedef struct pre_keys_s *pre_rel_keys_ptr;
typedef struct pre_keys_s pre_keys_t[1];

/**
 * Returns the encoded key size of the provided key
 * @param key
 * @return the size in bytes of the encoded key
 */
int get_encoded_key_size(pre_keys_t key);

/**
 * Encodes the given key as a byte array.
 * @param buff the allocated buffer for the encoding
 * @param size the allocated buffer size
 * @param key the key
 * @return STS_OK if ok else STS_ERR
 */
int encode_key(char* buff, int size, pre_keys_t key);

/**
 * Decodes the encoded key from a buffer.
 * @param key the keys
 * @param buff the buffer containing the encoded key
 * @param size the buffer size of the encoded key
 * @return STS_OK if ok else STS_ERR
 */
int decode_key(pre_keys_t key, char* buff, int size);

/**
 * Represents a PRE re-encryption token
 */
struct pre_re_token_s {
    g2_t re_token;
};
typedef struct pre_re_token_s *pre_re_token_ptr;
typedef struct pre_re_token_s pre_re_token_t[1];

/**
 * Returns the encoded token size of the provided token
 * @param token the PRE token
 * @return the size in bytes of the encoded token
 */
int get_encoded_token_size(pre_re_token_t token);


/**
 * Encodes the given token as a byte array.
 * @param buff the allocated buffer for the encoding
 * @param size the allocated buffer size
 * @param token the token
 * @return STS_OK if ok else STS_ERR
 */
int encode_token(char* buff, int size, pre_re_token_t token);

/**
 * Decodes the encoded token from a byte buffer.
 * @param token the token
 * @param buff the buffer containing the encoded token
 * @param size the buffer size of the encoded token
 * @return STS_OK if ok else STS_ERR
 */
int decode_token(pre_re_token_t token, char* buff, int size);

/**
 * The representation of a PRE ciphertext.
 */
struct pre_ciphertext_s {
    gt_t C1;							// cyphertext part 1
    g1_t C2_G1;							// cyphertext part 2 in G1
    gt_t C2_GT;							// cyphertext part 2 in GT
    char group;							// flag to indicate the working group
};
typedef struct pre_ciphertext_s *pre_rel_ciphertext_ptr;
typedef struct pre_ciphertext_s pre_ciphertext_t[1];

/**
 * Returns the encoded token size of the provided ciphertext
 * @param cipher the PRE ciphertext
 * @return the size in bytes of the encoded ciphertext
 */
int get_encoded_cipher_size(pre_ciphertext_t cipher);

/**
 * Encodes the given ciphertext as a byte array.
 * @param buff the allocated buffer for the encoding
 * @param size the allocated buffer size
 * @param cipher the ciphertext
 * @return STS_OK if ok else STS_ERR
 */
int encode_cipher(char* buff, int size, pre_ciphertext_t cipher);

/**
 * Decodes the encoded ciphertext from a byte buffer.
 * @param cipher the ciphertext
 * @param buff the buffer containing the encoded ciphertext
 * @param size the buffer size of the encoded ciphertext
 * @return STS_OK if ok else STS_ERR
 */
int decode_cipher(pre_ciphertext_t cipher, char* buff, int size);

/**
 * Inits the PRE libray (HAS TO BE CALLED BEFORE USE!)
 * @return  STS_OK if ok else STS_ERR
 */
int pre_init();

/**
 * Deinits the PRE library
 * @return STS_OK if ok else STS_ERR
 */
int pre_deinit();

/**
 * Precomputes a bsgs table with the given size
 * @param size the number of mappings to compute
 * @return STS_OK if ok else STS_ERR
 */
int pre_init_bsgs_table(uint64_t size);

/**
 * Free a key
 * @param keys
 * @return STS_OK if ok else STS_ERR
 */
int pre_keys_clear(pre_keys_t keys);

/**
 * Free a ciphertext
 * @param cipher
 * @return STS_OK if ok else STS_ERR
 */
int pre_cipher_clear(pre_ciphertext_t cipher);

/**
 * Free a token
 * @param token
 * @return STS_OK if ok else STS_ERR
 */
int pre_token_clear(pre_re_token_t token);

/**
 * Allocates a empty ciphertext )
 * @param ciphertext
 * @param group the goupflag
 * @return STS_OK if ok else STS_ERR
 */
int pre_ciphertext_init(pre_ciphertext_t ciphertext, char group);

/**
 * Free a ciphertext
 * @param ciphertext
 * @return STS_OK if ok else STS_ERR
 */
int pre_ciphertext_clear(pre_ciphertext_t ciphertext);

/**
 * Generate l
 * @param keys
 * @return
 */
int pre_generate_keys(pre_keys_t keys);

/**
 * Generate a PRE key
 * @param keys the key
 * @return STS_OK if ok else STS_ERR
 */
int pre_generate_secret_key(pre_keys_t keys);

/**
 * Encrypt a plaintext integer with the PRE cipher.
 * @param ciphertext the resulting ciphertext.
 * @param keys the PRE key
 * @param plaintext the input integer
 * @return STS_OK if ok else STS_ERR
 */
int pre_encrypt(pre_ciphertext_t ciphertext, pre_keys_t keys, uint64_t plaintext);

/**
 * Decrypts a PRE ciphertext with the given key and maps it back to the plaintext integer.
 * @param res a pointer to the resulting integer.
 * @param keys a PRE key
 * @param ciphertext the input ciphertext
 * @param use_bsgs 0 for brutforce, else for baby-step-giant-step used for the mapping
 * @return STS_OK if ok else STS_ERR
 */
int pre_decrypt(dig_t *res, pre_keys_t keys, pre_ciphertext_t ciphertext, const char use_bsgs);

/**
 * Homomorphically adds two ciphertexts.
 * a+b = dec(enc(a)++enc(b))
 * @param keys a PRE key (only public part is needed)
 * @param res the resulting ciphertext
 * @param ciphertext1 summand1
 * @param ciphertext2 summand2
 * @param re_rand indicated if re-randomization should be used
 * @return STS_OK if ok else STS_ERR
 */
int pre_homo_add(pre_keys_t keys, pre_ciphertext_t res, pre_ciphertext_t ciphertext1, pre_ciphertext_t ciphertext2, char re_rand);

/**
 * Generates a re-encrpytion token from A to B.
 * @param token the token from (A->B)
 * @param keys the PRE key of A
 * @param pk_2_b the public part of the PRE key of B.
 * @return  STS_OK if ok else STS_ERR
 */
int pre_generate_re_token(pre_re_token_t token, pre_keys_t keys, g2_t pk_2_b);

/**
 * Re-encrypts the given ciphertext with the provided token
 * @param keys the PRE token
 * @param res the resulting ciphertext
 * @param ciphertext th input ciphertext
 * @return  STS_OK if ok else STS_ERR
 */
int pre_re_apply(pre_re_token_t keys, pre_ciphertext_t res, pre_ciphertext_t ciphertext);

//used for testing
int solve_dlog_brute(gt_t M, gt_t G, bn_t x, dig_t max_it);


#endif
