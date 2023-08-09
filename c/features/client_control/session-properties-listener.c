/**
 * Copyright © 2014, 2022 Push Technology Ltd.
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
 * @since 5.7
 */

/*
 * This example demonstrates how to register a listener that receives
 * notification of new client connections, clients closing and client
 * properties being updated.
 */

#include <stdio.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "apr.h"
#include "apr_thread_mutex.h"
#include "apr_thread_cond.h"

#include "diffusion.h"
#include "args.h"
#include "set.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

/*
 * Helper function to print the properties hash key/value pairs.
 */
static void
print_properties(HASH_T *properties)
{
        char **keys = hash_keys(properties);
        for(char **k = keys; *k != NULL; k++) {
                char *v = hash_get(properties, *k);
                printf("%s=%s\n", *k, v);
        }
        free(keys);
}

/*
 * Callback invoked when our listener registers successfully.
 */
static int
on_registered(SESSION_T *session, void *context)
{
        printf("on_registered\n");
        return HANDLER_SUCCESS;
}

/*
 * Callback invoked if our listener fails to register.
 */
static int
on_registration_error(SESSION_T *session, const DIFFUSION_ERROR_T *error)
{
        printf("on_registration_error: %s\n", error->message);
        return HANDLER_SUCCESS;
}

/*
 * Callback invoked when we receive notification of a new client
 * session.
 */
static int
on_session_open(SESSION_T *session, const SESSION_PROPERTIES_EVENT_T *request, void *context)
{
        char *sid_str = session_id_to_string(&request->session_id);
        printf("on_session_open: %s\n", sid_str);
        free(sid_str);
        print_properties(request->properties);
        return HANDLER_SUCCESS;
}

/*
 * Callback invoked when an existing client session undergoes a change
 * of properties.
 */
static int
on_session_update(SESSION_T *session, const SESSION_PROPERTIES_EVENT_T *request, void *context)
{
        printf("on_session_update\n");
        char *sid_str = session_id_to_string(&request->session_id);
        printf("on_session_close: %s\n", sid_str);
        free(sid_str);
        printf("update type: %d\n", request->update_type);
        return HANDLER_SUCCESS;
}

/*
 * Callback invoked when a client session closes.
 */
static int
on_session_close(SESSION_T *session, const SESSION_PROPERTIES_EVENT_T *request, void *context)
{
        char *sid_str = session_id_to_string(&request->session_id);
        printf("on_session_close: %s\n", sid_str);
        free(sid_str);
        printf("reason: %d\n", request->close_reason);
        print_properties(request->properties);
        return HANDLER_SUCCESS;
}

/*
 * Callback invoked if an error occurs while procssing a session
 * property event from the server.
 */
static int
on_session_error(SESSION_T *session, const DIFFUSION_ERROR_T *error)
{
        printf("on_session_error: %s\n", error->message);
        return HANDLER_SUCCESS;
}

/*
 * Program entry point.
 */
int
main(int argc, char **argv)
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

        /*
         * Create a session with Diffusion.
         */
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "Failed to create session: %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * Register a session properties listener.
         *
         * Requests all "fixed" properties, i.e. those defined by
         * Diffusion rather than user-defined properties.
         */
        SET_T *required_properties = set_new_string(5);
        set_add(required_properties, PROPERTIES_SELECTOR_ALL_FIXED_PROPERTIES);

        SESSION_PROPERTIES_REGISTRATION_PARAMS_T params = {
                .on_registered = on_registered,
                .on_registration_error = on_registration_error,
                .on_session_open = on_session_open,
                .on_session_close = on_session_close,
                .on_session_update = on_session_update,
                .on_session_error = on_session_error,
                .required_properties = required_properties
        };
        session_properties_listener_register(session, params);

        /*
         *  Wait for session events for 2 minutes.
         */
        sleep(120);

        /*
         * Close session and free resources.
         */
        session_close(session, NULL);
        session_free(session);

        set_free(required_properties);
        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
