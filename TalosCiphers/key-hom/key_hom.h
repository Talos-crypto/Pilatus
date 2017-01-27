//
// Created by Lukas Burkhalter on 15.07.16.
//

#include <openssl/ec.h>
#include <openssl/obj_mac.h>
#include <openssl/rand.h>
#include <assert.h>
#include <openssl/sha.h>
#include <openssl/rc4.h>
#include <openssl/bn.h>
#include <stdint.h>

#ifndef KEY_HOM_KEY_HOM_H
#define KEY_HOM_KEY_HOM_H



struct key_hom_cipher {
    EC_POINT **points;
    int num_points;
    unsigned char *iv;
    size_t iv_length;
};
typedef struct key_hom_cipher KHC_Cipher;

/**
 * Free a KHC-Cipher
 */
int KHC_Cipher_clear(KHC_Cipher *cipher);

/**
 * Encodes a KHC cipher to a byte array of the following form:
 * iv_length u16_bit|num_points u16bit|iv|EC_points......
 *
 * group: the used elliptic curve
 * cipher: the cipher to encode
 * buffer: (output) the byte array
 * len: (output) the length of the data
 *
 */
int KHC_Cipher_encode(EC_GROUP *group, KHC_Cipher *cipher, unsigned char **buffer, size_t *len);


/**
 * Decodes a byte array to a KHC cipher
 *
 * group: the used elliptic curve
 * buffer: the byte array
 * len: the length of the data
 * cipher: (output) the output cipher
 */
int KHC_Cipher_decode(EC_GROUP *group, unsigned char *buffer, size_t len, KHC_Cipher **cipher);

struct key_hom_key {
    BIGNUM *key;
    EC_GROUP *group;
};
typedef struct key_hom_key KHC_Key;

/**
 * Creates a new key homomoprhic key.
 * (With default parameter)
 */
KHC_Key *KHC_new_key();

/**
 * Advanced: Creates a new key based on the provided params
 *
 * group: the corresponding EC-Curve group
 * num_bytes: the size of the key
 */
KHC_Key *KHC_new_key_params(EC_GROUP *group, int num_bytes);

/**
 * Free a KHC_key
 */
int KHC_Key_clear(KHC_Key *cipher);

/**
 * Encodes a key to a byte array of the form ec-curve id u16_bit|key_bytes
 *
 * key: the key to encode
 * buffer: (output) the output byte array
 * len: the length of the buffer
 */
int KHC_Key_encode(KHC_Key *key, unsigned char **buffer, size_t *len);

/**
 * Decodes a byte array to a KHC cipher
 *
 * buffer: the byte array
 * len: the length of the byte array
 * cipher: (output) the output byte array
 */
int KHC_Key_decode(unsigned char *buffer, size_t len, KHC_Key **cipher);

/**
 * Encrypts a given plaintext pt with the key homomoprhic block cipher.
 *
 * pt: pointer to the plaintext data
 * pt_len: number of bytes of the plaintext data
 * key: the corresponding key
 * use_const_iv: 0 if a random iv should be used, 1 if a consant iv should be used (i.e RND oder DET)
 * res: (output) result pointer to the cipher
 */
int KHC_encrypt(unsigned char *pt, size_t pt_len, KHC_Key *key, int use_const_iv, KHC_Cipher **res);

/**
 * Decrypts key homomoprhic ciphertext to the plaintext value.
 *
 * cipher: the key homomoprhic ciphertext
 * key: the key homomoprhic key
 * res: (output) pointer to the resulting plaintext data
 * res_len: (output) length of the resutling plaintext data
 */
int KHC_decrypt(KHC_Cipher *cipher, KHC_Key *key, unsigned char **res, size_t *res_len);

/**
 * Re-encrypts a homomoprhic ciphertext to a new homomoprhic ciphertext given a re encryption token.
 *
 * cipher: old cipher
 * re_key: re-encryption key
 * res_cipher: (output) the resilting re-encrypted cipher
 */
int KHC_re_encrypt(KHC_Cipher *cipher, KHC_Key *re_key, KHC_Cipher **res_cipher);

/**
 * Creates a re-encryption token from Alice's to Bob's key given the key of Alice and the key of Bob.
 *
 * from_key: key of Alice
 * to_key: key of Bob
 * re_key: (output) the resulting re-encryption token
 */
int KHC_create_re_enc_token(KHC_Key *from_key, KHC_Key *to_key, KHC_Key **re_key);

#endif //KEY_HOM_KEY_HOM_H
