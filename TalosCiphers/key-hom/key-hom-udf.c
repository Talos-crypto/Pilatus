//
// Created by lubu on 25.01.17.
//

/**
 * Implements a MySQL UDF function for key updates
 * */

#ifdef STANDARD
/* STANDARD is defined, don't use any mysql functions */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#ifdef __WIN__
typedef unsigned __int64 ulonglong;/* Microsofts 64 bit types */
typedef __int64 longlong;
#else
typedef unsigned long long ulonglong;
typedef long long longlong;
#endif /*__WIN__*/
#else

#include <my_global.h>
#include <my_sys.h>

#if defined(MYSQL_SERVER)
#include <m_string.h>/* To get strmov() */
#else
/* when compiled as standalone */
#include <string.h>

#define strmov(a, b) stpcpy(a,b)
#define bzero(a, b) memset(a,0,b)
#define memcpy_fixed(a, b, c) memcpy(a,b,c)
#endif
#endif

#include <mysql.h>
#include "key_hom.h"


#ifdef HAVE_DLOPEN

#if !defined(HAVE_GETHOSTBYADDR_R) || !defined(HAVE_SOLARIS_STYLE_GETHOST)
#endif

my_bool KEY_HOM_UPDATE_init(UDF_INIT *initid, UDF_ARGS *args,
                           char *error) {
    if (args->arg_count != 2||
        args->arg_type[0] != STRING_RESULT||
        args->arg_type[1] != STRING_RESULT) {
        strcpy(error, "Usage: KEY_HOM_UPDATE(encoded cipher, re-token)");
        return 1;
    }
    return 0;
}

char *KEY_HOM_UPDATE(UDF_INIT *initid, UDF_ARGS *args,
                    char *result, unsigned long *length,
                    char *is_null, char *error) {
    size_t cipher_encoded_length = (size_t) args->lengths[0];
    char *cipher_encoded = (char *) args->args[0];
    size_t token_encoded_length = (size_t) args->lengths[1];
    char *token_encoded = (char *) args->args[1];

    KHC_Key *token;
    KHC_Cipher *cipher, *res;
    unsigned char* out;
    size_t res_size;

    KHC_Key_decode((unsigned char *) token_encoded, token_encoded_length, &token);
    KHC_Cipher_decode(token->group, (unsigned char *) cipher_encoded, cipher_encoded_length, &cipher);


    KHC_re_encrypt(cipher, token, &res);

    KHC_Cipher_encode(token->group, res, &out, &res_size);
    *length = (unsigned long) res_size;

    KHC_Key_clear(token);
    KHC_Cipher_clear(cipher);
    KHC_Cipher_clear(res);
    return (char *) out;
}

my_bool KEY_HOM_UPDATE_deinit(UDF_INIT *initid) {
    return 0;
}

#endif /* HAVE_DLOPEN */