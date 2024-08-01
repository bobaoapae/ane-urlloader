#ifndef WINDOWSNATIVE_LIBRARY_H
#define WINDOWSNATIVE_LIBRARY_H

#include <FlashRuntimeExtensions.h>

extern "C" {
    __declspec(dllexport) void InitExtension(void** extDataToSet, FREContextInitializer* ctxInitializerToSet, FREContextFinalizer* ctxFinalizerToSet);
    __declspec(dllexport) void DestroyExtension(void* extData);
}

#endif //WINDOWSNATIVE_LIBRARY_H