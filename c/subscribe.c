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
 * This is a sample client which connects to Diffusion and subscribes
 * to topics using a user-specified selector. Any messages received on
 * those topics are then displayed to standard output.
 */

#include <stdio.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'t', "topic_selector", "Topic selector", ARG_REQUIRED, ARG_HAS_VALUE, NULL},
        END_OF_ARG_OPTS
};

/*
 * This callback is used when the session state changes, e.g. when a session
 * moves from a "connecting" to a "connected" state, or from "connected" to
 * "closed".
 */
static void
on_session_state_changed(SESSION_T *session,
                         const SESSION_STATE_T old_state,
                         const SESSION_STATE_T new_state)
{
        printf("Session state changed from %s (%d) to %s (%d)\n",
               session_state_as_string(old_state), old_state,
               session_state_as_string(new_state), new_state);
}

/*
 * When a subscribed message is received, this callback is invoked.
 */
static int
on_topic_message(SESSION_T *session, const TOPIC_MESSAGE_T *msg)
{
        printf("Received message for topic %s\n", msg->name);
        printf("Payload: %.*s\n", (int)msg->payload->len, msg->payload->data);
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
 * This is callback is for when Diffusion response to an unsubscription
 * request to a topic, and only indicates that the request has been received.
 */
static int
on_unsubscribe(SESSION_T *session, void *context_data)
{
        printf("on_unsubscribe\n");
        return HANDLER_SUCCESS;
}

/*
 * Publishers and control clients may choose to subscribe any other client to
 * a topic of their choice at any time. We register this callback to capture
 * messages from these topics and display them.
 */
static int
on_unexpected_topic_message(SESSION_T *session, const TOPIC_MESSAGE_T *msg)
{
        printf("Received a message for a topic we didn't subscribe to (%s)\n", msg->name);
        printf("Payload: %.*s\n", (int)msg->payload->len, msg->payload->data);
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

/*
 * This callback is used when we receive notification that this client has been
 * unsubscribed from a specific topic. Causes of the unsubscription are the same
 * as those for subscription.
 */
static int
on_notify_unsubscription(SESSION_T *session, const SVC_NOTIFY_UNSUBSCRIPTION_REQUEST_T *request, void *context)
{
        printf("on_notify_unsubscription: ID: %d, Path: %s, Reason: %d\n",
               request->topic_id,
               request->topic_path,
               request->reason);
        return HANDLER_SUCCESS;
}

int
main(int argc, char **argv)
{
        /*
         * Standard command-line parsing
         */
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return EXIT_FAILURE;
        }

        char *url = hash_get(options, "url");
        char *topic = hash_get(options, "topic_selector");

        /*
         * A SESSION_LISTENER_T holds callbacks to inform the client
         * about changes to the state. Used here for informational
         * purposes only.
         */
        SESSION_LISTENER_T session_listener = { 0 };
        session_listener.on_state_changed = &on_session_state_changed;

        /*
         * Creating a session requires at least a URL. Creating a
         * session initiates a connection with Diffusion.
         */
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = NULL;
        session = session_create(url, NULL, NULL, &session_listener, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * When issuing commands to Diffusion (in this case, subscribe
         * to a topic), it's typical that more than one message may be
         * received in response and a handler can be installed for
         * each message type. In the case of subscription, we can
         * install handlers for:
         * 1. The topic message data (on_topic_message).
         * 2. Notification that the subscription has been received
         *    (on_subscribe).
         * 3. Topic details (on_topic_details).
         */
        notify_subscription_register(session,(NOTIFY_SUBSCRIPTION_PARAMS_T) { .on_notify_subscription = on_notify_subscription });
        notify_unsubscription_register(session, (NOTIFY_UNSUBSCRIPTION_PARAMS_T) { .on_notify_unsubscription = on_notify_unsubscription });

        subscribe(session, (SUBSCRIPTION_PARAMS_T) { .topic_selector = topic, .on_topic_message = on_topic_message, .on_subscribe = on_subscribe });

        /*
         * Install a global topic handler to capture messages for
         * topics we haven't explicitly subscribed to, and therefore
         * don't have a specific handler for.
         */
        session->global_topic_handler = on_unexpected_topic_message;

        /*
         * Receive messages for 5 seconds.
         */
        sleep(5);

        /*
         * Unsubscribe from the topic
         */
        unsubscribe(session, (UNSUBSCRIPTION_PARAMS_T) {.topic_selector = topic, .on_unsubscribe = on_unsubscribe} );

        /*
         * Wait for any unsubscription notifications to be received.
         */
        sleep(5);

        /*
         * Politely tell Diffusion we're closing down.
         */
        session_close(session, NULL);
        session_free(session);

        return EXIT_SUCCESS;
}
