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
 * This examples shows how to make a synchronous connection to Diffusion.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#include <apr.h>
#include <apr_thread_mutex.h>
#include <apr_thread_cond.h>

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
 * This callback is used when the session state changes, e.g. when a session
 * moves from a "connecting" to a "connected" state, or from "connected" to
 * "closed".
 */
static void
on_session_state_changed(SESSION_T *session,
        const SESSION_STATE_T old_state,
        const SESSION_STATE_T new_state)
{
        printf("Session state changed from %s (%d) to %s (%d)\n",
               session_state_as_string(old_state), old_state,
               session_state_as_string(new_state), new_state);
}

/*
 * Entry point for the example.
 */
int
main(int argc, char **argv)
{
        // Standard command line parsing.
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return 1;
        }

        const char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        SESSION_T *session;
        DIFFUSION_ERROR_T error;

        SESSION_LISTENER_T session_listener;
        session_listener.on_state_changed = &on_session_state_changed;

        char *urls[] = {
                "dpt://localhost:8080",
                NULL
        };
        SESSION_FAILOVER_STRATEGY_T failover_strategy = {
                .retry_count = 5,
                .retry_delay = 2000L,
                .failover_urls = urls
        };

        session = session_create(url, principal, credentials, &session_listener, &failover_strategy, &error);
        if(session != NULL) {
                printf("Session created (state=%d, id=%s)\n", session->state, session_id_to_string(session->id));
        }
        else {
                printf("Failed to create session: %s\n", error.message);
                free(error.message);
        }

        // Pretend we're doing work.
        sleep(5);

        session_close(session, NULL);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
