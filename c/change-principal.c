/**
 * Copyright © 2014, 2015 Push Technology Ltd.
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
 * @author Push Technology Limited
 * @since 5.0
 */

/*
 * This client shows how the principal (e.g. username) can be changed during
 * an active session.
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "diffusion.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "dpt://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        END_OF_ARG_OPTS
};

/*
 * Callback to display that the change_principal() request has been processed
 * by Diffusion.
 */
static int
on_change_principal(SESSION_T *session, void *context)
{
        printf("on_change_principal\n");
        return HANDLER_SUCCESS;
}

int
main(int argc, char** argv)
{
        // Standard command line parsing.
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return 1;
        }

        char *url = hash_get(options, "url");

        // Create a session with Diffusion, with no principal or credentials.
        SESSION_T *session;
        DIFFUSION_ERROR_T error;
        session = session_create(url, NULL, NULL, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "Failed to create session: %s\n", error.message);
                return 1;
        }

        // Wait for a couple of seconds.
        sleep(2);

        puts("Changing credentials");

        CREDENTIALS_T *credentials = credentials_create_password("chips");

        // Specify callbacks for the change_principal request.
        CHANGE_PRINCIPAL_PARAMS_T params = {
                .principal = "fish",
                .credentials = credentials,
                .on_change_principal = on_change_principal
        };

        // Do the change.
        change_principal(session, params);

        // Wait for a couple more seconds.
        sleep(2);

        puts("Closing session");

        // Gracefully close the connection.
        session_close(session, &error);

        return 0;
}
