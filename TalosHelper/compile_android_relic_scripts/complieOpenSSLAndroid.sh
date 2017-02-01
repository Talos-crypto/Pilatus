tar xzvf openssl-1.1.0-pre5_android.tar.gz
cd openssl-1.1.0-pre5
# Type here your path to the ndk
export NDK=/Users/lukas/Documents/TalosJobExperiments/android-ndk-r12b
$NDK/build/tools/make-standalone-toolchain.sh --platform=android-18 --toolchain=arm-linux-androideabi-4.9 --install-dir=`pwd`/android-toolchain-arm
export TOOLCHAIN_PATH=`pwd`/android-toolchain-arm/bin
export CROSS_SYSROOT=`pwd`/android-toolchain-arm/sysroot
export TOOL=arm-linux-androideabi
export NDK_TOOLCHAIN_BASENAME=${TOOLCHAIN_PATH}/${TOOL}
export CC=$NDK_TOOLCHAIN_BASENAME-gcc
export CXX=$NDK_TOOLCHAIN_BASENAME-g++
export LINK=${CXX}
export LD=$NDK_TOOLCHAIN_BASENAME-ld
export AR=$NDK_TOOLCHAIN_BASENAME-ar
export RANLIB=$NDK_TOOLCHAIN_BASENAME-ranlib
export STRIP=$NDK_TOOLCHAIN_BASENAME-strip
./Configure android-armeabi -D__ARM_MAX_ARCH__=7
export PATH=$TOOLCHAIN_PATH:$PATH
make build_libs
