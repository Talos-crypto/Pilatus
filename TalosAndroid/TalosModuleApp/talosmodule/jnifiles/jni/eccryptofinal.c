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
#include <openssl/ec.h>
#include <openssl/bn.h>
#include <openssl/objects.h>
#include <android/log.h>
#include <sys/time.h>

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%s",__VA_ARGS__)
#define  LOGINUM(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%d",__VA_ARGS__)
#define  LOGIFolat(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%f",__VA_ARGS__)


static int curve = NID_X9_62_prime192v1;

static EC_GROUP *curveGroup = NULL;

void init(const int in) {
    if (curveGroup != NULL) {
        EC_GROUP_free(curveGroup);
    }
    curveGroup = EC_GROUP_new_by_curve_name(in);
}

void setTestCurve() {
    BIGNUM *p, *a, *b, *x, *y, *order;
    EC_POINT *gen;
    BN_CTX *ctx;
    const char pS[] = "263";
    const char aS[] = "2";
    const char bS[] = "3";
    const char xS[] = "200";
    const char yS[] = "39";
    const char ordS[] = "270";

    ctx = BN_CTX_new();
    a = NULL;
    b = NULL;
    p = NULL;
    x = NULL;
    y = NULL;
    BN_dec2bn(&p, pS);
    BN_dec2bn(&a, aS);
    BN_dec2bn(&b, bS);
    BN_dec2bn(&x, xS);
    BN_dec2bn(&y, yS);
    BN_dec2bn(&order, ordS);
    curveGroup = EC_GROUP_new_curve_GFp(p, a, b, ctx);
    gen = EC_POINT_new(curveGroup);
    EC_POINT_set_affine_coordinates_GFp(curveGroup, gen, x, y, ctx);
    EC_GROUP_set_generator(curveGroup, gen, order, NULL);

    EC_POINT_free(gen);
    BN_free(order);
    BN_free(y);
    BN_free(x);
    BN_free(p);
    BN_free(b);
    BN_free(a);
    BN_CTX_free(ctx);
}


EC_POINT *multiplyConst(const EC_POINT *in, const BIGNUM *n) {
    EC_POINT *res;
    BIGNUM *bn1;
    BN_CTX *ctx;
    bn1 = BN_new();
    ctx = BN_CTX_new();
    BN_zero(bn1);
    res = EC_POINT_new(curveGroup);
    EC_POINT_mul(curveGroup, res, bn1, in, n, ctx);
    BN_free(bn1);
    BN_CTX_free(ctx);
    return res;
}

EC_POINT *multiplyGenerator(const BIGNUM *n) {
    return multiplyConst(EC_GROUP_get0_generator(curveGroup), n);
}

EC_POINT *multiplyPow2(const EC_POINT *in, int pow) {
    EC_POINT *res;
    BN_CTX *ctx;
    int i;
    ctx = BN_CTX_new();
    res = EC_POINT_dup(in, curveGroup);
    for (i = 0; i < pow; i++) {
        EC_POINT_dbl(curveGroup, res, res, ctx);
    }

    BN_CTX_free(ctx);
    return res;
}

EC_POINT *multiplyPow2Gen(int pow) {
    return multiplyPow2(EC_GROUP_get0_generator(curveGroup), pow);
}

BIGNUM *convertToBignum(jstring str, JNIEnv *env) {
    BIGNUM *res;
    const char *nativeStr;
    res = NULL;
    nativeStr = (*env)->GetStringUTFChars(env, str, 0);
    BN_dec2bn(&res, nativeStr);
    (*env)->ReleaseStringUTFChars(env, str, nativeStr);
    return res;
}

jstring *BigNumToString(BIGNUM *num, JNIEnv *env) {
    char *numString;
    jstring res;
    numString = BN_bn2dec(num);
    res = (*env)->NewStringUTF(env, numString);
    free(numString);
    return res;
}

char *convertToString(const EC_POINT *point) {
    BN_CTX *ctx;
    char *s;
    point_conversion_form_t form = POINT_CONVERSION_COMPRESSED;
    ctx = BN_CTX_new();
    s = EC_POINT_point2hex(curveGroup, point, form, ctx);
    BN_CTX_free(ctx);
    return s;
}

EC_POINT *convertToPoint(const char *pointHex) {
    BN_CTX *ctx;
    EC_POINT *res;
    res = EC_POINT_new(curveGroup);
    ctx = BN_CTX_new();
    EC_POINT_hex2point(curveGroup, pointHex, res, ctx);
    BN_CTX_free(ctx);
    return res;
}

jstring encodeAndFree(JNIEnv *env, char *p1, char *p2) {
    jstring res;
    char size1, size2;
    char *buff;
    size_t s1, s2, tot;
    s1 = strlen(p1);
    s2 = strlen(p2);
    tot = s1 + s2 + 2;
    buff = (char *) malloc(tot);
    snprintf(buff, tot, "%s%s%s", p1, "?", p2);
    res = (*env)->NewStringUTF(env, buff);
    free(buff);
    free(p1);
    free(p2);
    return res;
}

int decode(JNIEnv *env, EC_POINT **R, EC_POINT **S, jstring encoded) {
    const char *fullString;
    char *rS, *sS, *local;
    char delimiter[] = "?";
    fullString = (*env)->GetStringUTFChars(env, encoded, 0);
    local = (char *) fullString;
    rS = strtok(local, delimiter);
    sS = strtok(NULL, delimiter);
    *R = convertToPoint(rS);
    *S = convertToPoint(sS);
    (*env)->ReleaseStringUTFChars(env, encoded, fullString);
    return 0;
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

BIGNUM *getPrimeOfField() {
    BIGNUM *prime, *a, *b;
    BN_CTX *ctx;
    ctx = BN_CTX_new();
    prime = BN_new();
    a = BN_new();
    b = BN_new();

    EC_GROUP_get_curve_GFp(curveGroup, prime, a, b, ctx);

    BN_free(a);
    BN_free(b);
    BN_CTX_free(ctx);
    return prime;
}

static BIGNUM *m = NULL;

static BIGNUM *mInv = NULL;

static BIGNUM *mInvNeg = NULL;

static EC_POINT *powM = NULL;

static EC_POINT *powMneg = NULL;


/*Table Creation for BSGS*/
BIGNUM *itBSGSstorage = NULL;
EC_POINT *pointBSGSstorage = NULL;

jstring bsgsMappingInMap(JNIEnv *env) {
    char *point, *num, *buff;
    size_t s1, s2, tot;
    jstring arg;
    point = convertToString(pointBSGSstorage);
    num = BN_bn2dec(itBSGSstorage);
    s1 = strlen(point);
    s2 = strlen(num);
    tot = s1 + s2 + 2;
    buff = (char *) malloc(tot);
    snprintf(buff, tot, "%s%s%s", num, ",", point);
    arg = (*env)->NewStringUTF(env, buff);
    free(point);
    free(num);
    free(buff);
    return arg;
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_getNextBSGSStorageItem(
        JNIEnv *env, jobject javaThis) {
    BN_CTX *ctx;
    jstring resTemp;
    if (itBSGSstorage == NULL) {
        itBSGSstorage = BN_new();
        BN_zero(itBSGSstorage);
        pointBSGSstorage = multiplyGenerator(itBSGSstorage);
        resTemp = bsgsMappingInMap(env);
        return resTemp;
    } else {
        ctx = BN_CTX_new();
        EC_POINT_add(curveGroup, pointBSGSstorage, pointBSGSstorage,
                     EC_GROUP_get0_generator(curveGroup), ctx);
        BN_add_word(itBSGSstorage, 1);
        BN_CTX_free(ctx);

        if (BN_cmp(itBSGSstorage, m) == -1) {
            return bsgsMappingInMap(env);
        } else {
            BN_free(itBSGSstorage);
            EC_POINT_free(pointBSGSstorage);
            pointBSGSstorage = NULL;
            itBSGSstorage = NULL;
            return (*env)->NewStringUTF(env, "empty");
        }
    }
}

void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_setUPBSGS(JNIEnv *env,
                                                                         jobject javaThis,
                                                                         jstring size,
                                                                         jstring invsize) {
    BIGNUM *temp;
    //set Params
    /*if(m!=NULL) {
        LOGI("ParamsAlreadySet");
    } else {*/
    LOGI("SetParams");
    //BN_dec2bn(&m,"16384");
    m = convertToBignum(size, env);
    //BN_dec2bn(&mInv,"262144");
    mInv = convertToBignum(invsize, env);
    mInvNeg = BN_dup(mInv);
    BN_set_negative(mInvNeg, 1);
    temp = BN_dup(m);
    powMneg = multiplyGenerator(temp);
    BN_set_negative(temp, 1);
    powM = multiplyGenerator(temp);
    BN_free(temp);
    //}

}
/*END*/

/*BabyStep-GiantStep Java interaction*/
EC_POINT *BsGsStep = NULL;
BIGNUM *curIBsGs = NULL;
EC_POINT *NegBsGsStep = NULL;
BIGNUM *negCurIBsGs = NULL;

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_computeBsGsStep(JNIEnv *env,
                                                                                  jobject javaThis,
                                                                                  jstring M) {
    const char *cMStr;
    jstring res;
    char *resC;
    BN_CTX *ctx;
    EC_POINT *MP;
    if (BsGsStep == NULL) {
        cMStr = (*env)->GetStringUTFChars(env, M, 0);
        MP = convertToPoint(cMStr);
        BsGsStep = MP;
        curIBsGs = BN_new();
        BN_zero(curIBsGs);
        (*env)->ReleaseStringUTFChars(env, M, cMStr);
    } else {
        ctx = BN_CTX_new();
        BN_add_word(curIBsGs, 1);
        EC_POINT_add(curveGroup, BsGsStep, BsGsStep, powM, ctx);
        BN_CTX_free(ctx);
        if (!BN_cmp(curIBsGs, mInv) == -1) {
            return (*env)->NewStringUTF(env, "finish");
        }
    }
    resC = convertToString(BsGsStep);
    res = (*env)->NewStringUTF(env, resC);
    free(resC);
    return res;
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_computeBsGsStepNeg(JNIEnv *env,
                                                                                     jobject javaThis,
                                                                                     jstring M) {
    const char *cMStr;
    jstring res;
    char *resC;
    BN_CTX *ctx;
    EC_POINT *MP;
    if (NegBsGsStep == NULL) {
        cMStr = (*env)->GetStringUTFChars(env, M, 0);
        MP = convertToPoint(cMStr);
        NegBsGsStep = MP;
        negCurIBsGs = BN_new();
        BN_zero(negCurIBsGs);
        (*env)->ReleaseStringUTFChars(env, M, cMStr);
    } else {
        ctx = BN_CTX_new();
        BN_sub_word(negCurIBsGs, 1);
        EC_POINT_add(curveGroup, NegBsGsStep, NegBsGsStep, powMneg, ctx);
        BN_CTX_free(ctx);
        if (!BN_cmp(negCurIBsGs, mInvNeg) == 1) {
            return (*env)->NewStringUTF(env, "finish");
        }
    }
    resC = convertToString(NegBsGsStep);
    res = (*env)->NewStringUTF(env, resC);
    free(resC);
    return res;
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_computeBsGsResult(JNIEnv *env,
                                                                                    jobject javaThis,
                                                                                    jstring value) {
    BIGNUM *it, *resMap, *mul;
    jstring result;
    BN_CTX *ctx;
    if (BsGsStep == NULL) {
        return value;
    }
    ctx = BN_CTX_new();
    resMap = convertToBignum(value, env);
    mul = BN_new();
    BN_mul(mul, curIBsGs, m, ctx);
    BN_add(resMap, resMap, mul);
    result = BigNumToString(resMap, env);
    BN_CTX_free(ctx);
    BN_free(it);
    BN_free(resMap);
    BN_free(mul);
    EC_POINT_free(BsGsStep);
    EC_POINT_free(NegBsGsStep);
    negCurIBsGs = NULL;
    NegBsGsStep = NULL;
    BsGsStep = NULL;
    curIBsGs = NULL;
    return result;
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_computeBsGsResultNeg(JNIEnv *env,
                                                                                       jobject javaThis,
                                                                                       jstring value) {
    BIGNUM *it, *resMap, *mul;
    jstring result;
    BN_CTX *ctx;
    if (NegBsGsStep == NULL) {
        return value;
    }
    ctx = BN_CTX_new();
    resMap = convertToBignum(value, env);
    mul = BN_new();
    BN_mul(mul, negCurIBsGs, m, ctx);
    BN_add(resMap, resMap, mul);
    result = BigNumToString(resMap, env);
    BN_CTX_free(ctx);
    BN_free(it);
    BN_free(resMap);
    BN_free(mul);
    BN_free(curIBsGs);
    BN_free(negCurIBsGs);
    EC_POINT_free(BsGsStep);
    EC_POINT_free(NegBsGsStep);
    negCurIBsGs = NULL;
    NegBsGsStep = NULL;
    BsGsStep = NULL;
    curIBsGs = NULL;
    return result;
}

/*END*/

/**
 * Encrypts plain with random k to ECPoints R,S
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_encryptNative(JNIEnv *env,
                                                                                jobject javaThis,
                                                                                jstring plain,
                                                                                jstring k,
                                                                                jstring pubKey) {
    EC_POINT *R, *S, *Y;
    BIGNUM *plainBn, *kBn;
    BN_CTX *ctx;
    char *string1, *string2;
    const char *pubKeystr;
    jstring ret;
    ctx = BN_CTX_new();

    pubKeystr = (*env)->GetStringUTFChars(env, pubKey, 0);
    Y = convertToPoint(pubKeystr);
    (*env)->ReleaseStringUTFChars(env, pubKey, pubKeystr);

    plainBn = convertToBignum(plain, env);
    kBn = convertToBignum(k, env);
    R = multiplyGenerator(kBn);
    S = EC_POINT_new(curveGroup);
    EC_POINT_mul(curveGroup, S, plainBn, Y, kBn, ctx);

    string1 = convertToString(R);
    string2 = convertToString(S);
    ret = encodeAndFree(env, string1, string2);

    BN_CTX_free(ctx);
    EC_POINT_free(R);
    EC_POINT_free(S);
    EC_POINT_free(Y);
    BN_free(plainBn);
    BN_free(kBn);
    return ret;
}

/**
 * Decrypts the two cipher ECPoints to ECPoint M
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_decryptNative(JNIEnv *env,
                                                                                jobject javaThis,
                                                                                jstring cipher,
                                                                                jstring secret) {
    EC_POINT *R, *S, *temp, *M;
    BIGNUM *secretB;
    BN_CTX *ctx;
    jstring res;
    char *pointPlain;

    ctx = BN_CTX_new();
    decode(env, &R, &S, cipher);
    secretB = convertToBignum(secret, env);
    BN_set_negative(secretB, 1);
    temp = multiplyConst(R, secretB);
    //LOGI(convertToString(R));
    //LOGI(BN_bn2dec(secretB));
    //LOGI(convertToString(temp));
    //LOGI(convertToString(S));

    M = EC_POINT_new(curveGroup);
    EC_POINT_add(curveGroup, M, temp, S, ctx);

    pointPlain = convertToString(M);
    res = (*env)->NewStringUTF(env, pointPlain);

    //resNum = brutforceDiscreteLog(M);
    //res = BigNumToString(resNum,env);
    //LOGI(BN_bn2dec(resNum));

    EC_POINT_free(R);
    EC_POINT_free(S);
    EC_POINT_free(temp);
    EC_POINT_free(M);
    BN_free(secretB);
    BN_CTX_free(ctx);
    free(pointPlain);
    return res;
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_addCipherNative(JNIEnv *env,
                                                                                  jobject javaThis,
                                                                                  jstring cipher1,
                                                                                  jstring cipher2) {
    EC_POINT *R1, *S1, *R2, *S2, *rR, *rS;
    char *res1, *res2;
    jstring ret;
    BN_CTX *ctx;

    ctx = BN_CTX_new();
    decode(env, &R1, &S1, cipher1);
    decode(env, &R2, &S2, cipher2);
    rR = EC_POINT_new(curveGroup);
    rS = EC_POINT_new(curveGroup);
    EC_POINT_add(curveGroup, rR, R1, R2, ctx);
    EC_POINT_add(curveGroup, rS, S1, S2, ctx);
    res1 = convertToString(rR);
    res2 = convertToString(rS);
    ret = encodeAndFree(env, res1, res2);

    EC_POINT_free(R1);
    EC_POINT_free(S1);
    EC_POINT_free(R2);
    EC_POINT_free(S2);
    EC_POINT_free(rR);
    EC_POINT_free(rS);
    BN_CTX_free(ctx);
    return ret;
}

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_computePubKey(JNIEnv *env,
                                                                                jobject javaThis,
                                                                                jstring secret) {
    BIGNUM *secretB;
    jstring res;
    EC_POINT *pubKey;
    char *strY;

    secretB = convertToBignum(secret, env);
    pubKey = multiplyGenerator(secretB);
    strY = convertToString(pubKey);
    res = (*env)->NewStringUTF(env, strY);

    BN_free(secretB);
    EC_POINT_free(pubKey);
    free(strY);
    return res;
}


/**
 * Returns the curve order
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_getCurveOrder(JNIEnv *env,
                                                                                jobject javaThis) {
    BIGNUM *order;
    jstring res;
    BN_CTX *ctx;

    ctx = BN_CTX_new();
    order = BN_new();

    EC_GROUP_get_order(curveGroup, order, ctx);
    res = BigNumToString(order, env);

    BN_free(order);
    BN_CTX_free(ctx);

    return res;
}

/**
 * Returns the prime of the underlying field
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_getPrimeOfGf(JNIEnv *env,
                                                                               jobject javaThis) {
    BIGNUM *prime;
    jstring res;

    prime = getPrimeOfField();
    res = BigNumToString(prime, env);
    BN_free(prime);

    return res;
}

/**
 * Free static curve and Point
 */
void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_tearDOWN(JNIEnv *env,
                                                                        jobject javaThis) {
    EC_GROUP_clear_free(curveGroup);
    curveGroup = NULL;
}

/**
 * Set up the curve with curveNr from OpenGL library
 * 0 -> default NIST-p192
 */
void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_setUP(JNIEnv *env, jobject javaThis,
                                                                     jint curveNr) {
    if (curveNr == 0) {
        init(NID_X9_62_prime192v1);
    } else if (curveNr == -1) {
        setTestCurve();
    } else {
        init(((int) curveNr));
    }
}

/**
 * Computes and returns arg * G
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_NativeECElGamal_computeGenTimes(JNIEnv *env,
                                                                                  jobject javaThis,
                                                                                  jstring num) {
    EC_POINT *point;
    BIGNUM *numB, *res;
    char *temp;
    jstring returnVal;

    numB = convertToBignum(num, env);
    point = multiplyGenerator(numB);
    temp = convertToString(point);

    returnVal = (*env)->NewStringUTF(env, temp);
    
    EC_POINT_free(point);
    BN_free(numB);
    BN_free(res);
    free(temp);
    return returnVal;
}
