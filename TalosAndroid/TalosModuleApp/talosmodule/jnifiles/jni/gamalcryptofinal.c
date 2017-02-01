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
#include <khash.h>
#include <inttypes.h>


#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%s",__VA_ARGS__)
#define  LOGINUM(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%d",__VA_ARGS__)
#define  LOGIFolat(...)  __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%f",__VA_ARGS__)

#define kh_set(kname, hash, key, val) ({int ret; k = kh_put(kname, hash,key,&ret); kh_value(hash,k) = val; ret;})

KHASH_MAP_INIT_STR(LookUPTable, int)

static int curve = NID_X9_62_prime192v1;

//Elliptic Curve
static EC_GROUP *curveGroup = NULL;

//LookupTable
static khash_t(LookUPTable) *table = NULL;
static int tableSize;
static EC_POINT *mG = NULL;
static EC_POINT *mGInv = NULL;
//LookupTable END


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

EC_POINT *multiplyConstInt(const EC_POINT *in, int64_t n) {
	BIGNUM *bnI;
	EC_POINT *res;
	bnI = BN_new();
	BN_zero(bnI);
	BN_add_word(bnI, (BN_ULONG) n);
	res = multiplyConst(in, bnI);
	BN_free(bnI);
	return res;
}

EC_POINT *multiplyGenerator(const BIGNUM *n) {
	return multiplyConst(EC_GROUP_get0_generator(curveGroup), n);
}

EC_POINT *multiplyGeneratorIneteger(int i) {
	BIGNUM *bnI;
	EC_POINT *res;
	bnI = BN_new();
	BN_zero(bnI);
	BN_add_word(bnI, (BN_ULONG) i);
	res = multiplyConst(EC_GROUP_get0_generator(curveGroup), bnI);
	BN_free(bnI);
	return res;
}

BIGNUM *convertToBignum(jstring str, JNIEnv* env) {
	BIGNUM *res;
	const char *nativeStr;
	res = NULL;
	nativeStr = (*env)->GetStringUTFChars(env, str, 0);
	BN_dec2bn(&res, nativeStr);
	(*env)->ReleaseStringUTFChars(env, str, nativeStr);
	return res;
}

jstring *BigNumToString(BIGNUM *num, JNIEnv* env) {
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

EC_POINT *convertToPoint(const char* pointHex) {
	BN_CTX *ctx;
	EC_POINT *res;
	res = EC_POINT_new(curveGroup);
	ctx = BN_CTX_new();
	EC_POINT_hex2point(curveGroup, pointHex, res, ctx);
	BN_CTX_free(ctx);
	return res;
}

jstring encodeAndFree(JNIEnv* env, char *p1, char*p2) {
	jstring res;
	char size1, size2;
	char * buff;
	size_t s1, s2, tot;
	s1 = strlen(p1);
	s2 = strlen(p2);
	tot = s1 + s2 + 2;
	buff = (char*) malloc(tot);
	snprintf(buff, tot, "%s%s%s", p1, "?", p2);
	res = (*env)->NewStringUTF(env, buff);
	free(buff);
	free(p1);
	free(p2);
	return res;
}

int decode(JNIEnv* env, EC_POINT **R, EC_POINT **S, jstring encoded) {
	const char *fullString;
	char *rS, *sS, *local;
	char delimiter[] = "?";
	fullString = (*env)->GetStringUTFChars(env, encoded, 0);
	local = (char*) fullString;
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

// LOOKUPTABLE
int getFromTable(const char* key) {
	khiter_t k;
	int res;
	k = kh_get(LookUPTable, table, key); //
	if (k == kh_end(table)) {
		return -1;
	} else {
		res = kh_val(table,k);
	}
	return res;
}

void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_initTable(JNIEnv* env,
		jobject javaThis) {
	if (table != NULL) {
		kh_destroy(LookUPTable, table);
		table = NULL;
	}
	table = kh_init(LookUPTable);
}

void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_destroyTable(JNIEnv* env,
		jobject javaThis) {
	int k;
	if (table != NULL) {
		for (k = kh_begin(table); k != kh_end(table); ++k) {
			if (kh_exist(table, k)) {
				free((char*) kh_key(table, k));
			}
		}
		kh_destroy(LookUPTable, table);
		table = NULL;
	}
	if (mG != NULL) {
		EC_POINT_free(mG);
		EC_POINT_free(mGInv);
		mG = NULL;
		mGInv = NULL;
	}
	LOGI("Table Destoryed");
}

void setTableSizeConstants(int size) {
	BIGNUM *bnSize;
	bnSize = BN_new();
	BN_zero(bnSize);
	BN_add_word(bnSize, (BN_ULONG) size);
	tableSize = (int) size;
	mG = multiplyGenerator(bnSize);
	BN_set_negative(bnSize, 1);
	mGInv = multiplyGenerator(bnSize);
	BN_free(bnSize);
}

void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_setTableSizeConstants(
		JNIEnv* env, jobject javaThis, jint size) {
	setTableSizeConstants((int) size);
}

void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_computeTable(JNIEnv* env,
		jobject javaThis, jint size) {
	int count = 0;
	char *curString;
	EC_POINT * curPoint;
	const EC_POINT *gen;
	khiter_t k;
	BN_CTX *ctx = BN_CTX_new();
	setTableSizeConstants((int) size);
	gen = EC_GROUP_get0_generator(curveGroup);
	curPoint = EC_POINT_new(curveGroup);
	EC_POINT_set_to_infinity(curveGroup, curPoint);
	while (count <= size) {
		curString = convertToString(curPoint);
		kh_set(LookUPTable, table, curString, count);
		EC_POINT_add(curveGroup, curPoint, curPoint, gen, ctx);
		count = count + 1;
	}
	LOGINUM(count);
	EC_POINT_free(curPoint);
	BN_CTX_free(ctx);

	/*
	 LOGI("Table--------------------------------");
	 for (k = kh_begin(table); k != kh_end(table); ++k) {
	 if (kh_exist(table, k)) {
	 const char *key = kh_key(table,k);
	 int tval = kh_value(table, k);
	 __android_log_print(ANDROID_LOG_INFO,"ECCRYPT","%s -> %d",key,tval);
	 }
	 }
	 LOGI("TableEND-----------------------------");
	 */
}

jint Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_putInTable(JNIEnv* env,
		jobject javaThis, jstring key, jint value) {
	int ret;
	khiter_t k;
	char * keyValue;
	size_t keySize = (size_t)(*env)->GetStringLength(env, key);
	if (table == NULL) {
		return 1;
	}
	const char *keyStr = (*env)->GetStringUTFChars(env, key, 0);
	keyValue = malloc(keySize + 1);
	keyValue[keySize] = '\0';
	strncpy(keyValue, keyStr, keySize);
	kh_set(LookUPTable, table, keyValue, (int) value);
	(*env)->ReleaseStringUTFChars(env, key, keyStr);
	return 0;
}

jint Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_getFromTable(JNIEnv* env,
		jobject javaThis, jstring key) {
	int res;
	const char *keyStr = (*env)->GetStringUTFChars(env, key, 0);
	res = getFromTable(keyStr);
	(*env)->ReleaseStringUTFChars(env, key, keyStr);
	return res;
}

// LOOKUPTABLE END

// ECDLP

int getPowerFromTable(EC_POINT *point) {
	int res;
	char *strPoint = convertToString(point);
	res = getFromTable(strPoint);
	free(strPoint);
	return res;
}

int64_t computePower(int64_t i, int64_t j) {
	return i * tableSize + j;
}

int solveECDLPBsGs(int64_t *result, EC_POINT *M, int64_t maxIt, uint8_t doSigned) {
	int64_t j = 0, i = 0, iNeg = 0;
	EC_POINT *curPoint = EC_POINT_dup(M, curveGroup);
	EC_POINT *curPointNeg = EC_POINT_dup(M, curveGroup);
	BN_CTX *ctx = BN_CTX_new();

	while (i <= maxIt) {
		j = (int64_t) getPowerFromTable(curPoint);
		if (j != -1) {
			*result = computePower(i, j);
			break;
		}
		EC_POINT_add(curveGroup, curPoint, curPoint, mGInv, ctx);
		i = i + 1;
		if(doSigned) {
			j = (int64_t) getPowerFromTable(curPointNeg);
			if (j != -1) {
				*result = computePower(iNeg, j);
				break;
			}
			EC_POINT_add(curveGroup, curPointNeg, curPointNeg, mG, ctx);
			iNeg = iNeg - 1;
		}
	}

	if(i>maxIt) {
		return -1;
	}

	EC_POINT_free(curPoint);
	EC_POINT_free(curPointNeg);
	BN_CTX_free(ctx);

	return 0;
}

/**
 * Solves ECDLP pos
 */
jlong Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_solveECDLPBsGsPos(JNIEnv* env,
		jobject javaThis, jobject state, jstring M, jlong startI, jlong endI) {
	const char *Mstr;
	EC_POINT *Mpoint, *temp;
	BN_CTX *ctx = BN_CTX_new();
	int64_t i,j, maxi, res;
	int found = 1, MUTEX;
    jclass cls = (*env)->GetObjectClass(env,state);
    jfieldID fieldId = (*env) ->GetFieldID(env,cls, "found", "I");


	Mstr = (*env)->GetStringUTFChars(env, M, 0);
	Mpoint = convertToPoint(Mstr);
	(*env)->ReleaseStringUTFChars(env, M, Mstr);
	EC_POINT *curPoint = EC_POINT_new(curveGroup);
	temp = multiplyConstInt(mGInv,(int64_t) startI);
	EC_POINT_add(curveGroup, curPoint, Mpoint, temp, ctx);
	EC_POINT_free(temp);

	i = (int64_t) startI;
	maxi = (int64_t) endI;
	while(1) {
		j = (int64_t) getPowerFromTable(curPoint);
		if (j != -1) {
			res = computePower(i, j);
			break;
		}
		EC_POINT_add(curveGroup, curPoint, curPoint, mGInv, ctx);
		i = i + 1;
		MUTEX = (int)  (*env)->GetIntField(env,state, fieldId);
		if(i>maxi || 0!= MUTEX) {
			found = 0;
			break;
		}
	}

	EC_POINT_free(curPoint);
	EC_POINT_free(Mpoint);
	BN_CTX_free(ctx);

	if(found) {
		return (jlong) res;
	} else {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "IntNotFound");
	}
}

jlong Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_solveECDLPBsGsNeg(JNIEnv* env,
		jobject javaThis, jobject state, jstring M, jlong startI, jlong endI) {
	const char *Mstr;
	EC_POINT *Mpoint, *temp;
	BN_CTX *ctx = BN_CTX_new();
	int64_t i,j, maxi, res;
	int found = 1, MUTEX;
	jclass cls = (*env)->GetObjectClass(env,state);
	jfieldID fieldId = (*env) ->GetFieldID(env,cls, "found", "I");

	Mstr = (*env)->GetStringUTFChars(env, M, 0);
	Mpoint = convertToPoint(Mstr);
	(*env)->ReleaseStringUTFChars(env, M, Mstr);
	EC_POINT *curPoint = EC_POINT_new(curveGroup);
	temp = multiplyConstInt(mG,(int64_t) startI);
	EC_POINT_add(curveGroup, curPoint, Mpoint, temp, ctx);
	EC_POINT_free(temp);

	i = -((int64_t) startI);
	maxi = -((int64_t) endI);
	while(1) {
		j = (int64_t) getPowerFromTable(curPoint);
		if (j != -1) {
			res = computePower(i, j);
			break;
		}
		EC_POINT_add(curveGroup, curPoint, curPoint, mG, ctx);
		i = i - 1;
		MUTEX = (int)  (*env)->GetIntField(env,state, fieldId);
		if(i<maxi || 0!= MUTEX) {
			found = 0;
			break;
		}
	}

	EC_POINT_free(curPoint);
	EC_POINT_free(Mpoint);
	BN_CTX_free(ctx);

	if(found) {
		return (jlong) res;
	} else {
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "IntNotFound");
	}
}

// ECDLP END

/**
 * Encrypts plain with random k to ECPoints R,S
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_encryptNative(JNIEnv* env,
		jobject javaThis, jstring plain, jstring k, jstring pubKey) {
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
 * Solves ECDLP
 */
jlong Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_solveECDLPBsGs(JNIEnv* env,
		jobject javaThis, jstring M, jlong maxIt, jboolean doSigned) {
	const char *Mstr;
	EC_POINT *Mpoint;
	int64_t res;
	Mstr = (*env)->GetStringUTFChars(env, M, 0);
	Mpoint = convertToPoint(Mstr);
	if(solveECDLPBsGs(&res, Mpoint, (int64_t) maxIt, (uint8_t) doSigned)) {
		(*env)->ReleaseStringUTFChars(env, M, Mstr);
		EC_POINT_free(Mpoint);
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Result Not Found");
	}
	(*env)->ReleaseStringUTFChars(env, M, Mstr);
	EC_POINT_free(Mpoint);
	return (jlong) res;
}

/**
 * Decrypts the two cipher ECPoints to ECPoint M
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_decryptNative(JNIEnv* env,
		jobject javaThis, jstring cipher, jstring secret) {
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

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_addCipherNative(JNIEnv* env,
		jobject javaThis, jstring cipher1, jstring cipher2) {
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

jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_computePubKey(JNIEnv* env,
		jobject javaThis, jstring secret) {
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
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_getCurveOrder(JNIEnv* env,
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
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_getPrimeOfGf(JNIEnv* env,
		jobject javaThis) {
	BIGNUM *prime;
	jstring res;

	prime = getPrimeOfField();
	res = BigNumToString(prime, env);
	BN_free(prime);

	return res;
}

/**
 * Free static curve
 */
void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_tearDOWN(JNIEnv* env,
		jobject javaThis) {
	if (curveGroup != NULL) {
		EC_GROUP_clear_free(curveGroup);
	}
	curveGroup = NULL;
}

/**
 * Set up the curve with curveNr from OpenSSL library
 * 0 -> default NIST-p192
 */
void Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_setUP(JNIEnv* env,
		jobject javaThis, jint curveNr) {
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
 *
 */
jstring Java_ch_ethz_inf_vs_talosmodule_cryptoalg_FastECElGamal_computeGenTimes(JNIEnv* env,
		jobject javaThis, jstring num) {
	EC_POINT *point;
	BIGNUM *numB, *res;
	char* temp;
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
