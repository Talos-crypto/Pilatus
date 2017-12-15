#!/bin/bash
LOCAL_PATH=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
CUR_PATH=$(pwd)

FITBIT_APP_NAME="FitBitAppWS"
AVA_APP_NAME="TalosCloudAva"
SENSOR_APP_NAME="SensorAppWS"

FITBIT_DB_NAME="TalosFitbit"
AVA_DB_NAME="AvaDataset"
SENSOR_DB_NAME="SensorAppDB"

echo "glassfish 4 must be running"

echo "setup glassfish backend applications"
bash $LOCAL_PATH/TalosHelper/deployTalosBEApplication.sh $FITBIT_APP_NAME $FITBIT_DB_NAME

bash $LOCAL_PATH/TalosHelper/deployTalosBEApplication.sh $AVA_APP_NAME $AVA_DB_NAME

bash $LOCAL_PATH/TalosHelper/deployTalosBEApplication.sh $SENSOR_APP_NAME $SENSOR_DB_NAME

echo "setup databases"
echo "login to mysql db as root"
mysql -u root -p < $LOCAL_PATH/TalosAndroid/AppSQLBackendScripts/AVAApp.sql
mysql -u root -p < $LOCAL_PATH/TalosAndroid/AppSQLBackendScripts/FitbitAPP.sql
mysql -u root -p < $LOCAL_PATH/TalosAndroid/AppSQLBackendScripts/SensorAPP.sql
