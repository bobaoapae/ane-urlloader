#ifndef URL_LOADER_SUPPORT_HPP
#define URL_LOADER_SUPPORT_HPP

#include <cstdio>
#include <string>
#include <map>
#include <mutex>
#include <vector>
#include "log.hpp"
typedef void* NSWindow; // don't need this..
#include <FlashRuntimeExtensions.h>  // Adobe AIR runtime includes
#include "UrlLoaderNativeLibrary.hpp"

class UrlLoaderSupport {
private:
    std::map<std::string, std::vector<uint8_t>> _results;
    std::mutex _resultsMutex;
    uint32_t _numFunctions;
    FRENamedFunction* _functions;

public:
    UrlLoaderSupport();
    ~UrlLoaderSupport();

    uint32_t numFunctions() const;
    FRENamedFunction* getFunctions() const;

    static void SuccessCallback(const char *id, uint8_t *result, int32_t length);
    static void ErrorCallback(const char *id, const char *message);
    static void ProgressCallback(const char *id, const char *message);
    static void WriteLogCallback(const char *message);

    static FREObject expose_loadUrl(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]);
    static FREObject expose_getResult(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]);
    static FREObject expose_initialize(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]);

    static void ContextInitializer(void *extData, const uint8_t *ctxType, FREContext ctx, uint32_t *numFunctionsToSet, const FRENamedFunction **functionsToSet);
    static void ContextFinalizer(FREContext ctx);
};

#endif // URL_LOADER_SUPPORT_HPP
