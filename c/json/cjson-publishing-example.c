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
 *
 * It also shows:
 *  1. Use of a third-party JSON library (cJSON) to build the JSON structure.
 *  2. Reading JSON tokens and translating those to CBOR equivalent.
 *
 * The example reads a list of processes running on the system and uses it as
 * a source of data to be published into a JSON topic.
 *
 * cJSON can be downloaded from https://github.com/DaveGamble/cJSON/
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

#ifdef WIN32
#define sleep(x) Sleep(1000 * x)
#endif

#include <apr.h>

#include "diffusion.h"
#include "args.h"
#include "cJSON.h"

// 5 seconds
#define SYNC_DEFAULT_TIMEOUT 5000 * 1000
static void json_to_cbor(cJSON *item, CBOR_GENERATOR_T *cbor_generator);

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
 * Handler for add topic feature.
 */
static int
add_topic_callback(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
        if(response->reason != ADD_TOPIC_FAILURE_REASON_SUCCESS &&
           response->reason != ADD_TOPIC_FAILURE_REASON_EXISTS) {
                fprintf(stderr, "Failed to add topic\n");
                exit(EXIT_FAILURE);
        }
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

/*
 * Handler for processing updater registration callbacks.
 */
static int
register_updater_callback(SESSION_T *session,
                          const CONVERSATION_ID_T *updater_id,
                          const SVC_UPDATE_REGISTRATION_RESPONSE_T *response,
                          void *context)
{
        apr_thread_mutex_lock(mutex);

        switch(response->state) {
        case UPDATE_SOURCE_STATE_ACTIVE:
                g_active = 1;
                break;
        default:
                g_active = 0;
                break;
        }
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

/*
 * Handlers for update of data.
 */
static int
on_update_success(SESSION_T *session,
                  const CONVERSATION_ID_T *updater_id,
                  const SVC_UPDATE_RESPONSE_T *response,
                  void *context)
{
        char *id_str = conversation_id_to_string(*updater_id);
        printf("on_update_success for updater \"%s\"\n", id_str);
        free(id_str);
        return HANDLER_SUCCESS;
}

static int
on_update_failure(SESSION_T *session,
                  const CONVERSATION_ID_T *updater_id,
                  const SVC_UPDATE_RESPONSE_T *response,
                  void *context)
{
        char *id_str = conversation_id_to_string(*updater_id);
        printf("on_update_failure for updater \"%s\"\n", id_str);
        free(id_str);
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

        /*
         * Synchronously create a topic holding JSON content.
         */
        TOPIC_DETAILS_T *topic_details = create_topic_details_json();
        const ADD_TOPIC_PARAMS_T add_topic_params = {
                .topic_path = topic_name,
                .details = topic_details,
                .on_topic_added = add_topic_callback,
                .on_topic_add_failed = add_topic_callback
        };
        apr_thread_mutex_lock(mutex);
        add_topic(session, add_topic_params);
        apr_status_t rc = apr_thread_cond_timedwait(cond, mutex, SYNC_DEFAULT_TIMEOUT);
        apr_thread_mutex_unlock(mutex);
        topic_details_free(topic_details);
        if(rc != APR_SUCCESS) {
                fprintf(stderr, "Timed out while waiting for topic to be created\n");
                return EXIT_FAILURE;
        }

        /*
         * Register an updater for the topic.
         */
        const UPDATE_SOURCE_REGISTRATION_PARAMS_T update_reg_params = {
                .topic_path = topic_name,
                .on_active = register_updater_callback,
                .on_standby = register_updater_callback,
                .on_close = register_updater_callback
        };
        apr_thread_mutex_lock(mutex);
        g_updater_id = register_update_source(session, update_reg_params);
        rc = apr_thread_cond_timedwait(cond, mutex, SYNC_DEFAULT_TIMEOUT);
        apr_thread_mutex_unlock(mutex);
        if(rc != APR_SUCCESS) {
                fprintf(stderr, "Timed out while waiting to register an updater\n");
                return EXIT_FAILURE;
        }

        /*
         * Define default parameters for an update source.
         */
        UPDATE_VALUE_PARAMS_T update_value_params_base = {
                .updater_id = (CONVERSATION_ID_T *)g_updater_id,
                .topic_path = (char *)topic_name,
                .on_success = on_update_success,
                .on_failure = on_update_failure
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

                // Convert JSON to CBOR
                cJSON *json_object = cJSON_Parse(json);
                CBOR_GENERATOR_T *cbor_generator = cbor_generator_create();
                json_to_cbor(json_object, cbor_generator);

                // Extract the CBOR-encoded data and wrap it in a BUF_T structure.
                BUF_T *cbor_buf = buf_create();
                buf_write_bytes(cbor_buf, cbor_generator->data, cbor_generator->len);

                // Issue an update request to Diffusion. Under the covers,
                // this transmits a binary delta of changes, assuming those
                // changes are smaller than sending the entire value.
                UPDATE_VALUE_PARAMS_T update_value_params = update_value_params_base;
                update_value_params.data = cbor_buf;
                update_value(session, update_value_params);

                buf_free(cbor_buf);
                cbor_generator_free(cbor_generator);
                cJSON_Delete(json_object);

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

/*
 * This function takes a JSON token, and writes an equivalent CBOR token to
 * the supplied CBOR generator.
 */
static void
cbor_write_json_token(cJSON *item, CBOR_GENERATOR_T *cbor_generator)
{
        if(item->string) {
                // The item is a JSON key/value pair; write out the key.
                cbor_write_text_string(cbor_generator, item->string, strlen(item->string));
        }

        switch(item->type) {
        case cJSON_False:
                cbor_write_false(cbor_generator);
                break;
        case cJSON_True:
                cbor_write_true(cbor_generator);
                break;
        case cJSON_NULL:
                cbor_write_null(cbor_generator);
                break;
        case cJSON_Number:
                if(floor(item->valuedouble) == item->valuedouble) {
                        cbor_write_uint(cbor_generator, item->valueint);
                }
                else {
                        cbor_write_float(cbor_generator, item->valuedouble);
                }
                break;
        case cJSON_String:
                cbor_write_text_string(cbor_generator, item->valuestring, strlen(item->valuestring));
                break;
        case cJSON_Array:
                cbor_write_array(cbor_generator, cJSON_GetArraySize(item));
                break;
        case cJSON_Object:
                cbor_write_map(cbor_generator, cJSON_GetArraySize(item));
                break;
        default:
                printf("Unknown type\n");
                break;
        }
}

/*
 * Iterate/recurse through a JSON object, building up a stream of CBOR tokens
 * inside a CBOR generator.
 */
static void
json_to_cbor(cJSON *item, CBOR_GENERATOR_T *cbor_generator)
{
        while(item) {
                cbor_write_json_token(item, cbor_generator);
                if(item->child) {
                        json_to_cbor(item->child, cbor_generator);
                }
                item = item->next;
        }
}
