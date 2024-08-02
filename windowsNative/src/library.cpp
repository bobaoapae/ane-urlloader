#include <log.h>
#include <map>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>
#ifdef WIN32
#include <winsock2.h>
#include <windows.h>
#include <cstdint>
typedef unsigned __int32 uint32_t;
typedef unsigned __int8 uint8_t;
typedef __int32 int32_t;
#endif
#include "library.h"
#include <cstdio>
#include "UrlLoaderNativeLibrary.h"

static FREContext g_ctx = nullptr;
static std::map<std::string, std::vector<uint8_t> > g_results = {};
static std::mutex g_results_mutex;

BOOL APIENTRY DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            initLog();
            writeLog("DLL loaded (DLL_PROCESS_ATTACH)");
            break;
        case DLL_PROCESS_DETACH:
            writeLog("DLL unloaded (DLL_PROCESS_DETACH)");
            closeLog();
            break;
    }
    return TRUE;
}

void SucessCallback(const char *id, uint8_t *result, int32_t length) {
    writeLog("Calling SucessCallback");
    std::string id_str(id);

    writeLog("ID: ");
    writeLog(id_str.c_str());
    writeLog("Result Length: ");
    writeLog(std::to_string(length).c_str());

    // Concatenar strings com ';' separador
    std::string concatenated = id_str + ";";

    writeLog("Storing result");
    writeLog("Storing result"); {
        std::lock_guard lock(g_results_mutex);
        g_results.insert(std::pair(id_str, std::vector(result, result + length)));
    }
    writeLog("Result stored");

    FREDispatchStatusEventAsync(g_ctx, (const uint8_t *) "success", (const uint8_t *) concatenated.c_str());
    writeLog("Dispatched success event");
}

void ErrorCallback(const char *id, const char *message) {
    std::string id_str(id);
    std::string message_str(message);

    // Concatenar strings com ';' separador
    std::string concatenated = id_str + ";" + message_str;

    FREDispatchStatusEventAsync(g_ctx, (const uint8_t *) "error", (const uint8_t *) concatenated.c_str());
}

void ProgressCallback(const char *id, const char *message) {
    std::string id_str(id);
    std::string message_str(message);

    // Concatenar strings com ';' separador
    std::string concatenated = id_str + ";" + message_str;

    FREDispatchStatusEventAsync(g_ctx, (const uint8_t *) "progress", (const uint8_t *) concatenated.c_str());
}

void WriteLogCallback(const char *message) {
    std::string msg = "C#: ";
    msg += message;
    writeLog(msg.c_str());
}

FREObject expose_loadUrl(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]) {
    writeLog("Calling expose_loadUrl");
    uint32_t stringLength;
    const uint8_t *url;
    FREGetObjectAsUTF8(argv[0], &stringLength, &url);

    writeLog("URL: ");
    writeLog((const char *) url);

    uint32_t methodLength;
    const uint8_t *method;
    FREGetObjectAsUTF8(argv[1], &methodLength, &method);

    writeLog("Method: ");
    writeLog((const char *) method);

    uint32_t variableLength;
    const uint8_t *variable;
    FREGetObjectAsUTF8(argv[2], &variableLength, &variable);

    writeLog("Variable: ");
    writeLog((const char *) variable);

    uint32_t headersLength;
    const uint8_t *headers;
    FREGetObjectAsUTF8(argv[3], &headersLength, &headers);

    writeLog("Headers: ");
    writeLog((const char *) headers);

    writeLog("Calling startLoader");

    char *result = startLoader((const char *) url, (const char *) method, (const char *) variable, (const char *) headers);

    if (!result) {
        writeLog("startLoader returned null");
        return nullptr;
    }

    writeLog("Result: ");
    writeLog(result);

    // return result as FREObject string
    FREObject resultStr;
    FRENewObjectFromUTF8(strlen(result), (const uint8_t *) result, &resultStr);
    free(result);
    return resultStr;
}


FREObject expose_getResult(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]) {
    writeLog("Calling expose_getResult");

    uint32_t uuidLength;
    const uint8_t *uuid;
    FREGetObjectAsUTF8(argv[0], &uuidLength, &uuid);
    std::string uuidStr(reinterpret_cast<const char *>(uuid), uuidLength);

    std::vector<uint8_t> result; {
        std::lock_guard lock(g_results_mutex);
        auto it = g_results.find(uuidStr);
        if (it != g_results.end()) {
            result = it->second;
            g_results.erase(it);
        }
    }

    writeLog("Filling AS3 ByteArray");

    FREObject byteArrayObject = nullptr;
    if (!result.empty()) {
        writeLog("Creating AS3 ByteArray");
        FREByteArray byteArray;
        byteArray.length = result.size();
        byteArray.bytes = result.data();

        FRENewByteArray(&byteArray, &byteArrayObject);
        writeLog("AS3 ByteArray created");
    }

    result.clear();
    return byteArrayObject;
}

FREObject expose_initialize(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]) {
    writeLog("Calling expose_initialize");
    auto result = initializerLoader(&SucessCallback, &ErrorCallback, &ProgressCallback, &WriteLogCallback);
    writeLog("InitializerLoader Result: ");
    writeLog(std::to_string(result).c_str());

    //return true
    FREObject resultBool;
    FRENewObjectFromBool(result == 1, &resultBool);
    return resultBool;
}

void ContextInitializer(void *extData, const uint8_t *ctxType, FREContext ctx, uint32_t *numFunctionsToSet, const FRENamedFunction **functionsToSet) {
    static FRENamedFunction arrFunctions[] = {
        {(const uint8_t *) "initialize", NULL, &expose_initialize},
        {(const uint8_t *) "loadUrl", NULL, &expose_loadUrl},
        {(const uint8_t *) "getResult", NULL, &expose_getResult},
    };

    g_ctx = ctx;

    *functionsToSet = arrFunctions;
    *numFunctionsToSet = sizeof(arrFunctions) / sizeof(arrFunctions[0]);
}

extern "C" {
__declspec(dllexport) void InitExtension(void **extDataToSet, FREContextInitializer *ctxInitializerToSet, FREContextFinalizer *ctxFinalizerToSet) {
    *extDataToSet = nullptr;
    *ctxInitializerToSet = ContextInitializer;
    *ctxFinalizerToSet = DestroyExtension;
}

__declspec(dllexport) void DestroyExtension(void *extData) {
}
}
