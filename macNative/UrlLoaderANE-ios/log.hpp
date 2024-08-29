//
//  log.hpp
//  WebSocketANE
//
//  Created by João Vitor Borges on 23/08/24.
//

#ifndef log_hpp
#define log_hpp

#include <stdio.h>

__attribute__((constructor)) void initLog();
void writeLog(const char *message);
void closeLog();

#endif /* log_hpp */
