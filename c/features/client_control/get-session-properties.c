/**
 * Copyright © 2020 - 2023 DiffusionData Ltd.
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
 * @since 6.6
 */

/*
 * In this example, we attempt to receive session properties for a client
 * with the specified client ID.
 *
 * In normal use, this could be used in conjunction with a session
 * properties listener that can track connecting client sessions and
 * their associated client IDs.
 */

#include <stdio.h>

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
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'i', "sessionid", "Session ID of the client. If not specified, get properties for this session.", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'r', "properties", "Comma separated list of properties to be requested.", ARG_OPTIONAL, ARG_HAS_VALUE, PROPERTIES_SELECTOR_ALL_FIXED_PROPERTIES},
        END_OF_ARG_OPTS
};

/*
 * Callback invoked when session properties are received.
 */
int on_session_properties(
        SESSION_T *session,
        const SVC_GET_SESSION_PROPERTIES_RESPONSE_T *response,
        void *context)
{
        char **keys = hash_keys(response->properties);
        for(char **k = keys; *k != NULL; k++) {
                char *v = hash_get(response->properties, *k);
                printf("%s=%s\n", *k, v);
        }
        free(keys);
        return HANDLER_SUCCESS;
}


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

        /*
         * Create a session with Diffusion.
         */
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "Failed to create session: %s\n", error.message);
                return EXIT_FAILURE;
        }

        SESSION_ID_T *sid;
        char *sid_str = hash_get(options, "sessionid");
        if(sid_str != NULL) {
                sid = session_id_create_from_string(sid_str);
        }
        else {
                sid = session->id;
        }

        SET_T *properties = set_new_string(10);
        char *props_str = strdup(hash_get(options, "properties"));
        char *str = props_str;
        char *tok = NULL;
        while((tok = strtok(str, ",")) != NULL) {
                str = NULL;
                set_add(properties, tok);
        }
        free(props_str);

        GET_SESSION_PROPERTIES_PARAMS_T params = {
                .session_id = sid,
                .required_properties = properties,
                .on_session_properties = on_session_properties
        };

        /*
         * Request the session properties, and wait for the response.
         */
        get_session_properties(session, params);

        // Sleep for a while
        sleep(5);

        /*
         * Close the session and clean up.
         */
        session_close(session, NULL);
        session_free(session);
        set_free(properties);
        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
