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

#include <jni.h>
#include "ecelgamal-relic.h"
#include <android/log.h>

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%s",__VA_ARGS__)
#define  LOGINUM(...)  __android_log_print(ANDROID_LOG_INFO,"BLIBLABLU","%lu",__VA_ARGS__)
#define  LOGIFolat(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%f",__VA_ARGS__)

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

void get_key(JNIEnv *env, gamal_key_t *key, jbyteArray array) {
    int buff_len;
    char *buffer = as_unsigned_char_array(env, array, &buff_len);
    decode_key(*key, buffer, buff_len);
    free(buffer);
}

void get_cipher(JNIEnv *env, gamal_ciphertext_t *cipher, jbyteArray array) {
    int buff_len;
    char *buffer = as_unsigned_char_array(env, array, &buff_len);
    decode_ciphertext(*cipher, buffer, buff_len);
    free(buffer);
}

jint Java_ch_dsg_talos_relicproxyreenc_crypto_ECElGamalRelic_initGamal(JNIEnv *env,
                                                               jobject javaThis) {
    return (jint) gamal_init();;
}

jint Java_ch_dsg_talos_relicproxyreenc_crypto_ECElGamalRelic_deinitGamal(JNIEnv *env,
                                                                 jobject javaThis) {
    return (jint) gamal_deinit();
}

jbyteArray Java_ch_dsg_talos_relicproxyreenc_crypto_ECElGamalRelic_generateKey(JNIEnv *env,
                                                                         jobject javaThis) {
    gamal_key_t key;
    char *buffer;
    int key_size;
    jbyteArray res;

    gamal_generate_keys(key);
    key_size = get_encoded_key_size(key);
    buffer = (char *) malloc((size_t) key_size);
    encode_key(buffer, key_size, key);
    res = as_byte_array(env, buffer, key_size);
    
    free(buffer);
    gamal_key_clear(key);
    return res;
}

jbyteArray Java_ch_dsg_talos_relicproxyreenc_crypto_ECElGamalRelic_encrypt(JNIEnv *env,
                                                                     jobject javaThis, jlong value,
                                                                     jbyteArray key_oct) {
    gamal_key_t key;
    gamal_ciphertext_t ciphertext;
    char *buffer;
    int cipher_size;
    jbyteArray res;

    get_key(env, &key, key_oct);
    gamal_encrypt(ciphertext, key, (dig_t) value);

    cipher_size = get_encoded_ciphertext_size(ciphertext);
    buffer = (char *) malloc((size_t) cipher_size);
    encode_ciphertext(buffer, cipher_size, ciphertext);
    res = as_byte_array(env, buffer, cipher_size);

    free(buffer);
    gamal_key_clear(key);
    gamal_cipher_clear(ciphertext);
    return res;
}

jlong Java_ch_dsg_talos_relicproxyreenc_crypto_ECElGamalRelic_decrypt(JNIEnv *env, jobject javaThis, jbyteArray ciphertext_oct,
                                                                jbyteArray key_oct, jboolean use_bsgs) {
    gamal_key_t key;
    gamal_ciphertext_t ciphertext;
    uint64_t value = 0;

    get_key(env, &key, key_oct);
    get_cipher(env, &ciphertext, ciphertext_oct);

    if(!gamal_decrypt(&value, key, ciphertext, use_bsgs) == STS_OK) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Error on decryption");
    }

    gamal_key_clear(key);
    gamal_cipher_clear(ciphertext);
    return (jlong) value;
}

jint Java_ch_dsg_talos_relicproxyreenc_crypto_ECElGamalRelic_initBsgsTable(JNIEnv *env, jobject javaThis,
                                                                             jint table_size) {
    gamal_init_bsgs_table((uint64_t) table_size);
    return 0;
}

jbyteArray Java_ch_dsg_talos_relicproxyreenc_crypto_ECElGamalRelic_homAdd(JNIEnv *env, jobject javaThis, jbyteArray ciphertext_1_oct,
                                                                     jbyteArray ciphertext_2_oct) {
    gamal_ciphertext_t ciphertext1, ciphertext2, res_ciphertext;
    char *buffer;
    int cipher_size;
    jbyteArray res;

    get_cipher(env, &ciphertext1, ciphertext_1_oct);
    get_cipher(env, &ciphertext2, ciphertext_2_oct);

    gamal_add(res_ciphertext, ciphertext1, ciphertext2);

    cipher_size = get_encoded_ciphertext_size(res_ciphertext);
    buffer = (char *) malloc((size_t) cipher_size);
    encode_ciphertext(buffer, cipher_size, res_ciphertext);
    res = as_byte_array(env, buffer, cipher_size);

    free(buffer);
    gamal_cipher_clear(ciphertext1);
    gamal_cipher_clear(ciphertext2);
    gamal_cipher_clear(res_ciphertext);
    return res;
}