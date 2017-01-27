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

#include <jni.h>
#include "pre-hom.h"

//Implements a Java JNI wrapper for the PRE cipher.

jbyteArray as_byte_array(JNIEnv *env, char *buf, int len) {
    jbyteArray array = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, array, 0, len, (jbyte *) buf);
    return array;
}

char *as_unsigned_char_array(JNIEnv *env, jbyteArray array, int *len) {
    *len = (*env)->GetArrayLength(env, array);
    char *buf = (char *) malloc((size_t) *len);
    (*env)->GetByteArrayRegion(env, array, 0, *len, (jbyte *) buf);
    return buf;
}

void get_key(JNIEnv *env, pre_keys_t *key, jbyteArray array) {
    int buff_len;
    char *buffer = as_unsigned_char_array(env, array, &buff_len);
    decode_key(*key, buffer, buff_len);
    free(buffer);
}

void get_cipher(JNIEnv *env, pre_ciphertext_t *cipher, jbyteArray array) {
    int buff_len;
    char *buffer = as_unsigned_char_array(env, array, &buff_len);
    decode_cipher(*cipher, buffer, buff_len);
    free(buffer);
}

void get_token(JNIEnv *env, pre_re_token_t *token, jbyteArray array) {
    int buff_len;
    char *buffer = as_unsigned_char_array(env, array, &buff_len);
    decode_token(*token, buffer, buff_len);
    free(buffer);
}

jint Java_crypto_PRERelic_initPre(JNIEnv *env,
                                                               jobject javaThis) {
    return (jint) pre_init();
}

jint Java_crypto_PRERelic_deinitPre(JNIEnv *env,
                                                                 jobject javaThis) {
    return (jint) pre_deinit();
}

jbyteArray Java_crypto_PRERelic_generateKey(JNIEnv *env,
                                                                         jobject javaThis) {
    pre_keys_t key;
    char *buffer;
    int key_size;
    jbyteArray res;

    pre_generate_keys(key);
    key_size = get_encoded_key_size(key);
    buffer = (char *) malloc((size_t) key_size);
    encode_key(buffer, key_size, key);
    res = as_byte_array(env, buffer, key_size);

    free(buffer);
    pre_keys_clear(key);
    return res;
}

jbyteArray Java_crypto_PRERelic_encrypt(JNIEnv *env,
                                                                     jobject javaThis, jlong value,
                                                                     jbyteArray key_oct) {
    pre_keys_t key;
    pre_ciphertext_t ciphertext;
    char *buffer;
    int cipher_size;
    jbyteArray res;

    get_key(env, &key, key_oct);
    pre_encrypt(ciphertext, key, (uint64_t) value);

    cipher_size = get_encoded_cipher_size(ciphertext);
    buffer = (char *) malloc((size_t) cipher_size);
    encode_cipher(buffer, cipher_size, ciphertext);
    res = as_byte_array(env, buffer, cipher_size);

    free(buffer);
    pre_keys_clear(key);
    pre_cipher_clear(ciphertext);
    return res;
}

jlong Java_crypto_PRERelic_decrypt(JNIEnv *env, jobject javaThis, jbyteArray ciphertext_oct,
                                                                jbyteArray key_oct, jboolean use_bsgs) {
    pre_keys_t key;
    pre_ciphertext_t ciphertext;
    dig_t value = 0;

    get_key(env, &key, key_oct);
    get_cipher(env, &ciphertext, ciphertext_oct);


    if(!pre_decrypt(&value, key, ciphertext, use_bsgs) == STS_OK) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Error on decryption");
    }

    pre_keys_clear(key);
    pre_cipher_clear(ciphertext);
    return (jlong) value;
}

jbyteArray Java_crypto_PRERelic_createReEncToken(JNIEnv *env, jobject javaThis, jbyteArray key_from_oct,
                                                                              jbyteArray key_to_oct) {
    pre_keys_t key_from, key_to;
    pre_re_token_t token;
    jbyteArray res;
    char *buffer;
    int token_size;

    get_key(env, &key_from, key_from_oct);
    get_key(env, &key_to, key_to_oct);

    pre_generate_re_token(token, key_from, key_to->pk_2);

    token_size = get_encoded_token_size(token);
    buffer = (char *) malloc((size_t) token_size);
    encode_token(buffer, token_size, token);
    res = as_byte_array(env, buffer, token_size);

    pre_token_clear(token);
    pre_keys_clear(key_from);
    pre_keys_clear(key_to);
    free(buffer);
    return res;
}

jbyteArray Java_crypto_PRERelic_reApply(JNIEnv *env, jobject javaThis, jbyteArray ciphertext_oct,
                                                                     jbyteArray token_oct) {
    pre_re_token_t token;
    pre_ciphertext_t ciphertext, res_ciphertext;
    jbyteArray res;
    char *buffer;
    int cipher_size;

    get_cipher(env, &ciphertext, ciphertext_oct);
    get_token(env, &token, token_oct);

    pre_re_apply(token, res_ciphertext, ciphertext);

    cipher_size = get_encoded_cipher_size(res_ciphertext);
    buffer = (char *) malloc((size_t) cipher_size);
    encode_cipher(buffer, cipher_size, res_ciphertext);
    res = as_byte_array(env, buffer, cipher_size);

    pre_token_clear(token);
    pre_ciphertext_clear(ciphertext);
    pre_ciphertext_clear(res_ciphertext);
    free(buffer);
    return res;
}

jbyteArray Java_crypto_PRERelic_homAdd(JNIEnv *env, jobject javaThis, jbyteArray ciphertext_1_oct,
                                                                    jbyteArray ciphertext_2_oct, jbyteArray key_oct, jboolean re_rand) {
    pre_keys_t key;
    pre_ciphertext_t ciphertext1, ciphertext2, res_ciphertext;
    char *buffer;
    int cipher_size;
    jbyteArray res;

    get_key(env, &key, key_oct);
    get_cipher(env, &ciphertext1, ciphertext_1_oct);
    get_cipher(env, &ciphertext2, ciphertext_2_oct);

    pre_ciphertext_init(res_ciphertext, ciphertext1->group);
    pre_homo_add(key, res_ciphertext, ciphertext1, ciphertext2, re_rand);

    cipher_size = get_encoded_cipher_size(res_ciphertext);
    buffer = (char *) malloc((size_t) cipher_size);
    encode_cipher(buffer, cipher_size, res_ciphertext);
    res = as_byte_array(env, buffer, cipher_size);

    free(buffer);
    pre_keys_clear(key);
    pre_cipher_clear(ciphertext1);
    pre_cipher_clear(ciphertext2);
    pre_cipher_clear(res_ciphertext);
    return res;
}

jint Java_crypto_PRERelic_initBsgsTable(JNIEnv *env, jobject javaThis,
                                                                     jint table_size) {
    pre_init_bsgs_table((uint64_t) table_size);
    return 0;
}