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
 * @author Push Technology Limited
 * @since 6.7
 */

/*
 * This example shows how roles can be changed during an active session.
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
 * Callback to display that the roles have been successfully changed.
 */
static int
on_roles_changed(void *context)
{
        printf("Successfully changed roles.\n");
        return HANDLER_SUCCESS;
}

/*
 * Callback to display an error when attempting to change roles.
 */
static int
on_error(SESSION_T *session, const DIFFUSION_ERROR_T *error)
{
        printf("Failed to change roles: [%d] %s\n", error->code, diffusion_error_str(error->code));
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
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        /*
         * Create a session with Diffusion.
         */
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "Failed to create session: %s\n", error.message);
                return EXIT_FAILURE;
        }

        SET_T *roles_to_add = set_new_string(1);
        set_add(roles_to_add, "AUTHENTICATION_HANDLER");
        
        DIFFUSION_CHANGE_ROLES_WITH_SESSION_ID_PARAMS_T params = {
                .session_id = session->id,
                .roles_to_remove = NULL,
                .roles_to_add = roles_to_add,
                .on_roles_changed = on_roles_changed,
                .on_error = on_error
        };

        DIFFUSION_API_ERROR api_error;
        diffusion_change_roles_with_session_id(session, params, &api_error);

        // Wait for a couple of seconds.
        sleep(2);

        puts("Closing session");

        // Close the connection.
        session_close(session, NULL);
        session_free(session);

        set_free(roles_to_add);
        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
