/**
 * Copyright © 2020, 2021 Push Technology Ltd.
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
 * @since 6.1
 */

/*
 * This example shows how to add a JSON value stream and subscribe to a selector.
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
        {'t', "topic", "Topic selector to subscribe to", ARG_REQUIRED, ARG_HAS_VALUE, "time"},
        END_OF_ARG_OPTS
};

static int on_subscription(const char* topic_path,
                    const TOPIC_SPECIFICATION_T *specification,
                    void *context)
{
        printf("Subscribed to topic: %s\n", topic_path);
        return HANDLER_SUCCESS;
}

static int on_unsubscription(const char* topic_path,
                      const TOPIC_SPECIFICATION_T *specification,
                      NOTIFY_UNSUBSCRIPTION_REASON_T reason,
                      void *context)
{
        printf("Unsubscribed from topic: %s\n", topic_path);
        return HANDLER_SUCCESS;
}

static int on_value(const char* topic_path,
             const TOPIC_SPECIFICATION_T *const specification,
             const DIFFUSION_DATATYPE datatype,
             const DIFFUSION_VALUE_T *const old_value,
             const DIFFUSION_VALUE_T *const new_value,
             void *context)
{
        DIFFUSION_API_ERROR api_error;
        char *result;
        bool success = to_diffusion_json_string(new_value, &result, &api_error);

        if(success) {
                printf("Received value: %s\n", result);
                free(result);
                return HANDLER_SUCCESS;
        }

        printf("Error during diffusion value read: %s\n",
                get_diffusion_api_error_description(api_error));
        diffusion_api_error_free(api_error);
        return HANDLER_SUCCESS;
}

static void on_close() 
{
        printf("Value stream closed\n");
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
        const char *selector = hash_get(options, "topic");

        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };

        /*
         * Create a session, synchronously.
         */
        session = session_create(url, principal, credentials, NULL, NULL, &error);
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
                credentials_free(credentials);
                return EXIT_FAILURE;
        }

        /*
         * Create a value stream
         */
        VALUE_STREAM_T value_stream = {
                .datatype = DATATYPE_JSON,
                .on_subscription = on_subscription,
                .on_unsubscription = on_unsubscription,
                .on_value = on_value,
                .on_close = on_close
        };

        add_stream(session, selector, &value_stream);
        SUBSCRIPTION_PARAMS_T params = {
                .topic_selector = selector
        };

        /*
         * Subscribe to topics matching the selector
         */
        subscribe(session, params);

        UNSUBSCRIPTION_PARAMS_T unsub_params = {
                .topic_selector = selector
        };

        /*
         * Unsubscribe to topics matching the selector
         */
        unsubscribe(session, unsub_params);

        /*
         * Sleep for 2 seconds.
         */
        sleep(2);

        /*
         * Close the session, and release resources and memory.
         */
        session_close(session, NULL);
        session_free(session);
        hash_free(options, NULL, free);

        credentials_free(credentials);

        return EXIT_SUCCESS;
}
