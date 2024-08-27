//
//  UrlLoaderNativeLibrary.cpp
//  UrlLoaderANE
//
//  Created by João Vitor Borges on 26/08/24.
//

#include "UrlLoaderNativeLibrary.hpp"
#include <iostream>
#include <log.hpp>
#include <string>
#include <dlfcn.h>
#include <limits.h>
#include <crt_externs.h>

static void *library = nullptr;

static int g_argc = 0;
static char** g_argv = nullptr;

void initializeCommandLineArgumentsMacOS() {
    char** environ_argv = *_NSGetArgv();
    g_argc = *_NSGetArgc();
    g_argv = new char*[g_argc + 1];
    for (int i = 0; i < g_argc; ++i) {
        size_t len = strlen(environ_argv[i]);
        g_argv[i] = new char[len + 1];
        strcpy(g_argv[i], environ_argv[i]);
    }
    g_argv[g_argc] = nullptr;
}

// This function will be called automatically when the shared library is loaded
__attribute__((constructor)) void initializeLibrary() {
    initializeCommandLineArgumentsMacOS();
}

std::string GetLibraryLocation() {
    std::string baseDirectory;

    // Check if the argument -extdir was passed
    for (int i = 0; i < g_argc; ++i) {
        if (std::string(g_argv[i]) == "-extdir" && i + 1 < g_argc) {
            baseDirectory = g_argv[i + 1];
            baseDirectory += "/br.com.redesurftank.aneurlloader.ane";
        }
    }
    
    if(baseDirectory.empty()){
        char buffer[PATH_MAX];
        if (realpath(g_argv[0], buffer) == nullptr) {
            std::cerr << "Error getting real path: " << strerror(errno) << std::endl;
            return "";
        }
        
        std::string fullPath(buffer);
        baseDirectory = fullPath.substr(0, fullPath.find_last_of("/"));
        baseDirectory += "/META-INF/AIR/extensions/br.com.redesurftank.aneurlloader";
    }
    
    return baseDirectory + "/META-INF/ANE/MacOS-x86-64/UrlLoaderNativeLibrary.dylib";
}

void *loadNativeLibrary() {
    if (library) {
        std::cerr << "Library is already loaded." << std::endl;
        return library;
    }
    writeLog("Loading native library");
    auto libraryPath = GetLibraryLocation();
    writeLog(libraryPath.c_str());
#ifdef _WIN32
    HINSTANCE handle = LoadLibraryA(libraryPath.c_str());
#else
    void *handle = dlopen(libraryPath.c_str(), RTLD_LAZY);
#endif

    if (!handle) {
#ifdef _WIN32
        std::cerr << "Could not load library: " << GetLastError() << std::endl;
#else
        std::cerr << "Could not load library: " << dlerror() << std::endl;
#endif
        writeLog("Could not load library");
        return nullptr;
    }

    writeLog("Library loaded");
    library = handle;
    return library;
}

void *getFunctionPointer(const char *functionName) {
    loadNativeLibrary();
    if (!library) {
        std::cerr << "Library is not loaded." << std::endl;
        return nullptr;
    }

    void *func = nullptr;
#ifdef _WIN32
    func = GetProcAddress((HINSTANCE) library, functionName);
#else
    func = dlsym(library, functionName);
#endif

    if (!func) {
#ifdef _WIN32
        std::cerr << "Could not load function: " << GetLastError() << std::endl;
#else
        std::cerr << "Could not load function: " << dlerror() << std::endl;
#endif
        writeLog("Could not load function");
    }

    std::string msg = "Function loaded: " + std::string(functionName);
    writeLog(msg.c_str());
    return func;
}

// As funções initializerLoader, startLoader e freeId permanecem as mesmas


int initializerLoader(void *callBackSuccess, void *callBackError, void *callBackProgress, void *callBackLog) {
    writeLog("Calling initializerLoader");
    typedef int (*myFunc)(void *, void *, void *, void *);
    auto func = (myFunc) getFunctionPointer("initializerLoader");
    if (!func) {
        writeLog("Could not load function initializerLoader");
        return -1;
    }

    writeLog("InitializerLoader called");
    auto result = func(callBackSuccess, callBackError, callBackProgress, callBackLog);
    std::string resultMsg = "InitializerLoader Result: " + std::to_string(result);
    writeLog(resultMsg.c_str());
    return result;
}

char *startLoader(const char *url, const char *method, const char *variables, const char *headers) {
    writeLog("Calling startLoader");
    typedef char *(*myFunc)(const char *, const char *, const char *, const char *);
    auto func = (myFunc) getFunctionPointer("startLoad");
    if (!func) {
        writeLog("Could not load function startLoader");
        return nullptr;
    }

    writeLog("startLoader called");
    auto result = func(url, method, variables, headers);

    if (!result) {
        writeLog("startLoader returned null");
        return nullptr;
    }

    std::string resultMsg = "startLoader Result: " + std::string(result);
    writeLog(resultMsg.c_str());
    return result;
}

void freeId(const char *id) {
    writeLog("Calling freeId");
    typedef void (*myFunc)(const char *);
    auto func = (myFunc) getFunctionPointer("freeId");
    if (!func) {
        writeLog("Could not load function freeId");
        return;
    }

    writeLog("freeId called");
    func(id);
    writeLog("freeId finished");
}
