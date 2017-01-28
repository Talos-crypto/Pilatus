#!/bin/bash
LOCAL_PATH=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
CUR_PATH=(pwd)

if [ -z "$1" ]
  then
    echo "argument <app_name> required"
    exit 1
fi

TALOS_APP_NAME=$1


if !(test -f $LOCAL_PATH/../TalosCloud/out/artifacts/TalosCloudWeb/TalosCloudWeb.war)
then
	echo "Build Talos WS"
	PKG_ANT_OK=$(dpkg-query -W --showformat='${Status}' ant | grep "install ok installed")
	echo Checking for ant: $PKG_ANT_OK
	if [ "" == "$PKG_ANT_OK" ]; then
	  echo "No ant. Setting up ant."
	  sudo apt-get install ant || { echo "failed installing ant"; exit 1; }
	fi
	command -v ant >/dev/null 2>&1 || { echo >&2 "ANT is required but it's not installed."; exit 1; }
	cd $LOCAL_PATH/../TalosCloud || { echo "change path failed"; exit 1; }
	bash buildANT.sh
	cd $LOCAL_PATH || { echo "change path failed"; exit 1; }
else
	echo "Build already exists.."
fi

GF_CMD=$LOCAL_PATH/../glassfish4/bin/asadmin

mv $LOCAL_PATH/../TalosCloud/out/artifacts/TalosCloudWeb/TalosCloudWeb.war $LOCAL_PATH/$TALOS_APP_NAME.war
$GF_CMD start-domain
$GF_CMD deploy $LOCAL_PATH/$TALOS_APP_NAME.war


echo "Create Database for APP. Type in DB name"
read dbname
dbuser=$dbname
dbpwd=talos

echo "CREATE DATABASE $dbname;CREATE USER '$dbuser'@'localhost' IDENTIFIED BY '$dbpwd';GRANT ALL PRIVILEGES ON $dbname.* TO '$dbuser'@'localhost';" >> temp.sql
mysql -u root -p < temp.sql
rm -f temp.sql

APP_POOL="$TALOS_APP_NAME"Pool
APP_RESOURCE="$TALOS_APP_NAME"Res

$GF_CMD create-jdbc-connection-pool --driverclassname com.mysql.jdbc.Driver --restype java.sql.Driver --property user=$dbuser:password=$dbpwd:url="jdbc\:mysql\://localhost\:3306/$dbname" $APP_POOL
$GF_CMD create-jdbc-resource --connectionpoolid $APP_POOL $APP_RESOURCE
$GF_CMD restart-domain


export PROPERTIES=$'# This file defines the properties for the Talos EE Application 
# The server client id key from Google, needed for Authetication 
# https://developers.google.com/identity/sign-in/android/backend-auth 
googleAuthServerID = <google sign-in server id> 
# The address of the identity provider for authentication 
identityProviderAddress = https://accounts.google.com 

# Indicates if debug authentication should be used 
debugAuthentication = false 

# The name of the MySql database connection pool resource
# https://docs.oracle.com/cd/E19316-01/820-4335/gibzk/index.html
dbConnPoolName = $APP_RESOURCE'

echo "$PROPERTIES" > $LOCAL_PATH/../glassfish4/glassfish/domains/domain1/config/$TALOS_APP_NAME.properties

echo "Edit $LOCAL_PATH/../glassfish4/glassfish/domains/domain1/config/$TALOS_APP_NAME.properties and add google sign in id"

cd $CUR_PATH