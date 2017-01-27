cmake_minimum_required(VERSION 3.5)
project(ecelgamal_relic)

INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/includes /usr/local/include)
set(RELIC_LOC /usr/local/lib/librelic.so)
#set(RELIC_LIB ${CMAKE_CURRENT_SOURCE_DIR}/libs/gmp-pbc-128/librelic.so)
#set(RELIC_LIB ${CMAKE_CURRENT_SOURCE_DIR}/libs/gmp-pbc-80/librelic.so)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/bin )
set(SOURCE_FILES main.cpp ecelgamal-relic.h ecelgamal-relic.c)
add_executable(ecelgamal_relic ${SOURCE_FILES})

set(SOURCE_FILES benchmark_ecelgamal_relic.cpp ecelgamal-relic.h ecelgamal-relic.c)
add_executable(ecelgamal_relic_bench ${SOURCE_FILES})

set(SOURCE_FILES benchmark_ecelgamal_udf.cpp ecelgamal-relic.h ecelgamal-relic.c)
add_executable(ecelgamal_relic_bench_udf ${SOURCE_FILES})

target_link_libraries(ecelgamal_relic ${RELIC_LOC})
target_link_libraries(ecelgamal_relic_bench ${RELIC_LOC})
target_link_libraries(ecelgamal_relic_bench_udf ${RELIC_LOC} mysqlclient pthread z m rt dl)



INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/includes /usr/local/include /usr/include/mysql)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fabi-version=2 -fno-omit-frame-pointer -O3")
add_library(ECG_RELIC_UDF SHARED ecelgamal_relic_udf.c ecelgamal-relic.h ecelgamal-relic.c)
add_library(ECG_RELIC_UDF_CRT SHARED ecelgamal_relic_udf_crt.c ecelgamal-relic.h ecelgamal-relic.c)
target_link_libraries(ECG_RELIC_UDF ${RELIC_LOC})
target_link_libraries(ECG_RELIC_UDF_CRT ${RELIC_LOC})