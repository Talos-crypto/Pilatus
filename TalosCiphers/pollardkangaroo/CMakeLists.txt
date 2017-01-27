cmake_minimum_required(VERSION 3.3)
project(PollardKangaroo)

INCLUDE_DIRECTORIES(/opt/local/include)
find_library(Crypto crypto PATHS /opt/local/lib)
find_library(SSL ssl PATHS /opt/local/lib)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -O2")
set(SOURCE_FILES main.cpp PollardKang.c PollardKang.h)
add_executable(PollardKangaroo ${SOURCE_FILES})
target_link_libraries(PollardKangaroo crypto ssl)

install(TARGETS PollardKangaroo DESTINATION bin)