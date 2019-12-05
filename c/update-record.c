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
 * This example creates simple RecordTopicData with a single Record
 * containing two Fields, and updates it every second.
 *
 * When running this example, it's possible to choose whether
 * subscribing clients see a entire contents of the topic with every
 * update, or just the fields that have changed (ie, a delta).
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

int active = 0;

apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "foo"},
        {'s', "seconds", "Number of seconds to run for before exiting", ARG_OPTIONAL, ARG_HAS_VALUE, "30"},
        END_OF_ARG_OPTS
};

static const char *EMPTY_FIELD_MARKER = "-EMPTY-";

/*
 * Handlers for adding topics.
 */
static int
on_topic_added(SESSION_T *session, TOPIC_ADD_RESULT_CODE result_code, void *context)
{
        printf("Added topic\n");
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_failed(SESSION_T *session, TOPIC_ADD_FAIL_RESULT_CODE result_code, const DIFFUSION_ERROR_T *error, void *context)
{
        printf("Failed to add topic (%d)\n", result_code);
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
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

int
main(int argc, char** argv)
{
        /*
         * Standard command-line parsing.
         */
        const HASH_T *options = parse_cmdline(argc, argv, arg_opts);
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
         * Setup for condition variable
         */
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

        /*
         * Connect to the Diffusion server.
         */
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * Add a topic with a simple record topic data structure,
         * containing two fields.
         */
        DIFFUSION_RECORDV2_SCHEMA_BUILDER_T *schema_builder = diffusion_recordv2_schema_builder_init();
        diffusion_recordv2_schema_builder_record(schema_builder, "SimpleRecord", NULL);
        diffusion_recordv2_schema_builder_string(schema_builder, "first", NULL);
        diffusion_recordv2_schema_builder_string(schema_builder, "second", NULL);

        DIFFUSION_RECORDV2_SCHEMA_T *schema = diffusion_recordv2_schema_builder_build(schema_builder, NULL);
        char *schema_as_string = diffusion_recordv2_schema_as_json_string(schema);

        HASH_T *properties = hash_new(2);
        hash_add(properties, DIFFUSION_VALIDATE_VALUES, "true");
        hash_add(properties, DIFFUSION_SCHEMA, schema_as_string);

        TOPIC_SPECIFICATION_T *recordv2_specification = topic_specification_init_with_properties(TOPIC_TYPE_RECORDV2, properties);

        ADD_TOPIC_CALLBACK_T callback = {
                .on_topic_added_with_specification = on_topic_added,
                .on_topic_add_failed_with_specification = on_topic_add_failed,
                .on_discard = NULL,
                .context = (void *)topic_name
        };

        apr_thread_mutex_lock(mutex);
        add_topic_from_specification(session, topic_name, recordv2_specification, callback);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        diffusion_recordv2_schema_builder_free(schema_builder);
        diffusion_recordv2_schema_free(schema);
        free(schema_as_string);

        topic_specification_free(recordv2_specification);
        hash_free(properties, NULL, NULL);

        /*
         * Alternately update one field or both every second.
         */
        int count1 = 0;
        int count2 = 0;

        DIFFUSION_RECORDV2_BUILDER_T *value_builder = diffusion_recordv2_builder_init();
        char **fields = calloc(3, sizeof(char *));

        time_t end_time = time(NULL) + seconds;
        while(time(NULL) < end_time) {

                BUF_T *buf = buf_create();

                if(count1 % 2 == 0) {
                        count2++;
                }
                count1++;

                buf = buf_create();
                if(count1 == 5 || count1 == 6) {
                        fields[0] = (char *)EMPTY_FIELD_MARKER;
                        fields[1] = (char *)EMPTY_FIELD_MARKER;
                }
                else {
                        char count1_string[20];
                        char count2_string[20];
                        snprintf(count1_string, 20, "%d", count1);
                        snprintf(count2_string, 20, "%d", count2);
                        fields[0] = count1_string;
                        fields[1] = count2_string;
                }

                diffusion_recordv2_builder_add_record(value_builder, fields);
                void *record_bytes = diffusion_recordv2_builder_build(value_builder);

                if(!write_diffusion_recordv2_value(record_bytes, buf)) {
                        fprintf(stderr, "Unable to write the recordv2 update\n");
                        diffusion_recordv2_builder_free(value_builder);
                        free(fields);
                        buf_free(buf);
                        return EXIT_FAILURE;
                }

                DIFFUSION_TOPIC_UPDATE_SET_PARAMS_T topic_update_params = {
                        .topic_path = topic_name,
                        .datatype = DATATYPE_RECORDV2,
                        .update = buf,
                        .on_topic_update = on_topic_update,
                        .on_error = on_error
                };

                /*
                 * Update the topic.
                 */
                diffusion_topic_update_set(session, topic_update_params);

                diffusion_recordv2_builder_clear(value_builder);
                free(record_bytes);
                buf_free(buf);

                sleep(1);
        }

        diffusion_recordv2_builder_free(value_builder);
        free(fields);

        /*
         * Close session and tidy up.
         */
        session_close(session, NULL);
        session_free(session);

        apr_thread_mutex_destroy(mutex);
        apr_thread_cond_destroy(cond);
        apr_pool_destroy(pool);
        apr_terminate();

        puts("Done.");
        return EXIT_SUCCESS;
}
