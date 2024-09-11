cmake_minimum_required(VERSION 3.28)
project(windowsNative)

set(CMAKE_CXX_STANDARD 17)

# Definindo as opções de compilação para Debug e Release
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} /D_ITERATOR_DEBUG_LEVEL=2 /MTd")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /D_ITERATOR_DEBUG_LEVEL=0 /MT")

if(CMAKE_SIZEOF_VOID_P EQUAL 8)
    message(STATUS "Compiling for x64")
    set(LIBRARY_PATH "C:/AIRSDKs/AIRSDK_51.1.1.3/lib/win64")
    set(INCLUDE_PATH "C:/AIRSDKs/AIRSDK_51.1.1.3/include")
else()
    message(STATUS "Compiling for x86")
    set(LIBRARY_PATH "C:/AIRSDKs/AIRSDK_51.1.1.3/lib/win")
    set(INCLUDE_PATH "C:/AIRSDKs/AIRSDK_51.1.1.3/include")
endif()

if(VCPKG_TARGET_TRIPLET MATCHES "-static$")
    message(STATUS "Compiling static")
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
else()
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreadedDLL$<$<CONFIG:Debug>:Debug>")
endif()

link_directories(${LIBRARY_PATH})
include_directories(${INCLUDE_PATH})
include_directories(${CMAKE_SOURCE_DIR}/src)

add_library(windowsNative SHARED
        src/library.h
        src/library.cpp
        src/UrlLoaderNativeLibrary.h
        src/UrlLoaderNativeLibrary.cpp
        src/log.h
        src/log.cpp
)

target_link_libraries(windowsNative PRIVATE ${LIBRARY_PATH}/FlashRuntimeExtensions.lib)

set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /O2 /GL")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} /LTCG /OPT:REF /OPT:ICF /DEBUG /PDBALTPATH:%_PDB%")