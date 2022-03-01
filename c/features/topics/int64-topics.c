/**
 * Copyright © 2018, 2021 Push Technology Ltd.
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

/**
 * This example shows how to add, subscribe and update an Int64 topic
 */

#include <stdio.h>
#include <stdlib.h>

#include "apr.h"
#include "apr_thread_mutex.h"
#include "apr_thread_cond.h"

#include "diffusion.h"
#include "args.h"
#include "utils.h"

static const long timeout = 5000;
static const long sleep_timeout = 1000 * 1000;

apr_pool_t *pool = NULL;

apr_thread_mutex_t *mutex_add_topic = NULL;
apr_thread_cond_t *cond_add_topic = NULL;

apr_thread_mutex_t *mutex_value_stream = NULL;
apr_thread_cond_t *cond_value_stream = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

/*
 * Handlers for add_topic_from_specification() function.
 */
static int
on_topic_added_with_specification(SESSION_T *session, TOPIC_ADD_RESULT_CODE result_code, void *context)
{
        apr_thread_mutex_lock(mutex_add_topic);
        apr_thread_cond_broadcast(cond_add_topic);
        apr_thread_mutex_unlock(mutex_add_topic);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_failed_with_specification(SESSION_T *session, TOPIC_ADD_FAIL_RESULT_CODE result_code, const DIFFUSION_ERROR_T *error, void *context)
{
        fprintf(stderr, "Failed to add topic: %s\n", error->message);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_discard(SESSION_T *session, void *context)
{
        fprintf(stderr, "Topic add discarded.");
        return HANDLER_SUCCESS;
}

static int
on_subscription(const char *const topic_path,
                    const TOPIC_SPECIFICATION_T *const specification,
                    void *context)
{
        printf("Subscribed to topic: %s\n", topic_path);
        apr_thread_mutex_lock(mutex_value_stream);
        apr_thread_cond_broadcast(cond_value_stream);
        apr_thread_mutex_unlock(mutex_value_stream);
        return HANDLER_SUCCESS;
}

static int
on_unsubscription(const char *const topic_path,
                      const TOPIC_SPECIFICATION_T *const specification,
                      NOTIFY_UNSUBSCRIPTION_REASON_T reason,
                      void *context)
{
        printf("Unsubscribed from topic: %s\n", topic_path);
        apr_thread_mutex_lock(mutex_value_stream);
        apr_thread_cond_broadcast(cond_value_stream);
        apr_thread_mutex_unlock(mutex_value_stream);
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
        if(old_value) {
                DIFFUSION_API_ERROR old_value_error;
                int64_t old_int64_value;
                if(!read_diffusion_int64_value(old_value, &old_int64_value, &old_value_error)) {
                        fprintf(stderr, "Error parsing int64 old value: %s\n", get_diffusion_api_error_description(old_value_error));
                        diffusion_api_error_free(old_value_error);
                        return HANDLER_SUCCESS;
                }

                printf("Old int64 value: " "%"PRId64"" "\n", old_int64_value);
        }

        DIFFUSION_API_ERROR new_value_error;
        int64_t new_int64_value;
        if(!read_diffusion_int64_value(new_value, &new_int64_value, &new_value_error)) {
                fprintf(stderr, "Error parsing int64 new value: %s\n", get_diffusion_api_error_description(new_value_error));
                diffusion_api_error_free(new_value_error);
                return HANDLER_SUCCESS;
        }

        printf("New int64 value: " "%"PRId64"" "\n\n", new_int64_value);

        apr_thread_mutex_lock(mutex_value_stream);
        apr_thread_cond_broadcast(cond_value_stream);
        apr_thread_mutex_unlock(mutex_value_stream);
        return HANDLER_SUCCESS;
}

static ADD_TOPIC_CALLBACK_T
create_topic_callback()
{
        ADD_TOPIC_CALLBACK_T callback = {
                .on_topic_added_with_specification = on_topic_added_with_specification,
                .on_topic_add_failed_with_specification = on_topic_add_failed_with_specification,
                .on_discard = on_topic_add_discard
        };

        return callback;
}

static int
on_topic_update(void *context)
{
        printf("topic update success\n");
        return HANDLER_SUCCESS;
}

static int
on_error(SESSION_T *session, const DIFFUSION_ERROR_T *error)
{
        printf("topic update error: %s\n", error->message);
        return HANDLER_SUCCESS;
}

static void
dispatch_int64_update(SESSION_T *session, const char *topic_path)
{
        int64_t value = rand();

        BUF_T *buf = buf_create();
        if(!write_diffusion_int64_value(value, buf)) {
                fprintf(stderr, "Unable to write the int64 update\n");
                buf_free(buf);
                return;
        }

        char *topic_path_dup = strdup(topic_path);

        DIFFUSION_TOPIC_UPDATE_SET_PARAMS_T topic_update_params = {
                .topic_path = topic_path_dup,
                .datatype = DATATYPE_INT64,
                .update = buf,
                .on_topic_update = on_topic_update,
                .on_error = on_error
        };

        apr_thread_mutex_lock(mutex_value_stream);
        diffusion_topic_update_set(session, topic_update_params);
        if(apr_thread_cond_timedwait(cond_value_stream, mutex_value_stream, timeout * 1000) != APR_SUCCESS) {
                fprintf(stderr, "Timed out while waiting for value stream on_value callback\n");
        }
        apr_thread_mutex_unlock(mutex_value_stream);

        buf_free(buf);
        free(topic_path_dup);
}

static void tear_down(SESSION_T *session, TOPIC_SPECIFICATION_T *specification)
{
        session_close(session, NULL);
        session_free(session);

        topic_specification_free(specification);

        apr_thread_mutex_destroy(mutex_add_topic);
        apr_thread_cond_destroy(cond_add_topic);

        apr_thread_mutex_destroy(mutex_value_stream);
        apr_thread_cond_destroy(cond_value_stream);

        apr_pool_destroy(pool);
        apr_terminate();
}

int main(int argc, char** argv)
{
        /*
         * Standard command-line parsing.
         */
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return EXIT_FAILURE;
        }

        const char *topic_path = "int64-example";

        const char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        // Setup for session
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        apr_initialize();
        apr_pool_create(&pool, NULL);

        apr_thread_mutex_create(&mutex_add_topic, APR_THREAD_MUTEX_DEFAULT, pool);
        apr_thread_cond_create(&cond_add_topic, pool);

        apr_thread_mutex_create(&mutex_value_stream, APR_THREAD_MUTEX_DEFAULT, pool);
        apr_thread_cond_create(&cond_value_stream, pool);

        // Add the int64 topic
        TOPIC_SPECIFICATION_T *specification = topic_specification_init(TOPIC_TYPE_INT64);
        ADD_TOPIC_CALLBACK_T add_topic_callback = create_topic_callback();

        apr_thread_mutex_lock(mutex_add_topic);
        add_topic_from_specification(session, topic_path, specification, add_topic_callback);
        if(apr_thread_cond_timedwait(cond_add_topic, mutex_add_topic, timeout * 1000) != APR_SUCCESS) {
                fprintf(stderr, "Failed to add topic\n");
                tear_down(session, specification);
                return EXIT_FAILURE;
        }
        apr_thread_mutex_unlock(mutex_add_topic);

        // Set up and add the value stream to receive int64 topic updates
        VALUE_STREAM_T value_stream = {
                .datatype = DATATYPE_INT64,
                .on_subscription = on_subscription,
                .on_unsubscription = on_unsubscription,
                .on_value = on_value
        };

        add_stream(session, topic_path, &value_stream);

        // Subscribe to the topic path
        SUBSCRIPTION_PARAMS_T params = {
                .topic_selector = topic_path,
                .on_topic_message = NULL
        };

        apr_thread_mutex_lock(mutex_value_stream);
        subscribe(session, params);
        if(apr_thread_cond_timedwait(cond_value_stream, mutex_value_stream, timeout * 1000) != APR_SUCCESS) {
                fprintf(stderr, "Failed to receive value stream on_subscription callback\n");
                tear_down(session, specification);
                return EXIT_FAILURE;
        }
        apr_thread_mutex_unlock(mutex_value_stream);

        // Dispatch 120 int64 topic updates at 1 second intervals.
        for(int i = 1; i <= 120; i++) {
                dispatch_int64_update(session, topic_path);
                apr_sleep(sleep_timeout);
        }

        // Close our session, and release resources and memory.
        tear_down(session, specification);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
