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
#include <stdlib.h>

#include <openssl/ec.h>
#include <openssl/bn.h>
#include <openssl/objects.h>


static EC_GROUP *curveGroup = NULL;

jint Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMulOpenSSL_initOpenSSLCurve(JNIEnv *env, jobject javaThis,
                                                                      jint curve_id) {
    if (curveGroup != NULL) {
        EC_GROUP_free(curveGroup);
    }
    curveGroup = EC_GROUP_new_by_curve_name((int) curve_id);
    return 0;
}


EC_POINT *multiplyGenerator(const BIGNUM *n) {
    EC_POINT *res;
    BIGNUM *bn1;
    BN_CTX *ctx;
    ctx = BN_CTX_new();
    res = EC_POINT_new(curveGroup);
    EC_POINT_mul(curveGroup, res, n, NULL, NULL, ctx);
    BN_CTX_free(ctx);
    return res;
}

BIGNUM *getCurveOrder() {
    BIGNUM *order;
    BN_CTX *ctx;
    ctx = BN_CTX_new();
    order = BN_new();
    EC_GROUP_get_order(curveGroup, order, ctx);
    BN_CTX_free(ctx);
    return order;
}



jlong Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMulOpenSSL_OpenSSLMul(JNIEnv *env, jobject javaThis) {
    BIGNUM *num, *order;
    EC_POINT *res;
    num = BN_new();
    order = getCurveOrder();
    BN_rand_range(num, order);

    res = multiplyGenerator(num);

    BN_free(num);
    BN_free(order);
    EC_POINT_free(res);
    return 0;
}

jlong Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMulOpenSSL_OpenSSLOverhead(JNIEnv *env, jobject javaThis) {
    BIGNUM *num, *order;
    num = BN_new();
    order = getCurveOrder();
    BN_rand_range(num, order);
    BN_free(num);
    BN_free(order);
    return 0;
}