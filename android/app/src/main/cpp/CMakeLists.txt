cmake_minimum_required(VERSION 3.22.1)
project("FECunionNative")

add_library(fecunion SHARED
    jni_bridge.cpp
)

target_include_directories(fecunion PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

find_library(log-lib log)

target_link_libraries(fecunion ${log-lib})
