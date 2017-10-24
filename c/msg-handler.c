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
 * @since 5.6
 */

/*
 * This example shows how to receive messages, rather than topic
 * updates, as part of MessagingControl.
 *
 * You may register a handler against a path, which will
 * become the only destination for messages to that path (where
 * the control client which is considered "active" is determined by
 * the server).
 *
 * See send-msg.c for an example of how to send messages to a
 * path from a client.
 */

#include <stdio.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "conversation.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'t', "topic", "Topic name", ARG_REQUIRED, ARG_HAS_VALUE, "echo"},
        END_OF_ARG_OPTS
};

/*
 * Function to be called when the message receiver has been registered.
 */
int
on_registered(SESSION_T *session, void *context)
{
        printf("on_registered()\n");
        return HANDLER_SUCCESS;
}

/*
 * Function called on receipt of a message from a client.
 *
 * We print the following information:
 *   1. The message path on which the message was received.
 *   2. A hexdump of the message content.
 *   3. The headers associated with the message.
 *   4. The session properties that were requested when the handler was
 *      registered.
 *   5. The user context, as a string.
 */
int
on_msg(SESSION_T *session, const SVC_SEND_RECEIVER_CLIENT_REQUEST_T *request, void *context)
{
        printf("Received message on path %s\n", request->topic_path);
        hexdump_buf(request->content->data);
        printf("Headers:\n");
        if(request->send_options.headers->first == NULL) {
                printf("  No headers\n");
        }
        else {
                for(LIST_NODE_T *node = request->send_options.headers->first;
                    node != NULL;
                    node = node->next) {
                        printf("  Header: %s\n", (char *)node->data);
                }
        }

        printf("Session properties:\n");
        char **keys = hash_keys(request->session_properties);
        if(keys == NULL || *keys == NULL) {
                printf("  No properties\n");
        }
        else {
                for(char **k = keys; *k != NULL; k++) {
                        char *v = hash_get(request->session_properties, *k);
                        printf("  %s=%s\n", *k, v);
                }
        }
        free(keys);

        if(context != NULL) {
                printf("Context: %s\n", (char *)context);
        }

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

        char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }
        char *topic = hash_get(options, "topic");

        /*
         * Create a session with Diffusion.
         */
        SESSION_T *session = NULL;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        char *session_id = session_id_to_string(session->id);
        printf("Session created, id=%s\n", session_id);
        free(session_id);

        /*
         * Register a message handler, and for each message ask for
         * the $Principal property to be provided.
         */
        LIST_T *requested_properties = list_create();
        list_append_last(requested_properties, "$Principal");

        MSG_RECEIVER_REGISTRATION_PARAMS_T params = {
                .on_registered = on_registered,
                .topic_path = topic,
                .on_message = on_msg,
                .session_properties = requested_properties
        };
        register_msg_handler(session, params);

        /*
         * Accept messages for a while, then deregister.
         */
        sleep(30);
        deregister_msg_handler(session, params);

        /*
         * Close session and clean up.
         */
        session_close(session, NULL);
        session_free(session);

        list_free(requested_properties, NULL);

        return EXIT_SUCCESS;
}
