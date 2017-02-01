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

#include <string.h>
#include <jni.h>
#include <openssl/bn.h>
#include <openssl/objects.h>
#include <android/log.h>
#include <sys/time.h>

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%s",__VA_ARGS__)
#define  LOGINUM(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%d",__VA_ARGS__)
#define  LOGIFolat(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%f",__VA_ARGS__)



BIGNUM* convert_to_bignum(JNIEnv *env, jstring str) {
    BIGNUM *res;
    const char *nativeStr;
    res = NULL;
    nativeStr = (*env)->GetStringUTFChars(env, str, 0);
    BN_dec2bn(&res, nativeStr);
    (*env)->ReleaseStringUTFChars(env, str, nativeStr);
    return res;
}

jstring* BN_to_jstring(JNIEnv *env, BIGNUM *num) {
    char *numString;
    jstring res;
    numString = BN_bn2dec(num);
    res = (*env)->NewStringUTF(env, numString);
    free(numString);
    return res;
}

int Lfast(BIGNUM *res, const BIGNUM *u, const BIGNUM *ninv, const BIGNUM *two_n, const BIGNUM *n) {
	BN_CTX *ctx = BN_CTX_new();
	BN_copy(res, u);
	BN_sub_word(res, 1);
	BN_mod_mul(res, res, ninv, two_n, ctx);
	BN_mod(res, res, n, ctx);
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_PaillierNative_encrypt(JNIEnv *env,
                                                                                jobject javaThis,
                                                                                jstring j_plaintext,
                                                                                jstring j_g,
                                                                                jstring j_rand,
                                                                                jstring j_n2,
                                                                                jstring j_n) {
	BIGNUM *plaintext, *g, *r, *n2, *n;
	BIGNUM *temp = BN_new();
	jstring* res;
	BN_CTX *ctx = BN_CTX_new();

	plaintext = convert_to_bignum(env, j_plaintext);
	g = convert_to_bignum(env, j_g);
	r = convert_to_bignum(env, j_rand);
	n2 = convert_to_bignum(env, j_n2);
	n = convert_to_bignum(env, j_n);

	//temp = n*r
	BN_mul(temp, n, r, ctx);
	//temp = plaintext + n*r
	BN_add(temp, plaintext, temp);
	//temp = g^(plaintext + n*r) % n2
	BN_mod_exp(temp, g, temp,
               n2, ctx);

	res = BN_to_jstring(env, temp);

	BN_CTX_free(ctx);
	BN_free(plaintext);
    BN_free(g);
    BN_free(r);
    BN_free(n2);
    BN_free(n);
    BN_free(temp);

	return res;
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_PaillierPrivNative_decryptpart(JNIEnv *env,
                                                                                jobject javaThis,
                                                                                jstring j_ciphertext,
                                                                                jstring j_p2,
                                                                                jstring j_a,
                                                                                jstring j_pinv,
                                                                                jstring j_two_p,
                                                                                jstring j_p,
                                                                                jstring j_hp) {
	BIGNUM *ciphertext, *p2, *a, *pinv, *two_p, *p, *hp;
	jstring* res;
	BIGNUM *temp = BN_new();
	BIGNUM *temp_2 = BN_new();
	BN_CTX *ctx = BN_CTX_new();

	ciphertext = convert_to_bignum(env, j_ciphertext);
	p2 = convert_to_bignum(env, j_p2);
	a = convert_to_bignum(env, j_a);
	pinv = convert_to_bignum(env, j_pinv);
	two_p = convert_to_bignum(env, j_two_p);
	p = convert_to_bignum(env, j_p);
	hp = convert_to_bignum(env, j_hp);

	// temp = ciphertext % p2
	BN_mod(temp, ciphertext, p2, ctx);
	// temp = g^(plaintext + n*r) % n2
	BN_mod_exp(temp, temp, a, p2, ctx);

	Lfast(temp_2, temp, pinv, two_p, p);

	//temp = g^(plaintext + n*r) % n2
	BN_mod_mul(temp, temp_2, hp, p, ctx);

	res = BN_to_jstring(env, temp);

	BN_CTX_free(ctx);
	BN_free(ciphertext);
    BN_free(p2);
    BN_free(a);
    BN_free(pinv);
    BN_free(two_p);
    BN_free(p);
    BN_free(hp);
   	BN_free(temp);
   	BN_free(temp_2);

	return res;
}