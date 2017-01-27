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

#ifndef ECELGAMAL_RELIC_ECELGAMAL_RELIC_H
#define ECELGAMAL_RELIC_ECELGAMAL_RELIC_H

#include <relic/relic_core.h>
#include <relic/relic_ec.h>
#include <relic/relic_bn.h>
#include <inttypes.h>
#include "khash.h"

/**
 * Implementation of additative homomorphic EC-ElGamal with the relic toolkit
 * See benchmakr_ecelgamal_relic.cpp for more details on how to use it
 */

/**
 * HashMap for BSGS
 */
#define kh_set(kname, hash, key, val) ({int ret; k = kh_put(kname, hash,key,&ret); kh_value(hash,k) = val; ret;})

/**
 * Represents a EC-ElGamal Key
 */
struct gamal_key {
    char is_public;
    ec_t Y;
    bn_t secret;
};

typedef struct gamal_key *gamal_key_ptr;
typedef struct gamal_key gamal_key_t[1];

/**
 * Computes the size of the encoded representation of the key.
 * @param key the key
 * @return the size of the encoded key
 */
int get_encoded_key_size(gamal_key_t key);

/**
 * Encodes the key as a byte array
 * @param buff the allocated buffer for the encoding
 * @param size the  allocated buffer size
 * @param key the key
 * @return STS_OK if ok else STS_ERR
 */
int encode_key(char* buff, int size, gamal_key_t key);

/**
 * Decodes a key.
 * @param key the key
 * @param buff the buffer containing the encoded key
 * @param size the encoded key size
 * @return STS_OK if ok else STS_ERR
 */
int decode_key(gamal_key_t key, char* buff, int size);

/**
 * Represents an EC-ElGamal ciphertext
 */
struct gamal_ciphertext {
    ec_t C1;
    ec_t C2;
};

typedef struct gamal_ciphertext *gamal_ciphertext_ptr;
typedef struct gamal_ciphertext gamal_ciphertext_t[1];

/**
 * Computes the size of the encoded representation of the ciphertext.
 * @param ciphertext
 * @return the size of the encoded key ciphertext
 */
int get_encoded_ciphertext_size(gamal_ciphertext_t ciphertext);

/**
 * Encodes the ciphertext as a byte array
 * @param buff the allocated buffer for the encoding
 * @param size the  allocated buffer size
 * @param ciphertext
 * @return STS_OK if ok else STS_ERR
 */
int encode_ciphertext(char* buff, int size, gamal_ciphertext_t ciphertext);

/**
 * Decodes a ciphertext
 * @param ciphertext
 * @param buff the buffer containing the encoded ciphertext
 * @param size the encoded ciphertext size
 * @return STS_OK if ok else STS_ERR
 */
int decode_ciphertext(gamal_ciphertext_t ciphertext, char* buff, int size);

/**
 * Inits the library !Has to be called first ALWAYS (RELIC)
 * @return
 */
int gamal_init();

/**
 * Deinits the library (RELIC)
 * @return STS_OK if ok else STS_ERR
 */
int gamal_deinit();

/**
 * Precompute the bsgs table
 * @param size the number of table entries
 * @return STS_OK if ok else STS_ERR
 */
int gamal_init_bsgs_table(dig_t size);

/**
 * Free a key
 * @param keys
 * @return STS_OK if ok else STS_ERR
 */
int gamal_key_clear(gamal_key_t keys);

/**
 * Free a ciphertext
 * @param cipher
 * @return STS_OK if ok else STS_ERR
 */
int gamal_cipher_clear(gamal_ciphertext_t cipher);


/**
 * Create a  empty ciphertext
 * @param cipher
 * @return STS_OK if ok else STS_ERR
 */
int gamal_cipher_new(gamal_ciphertext_t cipher);

/**
 * Generate an EC-Elgamal key
 * @param keys the container for the key
 * @return STS_OK if ok else STS_ERR
 */
int gamal_generate_keys(gamal_key_t keys);

/**
 * Encrypt a plaintext integer with EC-ElGamal
 * @param ciphertext the resulting ciphertext
 * @param key the EC-ElGamal key
 * @param plaintext the integer (Note max 32bit without CRT)
 * @return STS_OK if ok else STS_ERR
 */
int gamal_encrypt(gamal_ciphertext_t ciphertext, gamal_key_t key, dig_t plaintext);

/**
 * Decrypts a EC-Elgamal ciphertext and maps it back to the plaintext integer.
 * @param res a pointer to the resulting plaintext
 * @param key the key
 * @param ciphertext the ciphertext
 * @param use_bsgs if 0 use brute-force else use baby-step-giant-step for the mapping
 * @return STS_OK if ok else STS_ERR
 */
int gamal_decrypt(dig_t *res, gamal_key_t key, gamal_ciphertext_t ciphertext, const char use_bsgs);

/**
 * Adds two ciphertext (Additative homomorphic property)
 * a+b = dec(enc(a)++enc(b))
 * @param res the resulting ciphertext
 * @param ciphertext1 summand 1
 * @param ciphertext2 summand 2
 * @return TS_OK if ok else STS_ERR
 */
int gamal_add(gamal_ciphertext_t res, gamal_ciphertext_t ciphertext1, gamal_ciphertext_t ciphertext2);

#endif //ECELGAMAL_RELIC_ECELGAMAL_RELIC_