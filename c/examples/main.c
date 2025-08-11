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

#ifndef WIN32
        #include <unistd.h>
#else
        #define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "args.h"


ARG_OPTS_T g_arg_options[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

char *g_executable_path;


void run_example(const char *url, const char *principal, CREDENTIALS_T *credentials);


int main(int argc, char **argv)
{
        HASH_T *options = parse_cmdline(argc, argv, g_arg_options);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, g_arg_options);
                return EXIT_FAILURE;
        }

        const char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        const char *password = hash_get(options, "credentials");

        g_executable_path = argv[0];

        CREDENTIALS_T *credentials = NULL;
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }
        run_example(url, principal, credentials);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
