#include "log.h"
#include <cstdio>
#include <exception>

FILE *logFile = nullptr;

void initLog() {
    try {
        fopen_s(&logFile, "C:/debug/dll_log.txt", "a");
        if (logFile != nullptr) {
            fprintf(logFile, "Log initialized\n");
            fflush(logFile);
        }
    } catch (std::exception &e) {
        fprintf(stderr, "Error initializing log: %s\n", e.what());
    }
}

void writeLog(const char *message) {
    //print to console too
    fprintf(stdout, "%s\n", message);
    if (logFile != nullptr) {
        fprintf(logFile, "%s\n", message);
        fflush(logFile);
    }
}

void closeLog() {
    if (logFile != nullptr) {
        fprintf(logFile, "Log closed\n");
        fclose(logFile);
        logFile = nullptr;
    }
}
