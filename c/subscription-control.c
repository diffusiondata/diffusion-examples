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
 * @since 5.7
 */

/*
 * This example waits to be notified of a client connection, and then
 * subscribes that client to a named topic.
 */

#include <stdio.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include <apr.h>
#include <apr_thread_mutex.h>
#include <apr_thread_cond.h>

#include "diffusion.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'t', "topic_selector", "Topic selector to subscribe/unsubscribe clients from", ARG_OPTIONAL, ARG_HAS_VALUE, ">foo"},
        END_OF_ARG_OPTS
};
HASH_T *options = NULL;

/*
 * Callback invoked when a client has been successfully subscribed to
 * a topic.
 */
static int
on_subscription_complete(SESSION_T *session, void *context)
{
        printf("Subscription complete\n");
        return HANDLER_SUCCESS;
}

/*
 * Callback invoked when a client session has been opened.
 */
static int
on_session_open(SESSION_T *session, const SESSION_PROPERTIES_EVENT_T *request, void *context)
{
        if(session_id_cmp(*session->id, request->session_id) == 0) {
                // It's our own session, ignore.
                return HANDLER_SUCCESS;
        }

        char *topic_selector = hash_get(options, "topic_selector");

        char *sid_str = session_id_to_string(&request->session_id);
        printf("Subscribing session %s to topic selector %s\n", sid_str, topic_selector);
        free(sid_str);

        /*
         * Subscribe the client session to the topic.
         */
        SUBSCRIPTION_CONTROL_PARAMS_T subscribe_params = {
                .session_id = request->session_id,
                .topic_selector = topic_selector,
                .on_complete = on_subscription_complete
        };
        subscribe_client(session, subscribe_params);

        return HANDLER_SUCCESS;
}

int
main(int argc, char **argv)
{
        /*
         * Standard command-line parsing.
         */
        options = parse_cmdline(argc, argv, arg_opts);
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
         * Register a session properties listener, so we are notified
         * of new client connections.
         * In the callback, we will subscribe the client to topics
         * according to the topic_selector argument.
         */
        SET_T *required_properties = set_new_string(1);
        set_add(required_properties, PROPERTIES_SELECTOR_ALL_FIXED_PROPERTIES);
        SESSION_PROPERTIES_REGISTRATION_PARAMS_T params = {
                .on_session_open = on_session_open,
                .required_properties = required_properties
        };
        session_properties_listener_register(session, params);
        set_free(required_properties);

        /*
         * Pretend to do some work.
         */
        sleep(10);

        /*
         * Close session and tidy up.
         */
        session_close(session, NULL);
        session_free(session);

        return EXIT_SUCCESS;
}
