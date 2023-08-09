/**
 * Copyright © 2021 Push Technology Ltd.
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
 * @since 6.7
 */

/*
 * This example shows how to close a session via a control session using
 * a session properties filter.
 */

#include <stdio.h>
#include <stdlib.h>
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
        END_OF_ARG_OPTS
};

/*
 * Callback to indicate that the affected sessions by the filter have been closed.
 */
static int on_clients_closed(int selected, void *context)
{
        printf("%d session(s) closed\n", selected);
         return HANDLER_SUCCESS;
}

/*
 * Callback to display an error when attempting to close a session.
 */
static int
on_error(SESSION_T *session, const DIFFUSION_ERROR_T *error)
{
        printf("Failed to close session(s): [%d] %s\n", error->code, diffusion_error_str(error->code));
        return HANDLER_SUCCESS;
}

int
main(int argc, char** argv)
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
        const char *principal = hash_get(options, "control");
        CREDENTIALS_T *control_credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                control_credentials = credentials_create_password(password);
        }

        /*
         * Create a control session with Diffusion.
         */
        DIFFUSION_SESSION_FACTORY_T *session_factory = diffusion_session_factory_init();
        diffusion_session_factory_principal(session_factory, principal);
        diffusion_session_factory_credentials(session_factory, control_credentials);
        
        SESSION_T *control_session = session_create_with_session_factory(session_factory, url);
        
        /*
         * Create a set of normal sessions with Diffusion, using `client` as Principal
         */
        CREDENTIALS_T *credentials = credentials_create_password("password");
        
        DIFFUSION_SESSION_FACTORY_T *client_session_factory = diffusion_session_factory_init();
        diffusion_session_factory_principal(client_session_factory, "client");
        diffusion_session_factory_credentials(client_session_factory, credentials);
        
        int totalSessions = 5;
        SESSION_T **sessions = calloc(totalSessions, sizeof(SESSION_T*));
        
        for (int i = 0; i < totalSessions; i++) {
            sessions[i] = session_create_with_session_factory(client_session_factory, url);
            if (sessions[i] == NULL) {
                fprintf(stderr, "Failed to create normal session\n");
                return EXIT_FAILURE;
            }
        }
        
        /*
         * Close normal sessions using control session and diffusion_client_close_with_filter
         */
         DIFFUSION_CLIENT_CLOSE_WITH_FILTER_PARAMS_T params = {
                 .filter = "$Principal EQ 'client'",
                 .on_clients_closed = on_clients_closed,
                 .on_error = on_error
         };

        DIFFUSION_API_ERROR api_error;
        diffusion_client_close_with_filter(control_session, params, &api_error);

        // Wait for a couple of seconds.
        sleep(2);

        puts("Closing sessions");

        // Close the connection.
        session_close(control_session, NULL);
        session_free(control_session);

        for(int i = 0; i < totalSessions; i++) {
            session_free(sessions[i]);
        }
        free(sessions);

        diffusion_session_factory_free(client_session_factory);
        diffusion_session_factory_free(session_factory);
        credentials_free(control_credentials);
        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
