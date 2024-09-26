//
//  UrlLoaderNativeLibrary.hpp
//  UrlLoaderANE
//
//  Created by João Vitor Borges on 26/08/24.
//

#ifndef UrlLoaderNativeLibrary_hpp
#define UrlLoaderNativeLibrary_hpp

#include <stdio.h>

extern "C" {
    // Force the use of the exact symbol names from the library
    int __cdecl initializerLoader(void* callBackSuccess, void* callBackError, void* callBackProgress, void* callBackLog);
    char* __cdecl startLoad(const char* url, const char* method, const char* variables, const char* headers);
    void __cdecl freeId(const char* id);
}

#endif /* UrlLoaderNativeLibrary_hpp */
