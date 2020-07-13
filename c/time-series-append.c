/**
 * Copyright © 2019 Push Technology Ltd.
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
 * @since 6.3
 */

/*
 * This example creates a Time series topic (of String datatype) and periodically appends
 * data to it.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
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
#include "conversation.h"

apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "time-series-append"},
        {'s', "seconds", "Number of seconds to run for before exiting", ARG_OPTIONAL, ARG_HAS_VALUE, "30"},
        END_OF_ARG_OPTS
};

/*
 * Handlers for add topic feature.
 */
static int
on_topic_added_with_specification(SESSION_T *session, TOPIC_ADD_RESULT_CODE result_code, void *context)
{
        printf("Added topic \"%s\"\n", (const char *)context);
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_failed_with_specification(SESSION_T *session, TOPIC_ADD_FAIL_RESULT_CODE result_code, const DIFFUSION_ERROR_T *error, void *context)
{
        printf("Failed to add topic \"%s\" (%d)\n", (const char *)context, result_code);
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_discard(SESSION_T *session, void *context)
{
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static ADD_TOPIC_CALLBACK_T
create_topic_callback(const char *topic_name)
{
        ADD_TOPIC_CALLBACK_T callback = {
                .on_topic_added_with_specification = on_topic_added_with_specification,
                .on_topic_add_failed_with_specification = on_topic_add_failed_with_specification,
                .on_discard = on_topic_add_discard,
                .context = (char *)topic_name
        };

        return callback;
}

static int on_append(const DIFFUSION_TIME_SERIES_EVENT_METADATA_T *event_metadata, void *context)
{
        printf("time series append success\n");
        return HANDLER_SUCCESS;
}

static int
on_error(SESSION_T *session, const DIFFUSION_ERROR_T *error)
{
        printf("time series append error: %s\n", error->message);
        return HANDLER_SUCCESS;
}

/*
 * Program entry point.
 */
int
main(int argc, char** argv)
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
        const long seconds = atol(hash_get(options, "seconds"));

        /*
         * Setup for condition variable.
         */
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

        /*
         * Create a session with the Diffusion server.
         */
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        ADD_TOPIC_CALLBACK_T callback = create_topic_callback(topic_name);

        HASH_T *properties = hash_new(2);
        hash_add(properties, DIFFUSION_TIME_SERIES_EVENT_VALUE_TYPE, "string");

        TOPIC_SPECIFICATION_T *spec = topic_specification_init(TOPIC_TYPE_TIME_SERIES);
        topic_specification_set_properties(spec, properties);

        apr_thread_mutex_lock(mutex);
        add_topic_from_specification(session, topic_name, spec, callback);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        topic_specification_free(spec);
        hash_free(properties, NULL, NULL);

        time_t end_time = time(NULL) + seconds;

        while(time(NULL) < end_time) {

                /*
                 * Compose the update content
                 */
                const time_t time_now = time(NULL);
                const char *time_str = ctime(&time_now);

                /*
                 * Create a BUF_T and write the string datatype value
                 * into it. 
                 */
                BUF_T *value = buf_create();
                write_diffusion_string_value(time_str, value);

                DIFFUSION_TIME_SERIES_APPEND_PARAMS_T params = {
                        .on_append = on_append,
                        .on_error = on_error,
                        .topic_path = topic_name,
                        .datatype = DATATYPE_STRING,
                        .value = value
                };

                /*
                 * Append to the time series topic
                 */
                diffusion_time_series_append(session, params, NULL);
                buf_free(value);

                sleep(1);
        }

        /*
         * Close session and free resources.
         */
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        apr_thread_mutex_destroy(mutex);
        apr_thread_cond_destroy(cond);
        apr_pool_destroy(pool);
        apr_terminate();

        return EXIT_SUCCESS;
}
