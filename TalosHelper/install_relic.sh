#!/bin/bash
PKG_CMAKE_OK=$(dpkg-query -W --showformat='${Status}\n' cmake | grep "install ok installed")
echo Checking for cmake: $PKG_CMAKE_OK
if [ "" == "$PKG_CMAKE_OK" ]; then
  echo "No cmake. Setting up cmake."
  sudo apt-get install g++
  sudo apt-get install cmake
fi

PKG_GMP_OK=$(dpkg-query -W --showformat='${Status}\n' libgmp3-dev | grep "install ok installed")
echo Checking for libgmp3-dev: $PKG_GMP_OK
if [ "" == "$PKG_GMP_OK" ]; then
  echo "No libgmp3-dev. Setting up libgmp3-dev."
  sudo apt-get install libgmp3-dev
fi

git clone https://github.com/relic-toolkit/relic.git

cd relic 

./preset/gmp-pbc-128.sh
make
sudo make install
