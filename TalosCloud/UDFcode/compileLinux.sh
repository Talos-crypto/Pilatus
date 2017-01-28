#!/bin/bash

LOCAL_PATH=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
MY_SQL_LOC="/usr/lib/mysql"

PKG_MYSQL_DEV_OK=$(dpkg-query -W --showformat='${Status}\n' libmysqlclient-dev | grep "install ok installed")
echo Checking for libmysqlclient-dev: $PKG_MYSQL_DEV_OK
if [ "" == "$PKG_MYSQL_DEV_OK" ]; then
  echo "No libmysqlclient-dev. Setting up libmysqlclient-dev."
  sudo apt-get install libmysqlclient-dev
fi

PKG_SSLDEV_DEV_OK=$(dpkg-query -W --showformat='${Status}\n' libssl-dev | grep "install ok installed")
echo Checking for libssl-dev: $PKG_SSLDEV_DEV_OK
if [ "" == "$PKG_SSLDEV_DEV_OK" ]; then
  echo "No libssl-dev. Setting up libssl-dev."
  sudo apt-get install libssl-dev
fi

PKG_CMAKE_OK=$(dpkg-query -W --showformat='${Status}\n' cmake | grep "install ok installed")
echo Checking for cmake: $PKG_CMAKE_OK
if [ "" == "$PKG_CMAKE_OK" ]; then
  echo "No cmake. Setting up cmake."
  sudo apt-get install cmake
fi

PKG_JDK_OK=$(dpkg-query -W --showformat='${Status}\n' default-jdk | grep "install ok installed")
echo Checking for default-jdk: $PKG_JDK_OK
if [ "" == "$PKG_JDK_OK" ]; then
  echo "No default-jdk. Setting up default-jdk."
  sudo apt-get install default-jdk || { echo "failed installing jdk"; exit 1; }
fi

gcc -w -g $(mysql_config --cflags) -shared -fPIC  -o ECElgamalUDF.so ECElgamalUDF.c  -lcrypto -lssl
if [ $? -eq 0 ]; then
    sudo mv ECElgamalUDF.so $MY_SQL_LOC/plugin/ECElgamalUDF.so
else
    echo "Compile ECElgamalUDF FAILED"
fi

gcc -w -g $(mysql_config --cflags) -shared -fPIC  -o PaillierUDF.so PaillierUDF.c  -lcrypto -lssl
if [ $? -eq 0 ]; then
    sudo mv PaillierUDF.so $MY_SQL_LOC/plugin/PaillierUDF.so
else
    echo "Compile PaillierUDF FAILED"
fi

gcc $(mysql_config --cflags) -shared -fPIC -o mOPE_AGR.so mOPEUDF.c
if [ $? -eq 0 ]; then
    sudo mv mOPE_AGR.so $MY_SQL_LOC/plugin/mOPE_AGR.so
else
    echo "Compile mOPE_AGR FAILED"
fi

gcc -w -g $(mysql_config --cflags) -shared -fPIC  -o CRT_GAMAL_AGR.so CRT_GAMAL_UDF.c  -lcrypto -lssl
if [ $? -eq 0 ]; then
    sudo mv CRT_GAMAL_AGR.so $MY_SQL_LOC/plugin/CRT_GAMAL_AGR.so
else
    echo "Compile CRT_GAMAL_AGR FAILED"
fi


cd ../../TalosCiphers/pre-relic
cmake .
make
if [ $? -eq 0 ]; then
    sudo mv libPRE_RELIC_UDF.so $MY_SQL_LOC/plugin/PRE_RELIC_UDF.so
else
    echo "Compile CRT_GAMAL_AGR FAILED"
fi

sudo cp /usr/local/lib/librelic.so /usr/lib/

echo "Create mysql UDFs on DB with user: root"
mysql -hlocalhost -uroot -p < createUDF.sql
if [ $? -eq 0 ]; then
	echo "Success :)"
  sudo service mysql restart
else
    echo "Failed :("
fi
