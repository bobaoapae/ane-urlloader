#include "UrlLoaderSupport.hpp"

static UrlLoaderSupport* staticUrlSupport = new UrlLoaderSupport();
static FREContext g_ctx;

UrlLoaderSupport::UrlLoaderSupport()
: _numFunctions(3)
{
    _functions = new FRENamedFunction[_numFunctions];
    _functions[0].name = (const uint8_t*)"initialize";
    _functions[0].function = UrlLoaderSupport::expose_initialize;
    _functions[1].name = (const uint8_t*)"loadUrl";
    _functions[1].function = UrlLoaderSupport::expose_loadUrl;
    _functions[2].name = (const uint8_t*)"getResult";
    _functions[2].function = UrlLoaderSupport::expose_getResult;
}

UrlLoaderSupport::~UrlLoaderSupport() {
    delete[] _functions;
    _functions = nullptr;
}

uint32_t UrlLoaderSupport::numFunctions() const {
    return _numFunctions;
}

FRENamedFunction* UrlLoaderSupport::getFunctions() const {
    return _functions;
}

void UrlLoaderSupport::SuccessCallback(const char *id, uint8_t *result, int32_t length) {
    writeLog("Calling SuccessCallback");

    std::string id_str(id);
    writeLog(("ID: " + id_str).c_str());
    writeLog(("Result Length: " + std::to_string(length)).c_str());

    if (staticUrlSupport) {
        std::lock_guard lock(staticUrlSupport->_resultsMutex);
        staticUrlSupport->_results.insert({id_str, std::vector<uint8_t>(result, result + length)});
        writeLog("Result stored");
    } else {
        writeLog("Loader Support not found to store result");
    }

    FREDispatchStatusEventAsync(g_ctx, (const uint8_t *)"success", (const uint8_t *)id_str.c_str());
    writeLog("Dispatched success event");
}

void UrlLoaderSupport::ErrorCallback(const char *id, const char *message) {
    std::string id_str(id);
    std::string message_str(message);
    std::string concatenated = id_str + ";" + message_str;

    FREDispatchStatusEventAsync(g_ctx, (const uint8_t *)"error", (const uint8_t *)concatenated.c_str());
}

void UrlLoaderSupport::ProgressCallback(const char *id, const char *message) {
    std::string id_str(id);
    std::string message_str(message);
    std::string concatenated = id_str + ";" + message_str;

    FREDispatchStatusEventAsync(g_ctx, (const uint8_t *)"progress", (const uint8_t *)concatenated.c_str());
}

void UrlLoaderSupport::WriteLogCallback(const char *message) {
    std::string msg = "C#: ";
    msg += message;
    writeLog(msg.c_str());
}

FREObject UrlLoaderSupport::expose_loadUrl(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]) {
    writeLog("Calling expose_loadUrl");

    uint32_t stringLength;
    const uint8_t *url;
    FREGetObjectAsUTF8(argv[0], &stringLength, &url);
    writeLog(("URL: " + std::string((const char *)url)).c_str());

    uint32_t methodLength;
    const uint8_t *method;
    FREGetObjectAsUTF8(argv[1], &methodLength, &method);
    writeLog(("Method: " + std::string((const char *)method)).c_str());

    uint32_t variableLength;
    const uint8_t *variable;
    FREGetObjectAsUTF8(argv[2], &variableLength, &variable);
    writeLog(("Variable: " + std::string((const char *)variable)).c_str());

    uint32_t headersLength;
    const uint8_t *headers;
    FREGetObjectAsUTF8(argv[3], &headersLength, &headers);
    writeLog(("Headers: " + std::string((const char *)headers)).c_str());

    char *result = startLoad((const char *)url, (const char *)method, (const char *)variable, (const char *)headers);

    if (!result) {
        writeLog("startLoader returned null");
        return nullptr;
    }

    writeLog(("Result: " + std::string(result)).c_str());

    FREObject resultStr;
    FRENewObjectFromUTF8(strlen(result), (const uint8_t *)result, &resultStr);
    free(result);
    return resultStr;
}

FREObject UrlLoaderSupport::expose_getResult(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]) {
    writeLog("Calling expose_getResult");

    uint32_t uuidLength;
    const uint8_t *uuid;
    FREGetObjectAsUTF8(argv[0], &uuidLength, &uuid);
    std::string uuidStr(reinterpret_cast<const char *>(uuid), uuidLength);
    writeLog(("GetResult ID: " + uuidStr).c_str());

    std::vector<uint8_t> result;
    if (staticUrlSupport) {
        std::lock_guard lock(staticUrlSupport->_resultsMutex);
        auto it = staticUrlSupport->_results.find(uuidStr);
        if (it != staticUrlSupport->_results.end()) {
            result = it->second;
            staticUrlSupport->_results.erase(it);
            writeLog("Result found");
        }
    } else {
        writeLog("LoaderSupport not found");
    }

    FREObject byteArrayObject = nullptr;
    if (!result.empty()) {
        writeLog("Creating AS3 ByteArray");
        FREByteArray byteArray;
        byteArray.length = result.size();
        byteArray.bytes = result.data();

        FRENewByteArray(&byteArray, &byteArrayObject);
        writeLog("AS3 ByteArray created");
    } else {
        writeLog("Result is empty");
    }

    result.clear();
    return byteArrayObject;
}

FREObject UrlLoaderSupport::expose_initialize(FREContext ctx, void *functionData, uint32_t argc, FREObject argv[]) {
    writeLog("Calling expose_initialize");
    auto result = initializerLoader((void*)&SuccessCallback, (void*)&ErrorCallback, (void*)&ProgressCallback, (void*)&WriteLogCallback);
    writeLog(("InitializerLoader Result: " + std::to_string(result)).c_str());

    FREObject resultBool;
    FRENewObjectFromBool(result == 1, &resultBool);
    return resultBool;
}

void UrlLoaderSupport::ContextInitializer(void *extData, const uint8_t *ctxType, FREContext ctx, uint32_t *numFunctionsToSet, const FRENamedFunction **functionsToSet) {
    FRESetContextNativeData(ctx, staticUrlSupport);

    g_ctx = ctx;

    if (numFunctionsToSet) *numFunctionsToSet = staticUrlSupport->numFunctions();
    if (functionsToSet) *functionsToSet = staticUrlSupport->getFunctions();
}

void UrlLoaderSupport::ContextFinalizer(FREContext ctx) {
    UrlLoaderSupport* loaderSupport = nullptr;
    if (FRE_OK == FREGetContextNativeData(ctx, reinterpret_cast<void**>(&loaderSupport))) {
        delete loaderSupport;
    }
}

extern "C" {

__attribute__ ((visibility ("default")))
void InitExtension(void** extDataToSet, FREContextInitializer* ctxInitializerToSet, FREContextFinalizer* ctxFinalizerToSet) {
    writeLog("InitExtension called");
    if (extDataToSet) *extDataToSet = nullptr;
    if (ctxInitializerToSet) *ctxInitializerToSet = UrlLoaderSupport::ContextInitializer;
    if (ctxFinalizerToSet) *ctxFinalizerToSet = UrlLoaderSupport::ContextFinalizer;
    writeLog("InitExtension completed");
}

__attribute__ ((visibility ("default")))
void DestroyExtension(void* extData) {
    writeLog("DestroyExtension called");
    closeLog();
}
}
