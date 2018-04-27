/**
 * Copyright © 2014, 2016 Push Technology Ltd.
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
 * @since 5.0
 */

/*
 * This example shows how to make a synchronous connection to
 * Diffusion, with user-provided reconnection logic.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include <apr_time.h>

#include "diffusion.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'s', "sleep", "Time to sleep before disconnecting (in seconds).", ARG_OPTIONAL, ARG_HAS_VALUE, "5" },
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

typedef struct {
        long current_wait;
        long max_wait;
} BACKOFF_STRATEGY_ARGS_T;

static RECONNECTION_ATTEMPT_ACTION_T
backoff_reconnection_strategy(SESSION_T *session, void *args)
{
        BACKOFF_STRATEGY_ARGS_T *backoff_args = args;

        printf("Waiting for %ld ms\n", backoff_args->current_wait);

        apr_sleep(backoff_args->current_wait * 1000); // µs -> ms

        // But only up to some maximum time.
        if(backoff_args->current_wait > backoff_args->max_wait) {
                backoff_args->current_wait = backoff_args->max_wait;
        }

        return RECONNECTION_ATTEMPT_ACTION_START;
}

static void
backoff_success(SESSION_T *session, void *args)
{
        printf("Reconnection successful\n");

        BACKOFF_STRATEGY_ARGS_T *backoff_args = args;
        backoff_args->current_wait = 0; // Reset wait.
}

static void
backoff_failure(SESSION_T *session, void *args)
{
        printf("Reconnection failed (%s)\n", session_state_as_string(session->state));

        BACKOFF_STRATEGY_ARGS_T *backoff_args = args;

        // Exponential backoff.
        if(backoff_args->current_wait == 0) {
                backoff_args->current_wait = 1;
        }
        else {
                backoff_args->current_wait *= 2;
        }
}

/*
 * Entry point for the example.
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

        const unsigned int sleep_time = atol(hash_get(options, "sleep"));

        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };

        SESSION_LISTENER_T session_listener = { 0 };
        session_listener.on_state_changed = &on_session_state_changed;

        /*
         * Set the arguments to our exponential backoff strategy.
         */
        BACKOFF_STRATEGY_ARGS_T *backoff_args = calloc(1, sizeof(BACKOFF_STRATEGY_ARGS_T));
        backoff_args->current_wait = 0;
        backoff_args->max_wait = 5000;

        /*
         * Create the backoff strategy.
         */
        RECONNECTION_STRATEGY_T *reconnection_strategy =
                make_reconnection_strategy_user_function(backoff_reconnection_strategy,
                                                         backoff_args,
                                                         backoff_success,
                                                         backoff_failure);

        /*
         * Only ever retry for 30 seconds.
         */
        reconnection_strategy_set_timeout(reconnection_strategy, 30 * 1000);

        /*
         * Create a session, synchronously.
         */
        session = session_create(url, principal, credentials, &session_listener, reconnection_strategy, &error);
        if(session != NULL) {
                char *sid_str = session_id_to_string(session->id);
                printf("Session created (state=%d, id=%s)\n", session_state_get(session), sid_str);
                free(sid_str);
        }
        else {
                printf("Failed to create session: %s\n", error.message);
                free(error.message);
        }

        // With the exception of backoff_args, the reconnection strategy is
        // copied withing session_create() and may be freed now.
        free(reconnection_strategy);

        /*
         * Sleep for a while.
         */
        sleep(sleep_time);

        /*
         * Close the session, and release resources and memory.
         */
        session_close(session, NULL);
        session_free(session);

        free(backoff_args);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
