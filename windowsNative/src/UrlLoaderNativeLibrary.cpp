#include <iostream>
#include <memory>
#include <string>
#include <Windows.h>
#include "UrlLoaderNativeLibrary.h"
#include <log.h>

// Use a unique_ptr with a custom deleter to manage the library handle
static std::unique_ptr<HMODULE, decltype(&FreeLibrary)> library(nullptr, FreeLibrary);

std::string GetLibraryLocation(int argc, char *argv[]) {
    std::string baseDirectory;

    // Check for -extdir argument
    for (int i = 0; i < argc; ++i) {
        if (std::string(argv[i]) == "-extdir" && i + 1 < argc) {
            baseDirectory = argv[i + 1];
            baseDirectory += R"(\br.com.redesurftank.aneurlloader.ane)";
            break;
        }
    }

    if (baseDirectory.empty()) {
        char buffer[MAX_PATH];
        DWORD length = GetModuleFileNameA(nullptr, buffer, MAX_PATH);
        if (length == 0) {
            std::cerr << "Error getting module file name: " << GetLastError() << std::endl;
            return "";
        }
        baseDirectory = std::string(buffer, length);
        baseDirectory = baseDirectory.substr(0, baseDirectory.find_last_of("\\/"));
        baseDirectory += R"(\META-INF\AIR\extensions\br.com.redesurftank.aneurlloader)";
    }

    return baseDirectory + R"(\META-INF\ANE\Windows-x86\UrlLoaderNativeLibrary.dll)";
}

bool loadNativeLibrary() {
    if (library) {
        std::cerr << "Library is already loaded." << std::endl;
        return true;
    }

    auto libraryPath = GetLibraryLocation(__argc, __argv);
    writeLog(("Loading native library from: " + libraryPath).c_str());

    HMODULE handle = LoadLibraryA(libraryPath.c_str());
    if (!handle) {
        std::cerr << "Could not load library: " << GetLastError() << std::endl;
        writeLog("Could not load library");
        return false;
    }

    library.reset(&handle);  // Pass handle directly, not &handle
    writeLog("Library loaded successfully");
    return true;
}

void* getFunctionPointer(const char* functionName) {
    if (!library && !loadNativeLibrary()) {
        return nullptr;
    }

    void* func = GetProcAddress(*library, functionName);  // Use library.get() instead of *library
    if (!func) {
        std::cerr << "Could not load function: " << GetLastError() << std::endl;
        writeLog("Could not load function");
    } else {
        writeLog(("Function loaded: " + std::string(functionName)).c_str());
    }

    return func;
}

int initializerLoader(void* callBackSuccess, void* callBackError, void* callBackProgress, void* callBackLog) {
    writeLog("Calling initializerLoader");

    using InitializerFunc = int (*)(void*, void*, void*, void*);
    auto func = reinterpret_cast<InitializerFunc>(getFunctionPointer("initializerLoader"));

    if (!func) {
        writeLog("Could not load function initializerLoader");
        return -1;
    }

    int result = func(callBackSuccess, callBackError, callBackProgress, callBackLog);
    writeLog(("InitializerLoader Result: " + std::to_string(result)).c_str());
    return result;
}

char* startLoader(const char* url, const char* method, const char* variables, const char* headers) {
    writeLog("Calling startLoader");

    using StartLoaderFunc = char* (*)(const char*, const char*, const char*, const char*);
    auto func = reinterpret_cast<StartLoaderFunc>(getFunctionPointer("startLoad"));

    if (!func) {
        writeLog("Could not load function startLoader");
        return nullptr;
    }

    char* result = func(url, method, variables, headers);
    if (result) {
        writeLog(("startLoader Result: " + std::string(result)).c_str());
    } else {
        writeLog("startLoader returned null");
    }

    return result;
}

void freeId(const char* id) {
    writeLog("Calling freeId");

    using FreeIdFunc = void (*)(const char*);
    auto func = reinterpret_cast<FreeIdFunc>(getFunctionPointer("freeId"));

    if (func) {
        func(id);
        writeLog("freeId finished");
    } else {
        writeLog("Could not load function freeId");
    }
}