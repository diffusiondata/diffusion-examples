/**
 * Copyright © 2014, 2015 Push Technology Ltd.
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
 * @author Push Technology Limited
 * @since 5.6
 */

/**
 * This example shows how a message can be sent from a client to Diffusion
 * via a topic.
 *
 * See msg-handler.c for an example of how to receive these messages
 * in a control client.
 */

#include <stdio.h>
#include <unistd.h>

#include <apr.h>
#include <apr_thread_mutex.h>
#include <apr_thread_cond.h>

#include "diffusion.h"
#include "args.h"

apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "dpt://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'t', "topic", "Topic name", ARG_REQUIRED, ARG_HAS_VALUE, "echo"},
        {'d', "data", "Data to send", ARG_REQUIRED, ARG_HAS_VALUE, NULL},
        END_OF_ARG_OPTS
};

/**
 * Callback invoked when/if a message is sent on the topic that the
 * client is writing to.
 */
static int
on_send(SESSION_T *session, void *context)
{
        printf("on_send() successful. Context=\"%s\".\n", (char *)context);

        // Allow main thread to continue.
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);

        return HANDLER_SUCCESS;
}

int
main(int argc, char **argv)
{
        // Standard command line parsing.
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return 1;
        }

        char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }
        char *topic = hash_get(options, "topic");

        // Setup for condition variable.
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

        // Create a session with Diffusion.
        SESSION_T *session = NULL;
        DIFFUSION_ERROR_T error;
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return 1;
        }

        // Create a payload.
        char *data = hash_get(options, "data");
        BUF_T *payload = buf_create();
        buf_write_bytes(payload, data, strlen(data));

        // Build up some headers to send with the message.
        LIST_T *headers = list_create();
        list_append_last(headers, "apple");
        list_append_last(headers, "train");

        // Parameters for send_msg() call.
        SEND_MSG_PARAMS_T params = {
                .topic_path = topic,
                .payload = *payload,
                .headers = headers,
                .priority = CLIENT_SEND_PRIORITY_NORMAL,
                .on_send = on_send,
                .context = "FOO"
        };

        // Send the message and wait for the callback to acknowledge delivery.
        apr_thread_mutex_lock(mutex);
        send_msg(session, params);
        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        // Politely close the client connection.
        session_close(session, &error);

        return 0;
}
