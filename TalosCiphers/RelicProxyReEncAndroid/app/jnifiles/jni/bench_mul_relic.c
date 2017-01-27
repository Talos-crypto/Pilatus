//
// Created by lubu on 11.08.16.
//

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