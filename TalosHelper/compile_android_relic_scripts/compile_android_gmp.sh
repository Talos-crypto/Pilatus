mkdir android_lib
STANDALONE_TOOLCHAIN="/home/lubu/Documents/TalosJob/android-toolchain"
export SYSROOT="$STANDALONE_TOOLCHAIN/sysroot"
export CC="$STANDALONE_TOOLCHAIN/bin/arm-linux-androideabi-gcc --sysroot=$SYSROOT"
export CXX="$STANDALONE_TOOLCHAIN/bin/arm-linux-androideabi-g++ --sysroot=$SYSROOT"
export AR="$STANDALONE_TOOLCHAIN/bin/arm-linux-androideabi-ar"
export RANLIB="$STANDALONE_TOOLCHAIN/bin/arm-linux-androideabi-ranlib"
#export CFLAGS="-O4 -flto -Ofast -mcpu=cortex-a15 -fprefetch-loop-arrays -mfpu=neon -funroll-all-loops -mtune=cortex-a15 -ftree-vectorize -fomit-frame-pointer -mvectorize-with-neon-quad -mthumb-interwork -finline-small-functions  -ffast-math -marm -ffunction-sections -fdata-sections -fomit-frame-pointer -finline-small-functions "
./configure --host=arm-linux-androideabi --prefix=$(pwd)/android_lib --disable-shared CFLAGS="-v -std=c99 -march=armv7-a -mfloat-abi=softfp -mfpu=neon"
echo "edit config.h  #define HAVE_LOCALECONV 0"
read

make
make install
