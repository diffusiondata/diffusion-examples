/**
 * Copyright © 2014, 2021 Push Technology Ltd.
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
 * @since 5.0
 */

/*
 * This client shows how the principal (e.g. username) can be changed during
 * an active session.
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
 * Callback to display that the principal has been successfully
 * changed.
 */
static int
on_change_principal(SESSION_T *session, void *context)
{
        printf("Successfully changed the principal.\n");
        return HANDLER_SUCCESS;
}

/*
 * Callback to display an error when attempting to change the
 * principal.
 */
static int
on_change_principal_failure(SESSION_T *session, void *context)
{
        printf("Failed to change the principal\n");
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

        char *url = hash_get(options, "url");

        // Create a session with Diffusion, with no principal or credentials.
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, NULL, NULL, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "Failed to create session: %s\n", error.message);
                return EXIT_FAILURE;
        }

        // Wait for a couple of seconds.
        sleep(2);

        puts("Changing credentials");

        CREDENTIALS_T *credentials = credentials_create_password(hash_get(options, "credentials"));

        // Specify callbacks for the change_principal request.
        CHANGE_PRINCIPAL_PARAMS_T params = {
                .principal = hash_get(options, "principal"),
                .credentials = credentials,
                .on_change_principal = on_change_principal,
                .on_change_principal_failure = on_change_principal_failure
        };

        // Do the change.
        change_principal(session, params);

        // Wait for a couple more seconds.
        sleep(2);

        puts("Closing session");

        // Gracefully close the connection.
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
