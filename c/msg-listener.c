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
 * This example shows how to receive messages, rather than topic
 * updates.
 *
 * Message streams may be received via a topic endpoint. We can
 * register a listener against a specific endpoint to process the
 * messages, and we can register a listener for all messages not
 * otherwise handled.
 *
 * See send-msg.c for an example of how to send messages to an
 * endpoint from a client.
 */

#include <stdio.h>
#include <unistd.h>

#include "diffusion.h"
#include "conversation.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "dpt://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
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
 */
int
on_msg(SESSION_T *session, const SVC_SEND_RECEIVER_CLIENT_REQUEST_T *request, void *context)
{
        printf("Received message on topic path %s\n", request->topic_path);
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

/*
 * Function called on receipt of a message directed to this session.
 */
int
on_stream_message(SESSION_T *session, const STREAM_MESSAGE_T *message, void *context)
{
        printf("Received stream message on listener for topic path %s\n", message->topic_path);
        hexdump_buf(message->content.data);

        if(context != NULL) {
                printf("Context: %s\n", (char *)context);
        }

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

        // Create a session with Diffusion.
        SESSION_T *session = NULL;
        DIFFUSION_ERROR_T error;
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return 1;
        }

        char *session_id = session_id_to_string(session->id);
        printf("Session created, id=%s\n", session_id);
        free(session_id);

        // Register a listener for messages on the given topic path.
        MSG_LISTENER_REGISTRATION_PARAMS_T listener_params = {
                .topic_path = topic,
                .listener = on_stream_message,
                .context = "xyzzy"
        };
        register_msg_listener(session, listener_params);

        // Register a listener for any other messages.
        MSG_LISTENER_REGISTRATION_PARAMS_T global_listener_params = {
                .listener = on_stream_message,
                .context = "UNEXPECTED"
        };
        register_msg_listener(session, global_listener_params);

        // Accept messages for a while.
        sleep(30);

        // Deregister explicit listener.
        deregister_msg_listener(session, (MSG_LISTENER_DEREGISTRATION_PARAMS_T){.topic_path = topic});
        // Deregister global listener.
        deregister_msg_listener(session, (MSG_LISTENER_DEREGISTRATION_PARAMS_T){});

        session_close(session, NULL);
        return 0;
}
