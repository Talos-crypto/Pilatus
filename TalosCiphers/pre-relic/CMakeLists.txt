cmake_minimum_required(VERSION 2.8)
project(relic_hom)

INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/includes /usr/lib/jvm/default-java/include /usr/lib/jvm/default-java/include/linux /usr/include/mysql /usr/local/include)

set(RELIC_LOC /usr/local/lib/librelic.so)
#set(RELIC_LIB ${CMAKE_CURRENT_SOURCE_DIR}/libs/gmp-pbc-128/librelic.so)
#set(RELIC_LIB ${CMAKE_CURRENT_SOURCE_DIR}/libs/gmp-pbc-128-ss/librelic.so)
#set(RELIC_LIB ${CMAKE_CURRENT_SOURCE_DIR}/libs/gmp-pbc-80/librelic.so)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fabi-version=2 -fno-omit-frame-pointer -std=c++11 -o2" )
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fabi-version=2 -fno-omit-frame-pointer -g -Wall -march=native -Wl,-rpath=/usr/local/lib -o2")

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/bin )

set(SOURCE_FILES main.cpp pre-hom.c pre-relic-udf.c pre-hom.h test_pre_udf.h)
add_executable(relic_hom ${SOURCE_FILES})

set(SOURCE_FILES test_relic_udf.cpp pre-hom.c pre-hom.h)
add_executable(relic_hom_test_udf ${SOURCE_FILES})

set(SOURCE_FILES benchamark_udf.cpp pre-hom.c pre-hom.h)
add_executable(relic_hom_benchmark ${SOURCE_FILES})

set(SOURCE_FILES benchmark_pre.cpp pre-hom.c pre-hom.h)
add_executable(relic_pre_re_enc_benchmark ${SOURCE_FILES})

set(SOURCE_FILES benchamark_udf_plain.cpp pre-hom.c pre-hom.h)
add_executable(plain_db_benchmark ${SOURCE_FILES})

add_library(relic-proxy-re-enc SHARED proxy-re-wrapper.c pre-hom.c pre-hom.h)

target_link_libraries(relic_hom ${RELIC_LOC})
target_link_libraries(relic-proxy-re-enc ${RELIC_LOC})
target_link_libraries(relic_pre_re_enc_benchmark ${RELIC_LOC})
target_link_libraries(relic_hom_test_udf ${RELIC_LOC})
target_link_libraries(relic_hom_benchmark ${RELIC_LOC} mysqlclient pthread z m rt dl)
target_link_libraries(plain_db_benchmark ${RELIC_LOC} mysqlclient pthread z m rt dl)


INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/includes /usr/include/mysql /usr/local/include)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fabi-version=2 -fno-omit-frame-pointer")
add_library(PRE_RELIC_UDF SHARED pre-relic-udf.c pre-hom.c pre-hom.h)
target_link_libraries(PRE_RELIC_UDF ${RELIC_LOC})