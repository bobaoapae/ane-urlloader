//
// Created by User on 31/07/2024.
//

#ifndef URLLOADERNATIVELIBRARY_H
#define URLLOADERNATIVELIBRARY_H

#ifdef _WIN32
#include "windows.h"
#else
#include "dlfcn.h"
#endif

bool loadNativeLibrary();
void *getFunctionPointer(const char *functionName);
int initializerLoader(void* callBackSuccess, void* callBackError, void* callBackProgress, void* callBackLog);
char *startLoader(const char *url, const char *method, const char *variables,  const char *headers);
void addStaticHost(const char* host, const char* ipAddress);
void removeStaticHost(const char* host);
void freeId(const char *id);


#endif //URLLOADERNATIVELIBRARY_H
