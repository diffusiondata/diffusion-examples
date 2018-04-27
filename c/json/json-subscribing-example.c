/**
 * Copyright © 2017 Push Technology Ltd.
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
 * @since 6.0
 */

/*
 * The purpose of this example is to show how to subscribe to a JSON topic.
 */

#include <stdio.h>
#include <apr.h>

#ifdef WIN32
#define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "args.h"

// 5 seconds
#define SYNC_DEFAULT_TIMEOUT 5000 * 1000

apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "client"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'t', "topic", "Topic name to subscribe", ARG_OPTIONAL, ARG_HAS_VALUE, "time"},
        END_OF_ARG_OPTS
};

static int
on_subscription(const char *const topic_path,
                    const TOPIC_SPECIFICATION_T *const specification,
                    void *context)
{
        apr_thread_mutex_lock(mutex);
        printf("on_subscription, topic_path: %s\n", topic_path);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_unsubscription(const char *const topic_path,
                      const TOPIC_SPECIFICATION_T *const specification,
                      NOTIFY_UNSUBSCRIPTION_REASON_T reason,
                      void *context)
{
        apr_thread_mutex_lock(mutex);
        printf("Unsubscribed from topic: %s\n", topic_path);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_value(const char *const topic_path,
        const TOPIC_SPECIFICATION_T *const specification,
        DIFFUSION_DATATYPE datatype,
        const DIFFUSION_VALUE_T *const old_value,
        const DIFFUSION_VALUE_T *const new_value,
        void *context)
{

        apr_thread_mutex_lock(mutex);

        DIFFUSION_API_ERROR error;
        char *json_value;
        if(!to_diffusion_json_string(new_value, &json_value, &error)) {
                printf("Error: %s\n", get_diffusion_api_error_description(error));
                diffusion_api_error_free(error);
                return HANDLER_SUCCESS;
        }

        printf("Received message for topic %s (%ld bytes)\n", topic_path, strlen(json_value));
        printf("As JSON: %s\n", json_value);
        free(json_value);

        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

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
        const char *topic_name = hash_get(options, "topic");

        /*
         * Setup mutex and condition variable.
         */
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

        /*
         * Create a session with the Diffusion server.
         */
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "Failed to create session: %s\n", error.message);
                return EXIT_FAILURE;
        }

        // Set up and add the value stream to receive JSON updates
        VALUE_STREAM_T value_stream = {
                .datatype = DATATYPE_JSON,
                .on_subscription = on_subscription,
                .on_unsubscription = on_unsubscription,
                .on_value = on_value
        };

        // Add value stream
        add_stream(session, topic_name, &value_stream);

        SUBSCRIPTION_PARAMS_T params = {
                .topic_selector = topic_name,
                .on_topic_message = NULL
        };

        subscribe(session, params);

        // Receive updates for 2 minutes
        sleep(120);

        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        apr_thread_cond_destroy(cond);
        apr_thread_mutex_destroy(mutex);
        apr_pool_destroy(pool);
        apr_terminate();

        return EXIT_SUCCESS;
}
