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

// Topic selector, selector set delimiter
#define DELIM "////"


#define BUILD_TOPIC_PARAMS(TOPIC_NAME, DETAILS) {\
    .on_topic_added = on_topic_added, \
    .on_topic_add_failed = on_topic_add_failed, \
    .on_discard = on_topic_add_discard, \
    .topic_path = (TOPIC_NAME), \
    .details = (DETAILS), \
    .context = (void *)(TOPIC_NAME) \
}



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
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

// Various handlers which are common to all add_topic() functions.
static int
on_topic_added(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
        printf("on_topic_added: %s\n", (const char *)context);
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_failed(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
        printf("on_topic_add_failed: %s -> %d\n", (const char *)context, response->response_code);
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

        const char *url = hash_get(options, "url");
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

        /*
         * Create a JSON topic.
         */
        {
            TOPIC_DETAILS_T *json_topic_details = create_topic_details_json();
            ADD_TOPIC_PARAMS_T json_params = BUILD_TOPIC_PARAMS("json", json_topic_details);

            CBOR_GENERATOR_T *cbor_generator = cbor_generator_create();
            const char *json_message_str = "Hello world, this is a JSON string.";
            cbor_write_text_string(cbor_generator, json_message_str, strlen(json_message_str));
            BUF_T *cbor_buf = buf_create();
            buf_write_bytes(cbor_buf, cbor_generator->data, cbor_generator->len);

            CONTENT_T *json_content = content_create(CONTENT_ENCODING_NONE, cbor_buf);
            json_params.content = json_content;

            apr_thread_mutex_lock(mutex);
            add_topic(session, json_params);
            apr_thread_cond_wait(cond, mutex);
            apr_thread_mutex_unlock(mutex);

            content_free(json_content);
            cbor_generator_free(cbor_generator);
            buf_free(cbor_buf);
            topic_details_free(json_topic_details);
        }


        /*
         * Create a slave topic which is an alias for the string-data
         * topic (its master topic).
         */
        TOPIC_DETAILS_T *slave_topic_details = create_topic_details_slave("string-data");
        ADD_TOPIC_PARAMS_T slave_params = BUILD_TOPIC_PARAMS("slave", slave_topic_details);
    
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
         * This adds a topic with a record containing multiple fields
         * of different types.
         */
        {
            BUF_T *record_schema = buf_create();
            buf_write_string(record_schema,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                "<message topicDataType=\"record\" name=\"MyContent\">"
                "<record name=\"Record1\">"
                "<field type=\"string\" default=\"\" allowsEmpty=\"true\" name=\"Field1\"/>"
                "<field type=\"integerString\" default=\"0\" allowsEmpty=\"false\" name=\"Field2\"/>"
                "<field type=\"decimalString\" default=\"0.00\" scale=\"2\" allowsEmpty=\"false\" name=\"Field3\"/>"
                "</record>"
                "</message>");
            TOPIC_DETAILS_T *record_topic_details = create_topic_details_record();
            record_topic_details->user_defined_schema = record_schema;

            ADD_TOPIC_PARAMS_T record_params = BUILD_TOPIC_PARAMS("record", record_topic_details);

            apr_thread_mutex_lock(mutex);
            add_topic(session, record_params);
            apr_thread_cond_wait(cond, mutex);
            apr_thread_mutex_unlock(mutex);
            topic_details_free(record_topic_details);
        }

        /*
         * Create a topic with binary data
         */
        {
            TOPIC_DETAILS_T *binary_topic_details = create_topic_details_binary();
            ADD_TOPIC_PARAMS_T binary_params = BUILD_TOPIC_PARAMS("binary-data", binary_topic_details);

            char binary_bytes[] = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };

            BUF_T *binary_data_buf = buf_create();
            buf_write_bytes(binary_data_buf, binary_bytes, sizeof(binary_bytes));
            CONTENT_T *binary_content = content_create(CONTENT_ENCODING_NONE, binary_data_buf);
            binary_params.content = binary_content;

            apr_thread_mutex_lock(mutex);
            add_topic(session, binary_params);
            apr_thread_cond_wait(cond, mutex);
            apr_thread_mutex_unlock(mutex);
            content_free(binary_content);
            topic_details_free(binary_topic_details);
        }

        /*
         * We can also remove topics.
         */
        {
            puts("Removing topics in 5 seconds...");
            sleep(5);

            REMOVE_TOPICS_PARAMS_T remove_params = {
                    .on_removed = on_topic_removed,
                    .on_discard = on_topic_remove_discard,
                    .topic_selector = "#json" DELIM "record" DELIM "binary-data"
            };

            apr_thread_mutex_lock(mutex);
            remove_topics(session, remove_params);
            apr_thread_cond_wait(cond, mutex);
            apr_thread_mutex_unlock(mutex);
        }

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
