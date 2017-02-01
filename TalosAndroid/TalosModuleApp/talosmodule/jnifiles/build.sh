#!/bin/bash
export NDK_PROJECT_PATH=`pwd`
DESTPATH=$(cd ..; pwd)/src/main/jniLibs/armeabi-v7
NDK_DIR="/opt/android-ndk"

if $NDK_DIR/ndk-build; then
	cp -a libs/armeabi-v7a $DESTPATH
	rm -r libs
	rm -r obj
else 
	echo "Build Failed"
	exit 1
fi



