export MY_SQL_LOC=/usr/local/mysql
export STATIC_CRYPTO_LIB=/opt/local/lib/libcrypto.a
export STATIC_CRYPTO_LIB_INCLUDE=/Users/lukas/Documents/BscThesis/opensslincludes
export STATIC_SSL_LIB=/opt/local/lib/libssl.a

gcc -bundle -bundle_loader $MY_SQL_LOC/bin/mysqld -w -o ECElgamalUDF.so -I$STATIC_CRYPTO_LIB_INCLUDE -I/usr/local/mysql/include -Os -g -fno-strict-aliasing -arch x86_64 $STATIC_CRYPTO_LIB $STATIC_SSL_LIB ECElgamalUDF.c
if [ $? -eq 0 ]; then
    sudo mv ECElgamalUDF.so $MY_SQL_LOC/lib/plugin/ECElgamalUDF.so
else
    echo "Compile ECElgamalUDF FAILED"
fi

gcc -bundle -bundle_loader $MY_SQL_LOC/bin/mysqld -w -o PaillierUDF.so -I$STATIC_CRYPTO_LIB_INCLUDE -I/usr/local/mysql/include -Os -g -fno-strict-aliasing -arch x86_64 $STATIC_CRYPTO_LIB $STATIC_SSL_LIB PaillierUDF.c
if [ $? -eq 0 ]; then
    sudo mv PaillierUDF.so $MY_SQL_LOC/lib/plugin/PaillierUDF.so
else
    echo "Compile Paillier FAILED"
fi

gcc -bundle -bundle_loader $MY_SQL_LOC/bin/mysqld -w -o mOPE_AGR.so -I/usr/local/mysql/include -Os -g -fno-strict-aliasing -arch x86_64 mOPEUDF.c
if [ $? -eq 0 ]; then
    sudo mv mOPE_AGR.so $MY_SQL_LOC/lib/plugin/mOPE_AGR.so
else
    echo "Compile mOPE FAILED"
fi


gcc -bundle -bundle_loader $MY_SQL_LOC/bin/mysqld -w -o CRT_GAMAL_AGR.so -I$STATIC_CRYPTO_LIB_INCLUDE -I/usr/local/mysql/include -Os -g -fno-strict-aliasing -arch x86_64 $STATIC_CRYPTO_LIB $STATIC_SSL_LIB CRT_GAMAL_UDF.c
if [ $? -eq 0 ]; then
    sudo mv CRT_GAMAL_AGR.so $MY_SQL_LOC/lib/plugin/CRT_GAMAL_AGR.so
else
    echo "Compile CRT_GAMAL_UDF FAILED"
fi