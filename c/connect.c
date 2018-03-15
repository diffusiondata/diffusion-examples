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
 * This examples shows how to make a synchronous connection to Diffusion.
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
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'d', "delay", "Delay between reconnection attempts, in ms", ARG_OPTIONAL, ARG_HAS_VALUE, "2000" },
        {'r', "retries", "Reconnection retry attempts", ARG_OPTIONAL, ARG_HAS_VALUE, "5" },
        {'t', "timeout", "Reconnection timeout for a disconnected session", ARG_OPTIONAL, ARG_HAS_VALUE, NULL },
        {'x', "cascade_urls", "Comma-separated list of URLs to use for cascading", ARG_OPTIONAL, ARG_HAS_VALUE, NULL },
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

        long retry_delay = atol(hash_get(options, "delay"));
        long retry_count = atol(hash_get(options, "retries"));
        long reconnect_timeout;
        if(hash_get(options, "timeout") != NULL) {
                reconnect_timeout = atol(hash_get(options, "timeout"));
        }
        else {
                reconnect_timeout = -1;
        }

        char *urls = hash_get(options, "cascade_urls");

        /*
         * Convert a comma-separated list of URLs into a
         * NULL-terminated array giving URLs to try (in order) until
         * we successfully connect, or they've all been tried
         * unsuccessfully. These are in addition to the initial URL.
         */
        char **url_array = NULL;
        if(urls != NULL) {
                char *urls_copy = strdup(urls);
                char *start = urls_copy;
                char *end = urls_copy;
                int url_count = 0;
                while(1) {
                        if(*end == ',' || *end == '\0') {
                                url_array = realloc(url_array, (url_count + 1) * sizeof(char *));
                                url_array[url_count] = start;
                                url_count++;
                                start = end+1;

                                if(*end == '\0') {
                                        break;
                                }
                                if(*end == ',') {
                                        *end = '\0';
                                }
                        }
                        end++;
                }
                url_array = realloc(url_array, (url_count + 1) * sizeof(char *));
                url_array[url_count] = NULL;
        }

        const unsigned int sleep_time = atol(hash_get(options, "sleep"));

        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };

        SESSION_LISTENER_T session_listener = { 0 };
        session_listener.on_state_changed = &on_session_state_changed;

        /*
         * Specify how we might want to cascade/failover or retry, and
         * how long to keep the session alive on the server before
         * it's discarded.
         */
        RECONNECTION_STRATEGY_T *reconnection_strategy =
                make_reconnection_strategy_repeating_attempt(retry_count, retry_delay);

        if(reconnect_timeout > 0) {
                reconnection_strategy_set_timeout(reconnection_strategy, reconnect_timeout);
        }

        reconnection_strategy_set_cascade_urls(reconnection_strategy, (const char **)url_array);

        /*
         * Create a session, synchronously.
         */
        session = session_create(url, principal, credentials, &session_listener, reconnection_strategy, &error);
        if(session != NULL) {
                char *sid_str = session_id_to_string(session->id);
                printf("Session created (state=%d, id=%s)\n",
                       session_state_get(session),
                       sid_str);
                free(sid_str);
        }
        else {
                printf("Failed to create session: %s\n", error.message);
                free(error.message);
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

        return EXIT_SUCCESS;
}
