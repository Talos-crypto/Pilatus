//
// Created by lubu on 11.08.16.
//

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