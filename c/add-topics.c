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
 * This example shows how to connect to Diffusion as a control client and
 * create various topics on the server.
 */

#include <stdio.h>
#include <stdlib.h>
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
#include "utils.h"

/*
 * We use a mutex and a condition variable to help synchronize the
 * flow so that it becomes linear and easier to follow the core logic.
 */
apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        END_OF_ARG_OPTS
};

// Various handlers which are common to all add_topic() functions.
static int
on_topic_added(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
        puts("on_topic_added");
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_failed(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
        printf("on_topic_add_failed: %d\n", response->response_code);
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_discard(SESSION_T *session, void *context)
{
        puts("on_topic_add_discard");
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_removed(SESSION_T *session, const SVC_REMOVE_TOPICS_RESPONSE_T *response, void *context)
{
        puts("on_topic_removed");
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_remove_discard(SESSION_T *session, void *context)
{
        puts("on_topic_remove_discard");
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}
/*
 *
 */
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

        char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        // Setup for condition variable
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

        // Setup for session
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        // Common params for all add_topic() functions.
        ADD_TOPIC_PARAMS_T common_params = {
                .on_topic_added = on_topic_added,
                .on_topic_add_failed = on_topic_add_failed,
                .on_discard = on_topic_add_discard
        };

        /*
         * Create a stateless topic.
         */
        TOPIC_DETAILS_T *topic_details = create_topic_details_stateless();
        ADD_TOPIC_PARAMS_T stateless_params = common_params;
        stateless_params.topic_path = "stateless";
        stateless_params.details = topic_details;

        apr_thread_mutex_lock(mutex);
        add_topic(session, stateless_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Create a topic with single value string data, but with
         * containing no default data.
         */
        TOPIC_DETAILS_T *string_topic_details = create_topic_details_single_value(M_DATA_TYPE_STRING);
        ADD_TOPIC_PARAMS_T string_params = common_params;
        string_params.topic_path = "string";
        string_params.details = string_topic_details;

        apr_thread_mutex_lock(mutex);
        add_topic(session, string_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Create a topic with single value string data and containing
         * some default data.
         */
        ADD_TOPIC_PARAMS_T string_data_params = common_params;
        string_data_params.topic_path = "string-data";
        string_data_params.details = string_topic_details;
        BUF_T *sample_data_buf = buf_create();
        buf_write_string(sample_data_buf, "Hello, world");
        string_data_params.content = content_create(CONTENT_ENCODING_NONE, sample_data_buf);

        apr_thread_mutex_lock(mutex);
        add_topic(session, string_data_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Create a topic with single value integer data, and with a
         * default value.
         */
        TOPIC_DETAILS_T *integer_topic_details = create_topic_details_single_value(M_DATA_TYPE_INTEGER_STRING);
        integer_topic_details->topic_details_params.integer.default_value = 999;

        ADD_TOPIC_PARAMS_T integer_params = common_params;
        integer_params.topic_path = "integer";
        integer_params.details = integer_topic_details;

        apr_thread_mutex_lock(mutex);
        add_topic(session, integer_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Create a topic with integer data, but using a CONTENT_T to
         * specify the initial data.
         */
        ADD_TOPIC_PARAMS_T integer_data_params = common_params;
        integer_data_params.topic_path = "integer-data";
        integer_data_params.details = integer_topic_details;
        BUF_T *integer_data_buf = buf_create();
        buf_sprintf(integer_data_buf, "%d", 123);
        integer_data_params.content = content_create(CONTENT_ENCODING_NONE, integer_data_buf);

        apr_thread_mutex_lock(mutex);
        add_topic(session, integer_data_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Create a topic with single value decimal data, with a
         * default value and specifying the scale (i.e. positions
         * after the decimal place).
         */
        TOPIC_DETAILS_T *decimal_topic_details = create_topic_details_single_value(M_DATA_TYPE_DECIMAL_STRING);
        decimal_topic_details->topic_details_params.decimal.default_value = 123.456;
        decimal_topic_details->topic_details_params.decimal.scale = 4;

        ADD_TOPIC_PARAMS_T decimal_params = common_params;
        decimal_params.topic_path = "decimal";
        decimal_params.details = decimal_topic_details;

        apr_thread_mutex_lock(mutex);
        add_topic(session, decimal_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Create a topic with decimal data, using a CONTENT_T to
         * specify the initial data.
         */
        ADD_TOPIC_PARAMS_T decimal_data_params = common_params;
        decimal_data_params.topic_path = "decimal-data";
        decimal_data_params.details = decimal_topic_details;
        BUF_T *decimal_data_buf = buf_create();
        buf_sprintf(decimal_data_buf, "%f", 987.654);
        decimal_data_params.content = content_create(CONTENT_ENCODING_NONE, decimal_data_buf);

        apr_thread_mutex_lock(mutex);
        add_topic(session, decimal_data_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Create a slave topic which is an alias for the string-data
         * topic (its master topic).
         */
        TOPIC_DETAILS_T *slave_topic_details = create_topic_details_slave("string-data");
        ADD_TOPIC_PARAMS_T slave_params = common_params;
        slave_params.topic_path = "slave";
        slave_params.details = slave_topic_details;
    
        apr_thread_mutex_lock(mutex);
        add_topic(session, slave_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Record topic data.
         *
         * The C API does not have the concept of "builders" for
         * creating record topic data, but requires you to build a
         * string containing XML that describes the structure of the
         * messages.
         */

        /*
         * First of all, this adds a topic equivalent to single-value
         * strings, but defined with XML.
         */
        BUF_T *manual_schema = buf_create();
        buf_write_string(manual_schema,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        buf_write_string(manual_schema,
                "<field name=\"x\" type=\"string\" default=\"xyzzy\" allowsEmpty=\"true\"/>");
        TOPIC_DETAILS_T *manual_topic_details = create_topic_details_single_value(M_DATA_TYPE_STRING);
        manual_topic_details->user_defined_schema = manual_schema;

        ADD_TOPIC_PARAMS_T string_manual_params = common_params;
        string_manual_params.topic_path = "string-manual";
        string_manual_params.details = manual_topic_details;

        apr_thread_mutex_lock(mutex);
        add_topic(session, string_manual_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * This adds a topic with a record containing multiple fields
         * of different types.
         */
        BUF_T *record_schema = buf_create();
        buf_write_string(record_schema,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        buf_write_string(record_schema,
                "<message topicDataType=\"record\" name=\"MyContent\">");
        buf_write_string(record_schema,
                "<record name=\"Record1\">");
        buf_write_string(record_schema,
                "<field type=\"string\" default=\"\" allowsEmpty=\"true\" name=\"Field1\"/>");
        buf_write_string(record_schema,
                "<field type=\"integerString\" default=\"0\" allowsEmpty=\"false\" name=\"Field2\"/>");
        buf_write_string(record_schema,
                "<field type=\"decimalString\" default=\"0.00\" scale=\"2\" allowsEmpty=\"false\" name=\"Field3\"/>");
        buf_write_string(record_schema,
                "</record>");
        buf_write_string(record_schema,
                "</message>");
        TOPIC_DETAILS_T *record_topic_details = create_topic_details_record();
        record_topic_details->user_defined_schema = record_schema;

        ADD_TOPIC_PARAMS_T record_params = common_params;
        record_params.topic_path = "record";
        record_params.details = record_topic_details;

        apr_thread_mutex_lock(mutex);
        add_topic(session, record_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * We can also remove topics. First, add a couple of topics
         * and then remove their parent topic. All child topics are
         * removed with the parent.
         */
        puts("Adding topics remove_me/1 and remove_me/2");

        ADD_TOPIC_PARAMS_T topic_params = common_params;
        topic_params.details = topic_details;
        topic_params.topic_path = "remove_me/1";

        apr_thread_mutex_lock(mutex);
        add_topic(session, topic_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        topic_params.topic_path = "remove_me/2";
        apr_thread_mutex_lock(mutex);
        add_topic(session, topic_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        puts("Removing topics in 5 seconds...");
        sleep(5);

        REMOVE_TOPICS_PARAMS_T remove_params = {
                .on_removed = on_topic_removed,
                .on_discard = on_topic_remove_discard,
                .topic_selector = ">remove_me"
        };

        apr_thread_mutex_lock(mutex);
        remove_topics(session, remove_params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Close our session, and release resources and memory.
         */
        session_close(session, NULL);
        session_free(session);

        apr_thread_mutex_destroy(mutex);
        apr_thread_cond_destroy(cond);
        apr_pool_destroy(pool);
        apr_terminate();

        return EXIT_SUCCESS;
}
