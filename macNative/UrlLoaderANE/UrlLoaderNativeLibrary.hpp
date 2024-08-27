//
//  UrlLoaderNativeLibrary.hpp
//  UrlLoaderANE
//
//  Created by João Vitor Borges on 26/08/24.
//

#ifndef UrlLoaderNativeLibrary_hpp
#define UrlLoaderNativeLibrary_hpp

#include <stdio.h>

void *loadNativeLibrary();
void *getFunctionPointer(const char *functionName);
int initializerLoader(void* callBackSuccess, void* callBackError, void* callBackProgress, void* callBackLog);
char *startLoader(const char *url, const char *method, const char *variables,  const char *headers);
void freeId(const char *id);

#endif /* UrlLoaderNativeLibrary_hpp */
