#!/bin/bash
LOCAL_PATH=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
CUR_PATH=(pwd)

sudo apt-get update

echo "Step 1.1: Setup MySQL database"

PKG_MYSQL_OK=$(dpkg-query -W --showformat='${Status}\n' mysql-server | grep "install ok installed")
echo Checking for mysql-server: $PKG_MYSQL_OK
if [ "" == "$PKG_MYSQL_OK" ]; then
  echo "No mysql-server. Setting up mysql-server."
  sudo apt-get install mysql-server || { echo "failed installing mysql"; exit 1; }
fi

echo "Step 1.2: Compile and install UDF's"
cd $LOCAL_PATH/../TalosCloud/UDFcode
bash ./compileLinux.sh
cd $LOCAL_PATH

echo "Step 2: Install glassfish"

echo "Step 2.1: Install JDK and unzip function"
PKG_JDK_OK=$(dpkg-query -W --showformat='${Status}\n' default-jdk | grep "install ok installed")
echo Checking for default-jdk: $PKG_JDK_OK
if [ "" == "$PKG_JDK_OK" ]; then
  echo "No default-jdk. Setting up default-jdk."
  sudo apt-get install default-jdk || { echo "failed installing jdk"; exit 1; }
fi

PKG_UNZIP_OK=$(dpkg-query -W --showformat='${Status}\n' unzip | grep "install ok installed")
echo Checking for unzip: $PKG_UNZIP_OK
if [ "" == "$PKG_UNZIP_OK" ]; then
  echo "No unzip function. Setting up unzip function."
  sudo apt-get install unzip || { echo "failed installing unzip"; exit 1; }
fi

cd $LOCAL_PATH/../
wget http://download.java.net/glassfish/4.1.1/release/glassfish-4.1.1.zip
unzip glassfish-4.1.1.zip

cd ./glassfish4/glassfish/domains/domain1/lib/ext/
wget https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.40.zip
unzip mysql-connector-java-5.1.40.zip 
mv ./mysql-connector-java-5.1.40/mysql-connector-java-5.1.40-bin.jar  .
rm -r ./mysql-connector-java-5.1.40
rm mysql-connector-java-5.1.40.zip


./glassfish4/bin/asadmin change-admin-password
./glassfish4/bin/asadmin start-domain
./glassfish4/bin/asadmin enable-secure-admin

cd $CUR_PATH