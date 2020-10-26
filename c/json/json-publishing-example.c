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
 * The purpose of this example is to show how to update a JSON topic.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

#ifdef WIN32
#define sleep(x) Sleep(1000 * x)
#else
#include <unistd.h>
#endif

#include <apr.h>

#include "diffusion.h"
#include "args.h"

// 5 seconds
#define SYNC_DEFAULT_TIMEOUT 5000 * 1000

int volatile g_active = 0;
CONVERSATION_ID_T volatile *g_updater_id;

apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control" },
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password" },
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "time" },
        END_OF_ARG_OPTS
};

/*
 * Handlers for update of data.
 */
static int
on_update_success(void *context)
{
        printf("on_update_success\n");
        return HANDLER_SUCCESS;
}

static int
on_update_failure(SESSION_T *session,
                  const DIFFUSION_ERROR_T *error)
{
        printf("on_update_failure\n");
        return HANDLER_SUCCESS;
}

static int
on_topic_added(SESSION_T *session, TOPIC_ADD_RESULT_CODE result_code, void *context)
{
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_failed(SESSION_T *session, TOPIC_ADD_FAIL_RESULT_CODE result_code, const DIFFUSION_ERROR_T *error, void *context)
{
        printf("on_topic_add_failed, code: %d\n", result_code);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_discard(SESSION_T *session, void *context)
{
        printf("on_topic_add_discard\n");
        return HANDLER_SUCCESS;
}

static ADD_TOPIC_CALLBACK_T
create_topic_callback()
{
        ADD_TOPIC_CALLBACK_T callback = {
                .on_topic_added_with_specification = on_topic_added,
                .on_topic_add_failed_with_specification = on_topic_add_failed,
                .on_discard = on_topic_add_discard
        };

        return callback;
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

        ADD_TOPIC_CALLBACK_T callback = create_topic_callback();
        TOPIC_SPECIFICATION_T *specification = topic_specification_init(TOPIC_TYPE_JSON);

        /*
         * Synchronously create a topic holding JSON content.
         */
        apr_thread_mutex_lock(mutex);
        add_topic_from_specification(session, topic_name, specification, callback);
        apr_status_t rc = apr_thread_cond_timedwait(cond, mutex, SYNC_DEFAULT_TIMEOUT);
        apr_thread_mutex_unlock(mutex);
        topic_specification_free(specification);
        if(rc != APR_SUCCESS) {
                fprintf(stderr, "Timed out while waiting for topic to be created\n");
                return EXIT_FAILURE;
        }

        /*
         * Define default parameters for an update source.
         */
         DIFFUSION_TOPIC_UPDATE_SET_PARAMS_T update_value_params_base = {
                .topic_path = (char *)topic_name,
                .datatype = DATATYPE_JSON,
                .on_topic_update = on_update_success,
                .on_error = on_update_failure
         };

        time_t current_time;
        struct tm *time_info;
        char json[255];
        char format_string[] = "{\"day\":\"%d\",\"month\":\"%m\",\"year\":\"%Y\",\"hour\":\"%H\",\"minute\":\"%M\",\"second\":\"%S\"}";

        /*
         * Forever, until deactivated.
         */
        while(g_active) {
                // Get current time
                current_time = time(NULL);
                if(current_time == ((time_t)-1)) {
                        fprintf(stderr, "Failure to obtain the current time\n");
                        return EXIT_FAILURE;
                }

                // Get UTC time info
                time_info = gmtime( &current_time );
                if(time_info == NULL) {
                        fprintf(stderr, "Failure to obtain UTC time info\n");
                        return EXIT_FAILURE;
                }

                // Construct JSON string based on current time
                if(strftime(json, sizeof(json), format_string, time_info) == 0) {
                        fprintf(stderr, "Failure to construct JSON value\n");
                        return EXIT_FAILURE;
                }

                printf("Updated value: %s\n", json);

                // Extract the CBOR-encoded data and wrap it in a BUF_T structure.
                BUF_T *buf = buf_create();
                write_diffusion_json_value(json, buf);

                // Issue an update request to Diffusion. Under the covers,
                // this transmits a binary delta of changes, assuming those
                // changes are smaller than sending the entire value.
                DIFFUSION_TOPIC_UPDATE_SET_PARAMS_T update_value_params = update_value_params_base;
                update_value_params.update = buf;

                diffusion_topic_update_set(session, update_value_params);
                buf_free(buf);

                // Sleep for a second
                sleep(1);
        }

        puts("Updater not active, exiting.");

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
