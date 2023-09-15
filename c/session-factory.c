/**
 * Copyright © 2022 Push Technology Ltd.
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
 *
 * @author DiffusionData Limited
 * @since 6.9
 */

/*
 * This examples shows how to connect to Diffusion via a session factory.
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


ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "client"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'a', "attempts", "Total attempts for initial session establishment", ARG_OPTIONAL, ARG_HAS_VALUE, "10"},
        {'i', "interval", "Interval in milliseconds between attempts for initial session establishment", ARG_OPTIONAL, ARG_HAS_VALUE, "1000"},
        {'s', "sleep", "Time to sleep before disconnecting (in seconds).", ARG_OPTIONAL, ARG_HAS_VALUE, "5" },
        END_OF_ARG_OPTS
};


/*
 * Entry point for the example.
 */
int main(int argc, char **argv)
{
        /*
         * Standard command-line parsing.
         */
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return EXIT_FAILURE;
        }

        const char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        uint32_t attempts = atol(hash_get(options, "attempts"));
        uint32_t interval = atol(hash_get(options, "interval"));

        const unsigned int sleep_time = atol(hash_get(options, "sleep"));

        DIFFUSION_SESSION_FACTORY_T *session_factory = diffusion_session_factory_init();
        diffusion_session_factory_principal(session_factory, principal);
        diffusion_session_factory_credentials(session_factory, credentials);

        DIFFUSION_RETRY_STRATEGY_T *retry_strategy = diffusion_retry_strategy_create(interval, attempts, NULL);
        diffusion_session_factory_initial_retry_strategy(session_factory, retry_strategy);

        /*
         * Create a session, synchronously.
         */
        SESSION_T *session = session_create_with_session_factory(session_factory, url);
        if(session != NULL) {
                char *sid_str = session_id_to_string(session->id);
                printf("Session created (state=%d, id=%s)\n",
                       session_state_get(session),
                       sid_str);
                free(sid_str);
        }
        else {
                printf("Failed to create session\n");
        }

        /*
         * Sleep for a while.
         */
        sleep(sleep_time);

        /*
         * Close the session, and release resources and memory.
         */
        session_close(session, NULL);
        session_free(session);

        diffusion_retry_strategy_free(retry_strategy);
        diffusion_session_factory_free(session_factory);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
