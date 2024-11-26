/**
 * Copyright © 2024 DiffusionData Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This example is written in C99. Please use an appropriate C99 capable compiler
 */

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "diffusion.h"
#include "utils.h"

#ifndef WIN32
    #include <libgen.h>

    char *os_dirname(char *path) {
        char *folder_path = dirname(path);
        // to maintain simetry with the Windows function, we allocate the memory
        char *result = calloc(strlen(folder_path) + 1, sizeof(char));
        memmove(result, folder_path, strlen(folder_path));
        return result;
    }

#else
    char *os_dirname(char *path) {
        size_t len = 500;
        char *result = calloc(len, sizeof(char));
        _splitpath_s(
            path, NULL, 0, result, len, NULL, 0, NULL, 0
        );
        char last_char = result[strlen(result) - 1];
        if (last_char == '\\') {
            char *trimmed_result = calloc(strlen(result), sizeof(char));
            memmove(trimmed_result, result, strlen(result) - 1);
            free(result);
            return trimmed_result;
        }
        return result;
    }
#endif
MUTEX_DEF

void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    extern char *g_executable_path;

    char *executable_folder_path = os_dirname(g_executable_path);
    char *target_folder_path = utils_path_to_folder(executable_folder_path, "target");

    char *certificate_relative_path = calloc(strlen(target_folder_path) + 40, sizeof(char));
    sprintf(certificate_relative_path, "%s/resources/DiffusionData_CA.crt", target_folder_path);

    // Define system environment variable indicating to accept a specific certificate
    OS_SETENV("DIFFUSION_TRUST_SELF_SIGNED_CERTS", "true");
    OS_SETENV("DIFFUSION_SSL_CA_FILE_PATH", certificate_relative_path);

    free(certificate_relative_path);
    free(target_folder_path);

    DIFFUSION_ERROR_T error = { 0 };
    SESSION_T *session = session_create(
        "wss://localhost:8080", principal, credentials, NULL, NULL, &error
    );
    if(session != NULL) {
        char *sid_str = session_id_to_string(session->id);
        printf("Connected Securely. Session Identifier: %s\n", sid_str);
        free(sid_str);
    }
    else {
        printf("Failed to create session: %s\n", error.message);
        free(error.message);
    }

    // Insert work here

    session_close(session, NULL);
    session_free(session);
}