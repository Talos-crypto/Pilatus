#!/bin/bash
LOCAL_PATH=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
CUR_PATH=$(pwd)

bash $LOCAL_PATH/install_relic.sh
cd $CUR_PATH
bash $LOCAL_PATH/installCloudBackend.sh
cd $CUR_PATH