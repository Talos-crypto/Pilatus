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
#include <relic/relic_core.h>
#include <relic/relic_ec.h>
#include <relic/relic_ep.h>
#include <stdlib.h>
#include <android/log.h>

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%s",__VA_ARGS__)
#define  LOGINUM(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%d",__VA_ARGS__)
#define  LOGIFolat(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%f",__VA_ARGS__)

jint Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMulRelic_initBench(JNIEnv *env, jobject javaThis) {
    if (core_init() != STS_OK) {
        core_clean();
        return (jint) STS_ERR;
    }

    if (ec_param_set_any() != STS_OK) {
        THROW(ERR_NO_CURVE);
        core_clean();
        return STS_ERR;
    } else {
    }
    ec_param_print();
    LOGI("INIT OK");
    return 0;
}

/*
jint Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMul_initRelicCurve(JNIEnv *env, jobject javaThis,
                                                                             jint curve_id) {
    switch(curve_id) {
        case (1):
            ep_param_set(SECG_160);
            break;
        case (2):
            ep_param_set(SECG_224);
            break;
        case (3):
            ep_param_set(NIST_224);
            break;
        case (4):
            ep_param_set(CURVE_25519);
            break;
        case (5):
            ep_param_set(BN_P254);
            break;
        default:
            ep_param_set(SECG_160);
            break;
    }
    return 0;
}
*/
/*
jlong Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMul_benchRelicMul(JNIEnv *env, jobject javaThis) {
    ep_t res;
    bn_t num, prime;
    jlong time;
    ctx_t *ctx = core_get();
    bench_reset();
    ep_curve_get_ord(prime);
    bn_rand_mod(num, prime);
    ep_new(res);
    bench_before();
    ep_mul_gen(res, num);
    bench_after();
    time = (jlong) ctx->total;
    ep_free(res);
    bn_free(num);
    bn_free(prime);
    return time;
}
*/

jlong Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMulRelic_benchRelicMul(JNIEnv *env, jobject javaThis) {
    ec_t res;
    bn_t num, prime;
    bn_null(prime);
    bn_null(num);
    bn_new(prime);
    bn_new(num);
    ec_curve_get_ord(prime);
    bn_rand_mod(num, prime);
    bn_set_dig(num, 100);
    ec_new(res);

    ec_mul_gen(res, num);

    ec_free(res);
    bn_free(num);
    bn_free(prime);
    return 0;
}

jlong Java_ch_dsg_talos_relicproxyreenc_crypto_BenchMulRelic_benchRelicOverhead(JNIEnv *env, jobject javaThis) {
    bn_t num, prime;
    bn_new(prime);
    bn_new(num);
    ec_curve_get_ord(prime);
    bn_rand_mod(num, prime);
    bn_free(num);
    bn_free(prime);
    return 0;
}