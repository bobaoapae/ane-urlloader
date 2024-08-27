#include "log.hpp"
#include <cstdio>
#include <exception>
#include <cstdlib>  // for getenv
#include <string>   // for std::string
#include <os/log.h> // for os_log

os_log_t logObject;

// Function automatically called when the library is loaded
__attribute__((constructor)) void initLog() {
    try {
        // Create a custom log object using the specified subsystem identifier
        logObject = os_log_create("br.com.redesurftank.UrlLoaderANE", "UrlLoaderANE");
        os_log(logObject, "Log initialized");

    } catch (const std::exception &e) {
        os_log_error(OS_LOG_DEFAULT, "Error initializing log: %s", e.what());
    }
}

void writeLog(const char *message) {
    // Print to console and log using macOS native logger
    fprintf(stdout, "%s\n", message);
    os_log(logObject, "%{public}s", message);
}

// Function automatically called when the library is unloaded
__attribute__((destructor)) void closeLog() {
    os_log(logObject, "Log closed");
}
