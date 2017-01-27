cmake_minimum_required(VERSION 2.8)
project(key-hom)

INCLUDE_DIRECTORIES(/opt/local/include)
find_library(Crypto crypto PATHS /opt/local/lib)
find_library(SSL ssl PATHS /opt/local/lib)

set( CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/bin )

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O2")
set(SOURCE_FILES testing.cpp key_hom.h key_hom.c)
add_executable(key-hom ${SOURCE_FILES})
target_link_libraries(key-hom crypto ssl)

set(SOURCE_FILES benchmark_key_hom_udf.cpp key_hom.c key_hom.h)
add_executable(key_hom_udf_bench ${SOURCE_FILES})
target_link_libraries(key_hom_udf_bench crypto ssl mysqlclient pthread z m rt dl)

INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/includes /usr/include/mysql)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fabi-version=2 -fno-omit-frame-pointer -O3")
add_library(KEY_HOM_UPDATE SHARED key-hom-udf.c key_hom.c key_hom.h)
target_link_libraries(KEY_HOM_UPDATE crypto ssl)

install(TARGETS key-hom DESTINATION bin)
install(TARGETS KEY_HOM_UPDATE DESTINATION bin)
