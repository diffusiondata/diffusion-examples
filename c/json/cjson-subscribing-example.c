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
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'t', "topic", "Topic name to subscribe", ARG_OPTIONAL, ARG_HAS_VALUE, "processes"},
        END_OF_ARG_OPTS
};

/*
 * When a subscribed message is received, this callback is invoked.
 */
static int
on_topic_message(SESSION_T *session, const TOPIC_MESSAGE_T *msg)
{
        printf("Received message for topic %s (%ld bytes)\n", msg->name, msg->payload->len);

        if(msg->details != NULL) {
            if(msg->details->topic_type == TOPIC_TYPE_JSON) {
                // Convert payload to a JSON string
                BUF_T *json = cbor_to_json(msg->payload->data, msg->payload->len);
                printf("As JSON: %.*s\n", (int)json->len, json->data);
                buf_free(json);

            }
            else {
                printf("Hexdump of binary data:\n");
                hexdump_buf(msg->payload);
            }
        }
        else {
            printf("Payload: %.*s\n",
                   (int)msg->payload->len,
                   msg->payload->data);
        }

        return HANDLER_SUCCESS;
}

/*
 * This callback is fired when Diffusion responds to say that a topic
 * subscription request has been received and processed.
 */
static int
on_subscribe(SESSION_T *session, void *context_data)
{
        printf("on_subscribe\n");
        return HANDLER_SUCCESS;
}

/*
 * We use this callback when Diffusion notifies us that we've been subscribed
 * to a topic. Note that this could be called for topics that we haven't
 * explicitly subscribed to - other control clients or publishers may ask to
 * subscribe us to a topic.
 */
static int
on_notify_subscription(SESSION_T *session, const SVC_NOTIFY_SUBSCRIPTION_REQUEST_T *request, void *context)
{
        printf("on_notify_subscription: %d: \"%s\"\n",
               request->topic_info.topic_id,
               request->topic_info.topic_path);
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

        notify_subscription_register(session,(NOTIFY_SUBSCRIPTION_PARAMS_T) { .on_notify_subscription = on_notify_subscription });

        subscribe(session, (SUBSCRIPTION_PARAMS_T) { .topic_selector = topic_name, .on_topic_message = on_topic_message, .on_subscribe = on_subscribe });

        sleep(60);

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
