/**
 * Copyright © 2014 - 2022 Push Technology Ltd.
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
 * This example is similar to that in subscribe.c but subscribes to the
 * specified topic twice. This is to show that multiple listeners can be
 * registered for a topic, or more likely, overlapping topic selectors cause
 * multiple callbacks to be issued.
 *
 * For clarity, many of the callbacks for handling session state have been
 * omitted; see subscribe.c for more detailed examples.
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
 * When a subscribed message is received by the first listener, this
 * callback is invoked.
 */
static int on_topic_message_1(
        SESSION_T *session,
        const TOPIC_MESSAGE_T *msg)
{
        printf("First handler: Received message for topic %s\n", msg->name);
        printf("Payload: %.*s\n", (int)msg->payload->len, msg->payload->data);
        return HANDLER_SUCCESS;
}

/*
 * When a subscribed message is received by the second listener, this
 * callback is invoked.
 */
static int
on_topic_message_2(SESSION_T *session, const TOPIC_MESSAGE_T *msg)
{
        printf("Second handler: Received message for topic %s\n", msg->name);
        printf("Payload: %.*s\n", (int)msg->payload->len, msg->payload->data);
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
        char *topic = hash_get(options, "topic_selector");

        /*
         * Create a session with Diffusion.
         */
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = NULL;
        session = session_create(url, NULL, NULL, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * When issuing commands to Diffusion (in this case, subscribe to
         * a topic), it's typical that more than one message may be
         * received in response and a handler can be installed for each
         * message type.
         */
        SUBSCRIPTION_PARAMS_T sub_params_1 = {
                .topic_selector = topic,
                .on_topic_message = on_topic_message_1
        };

        SUBSCRIPTION_PARAMS_T sub_params_2 = {
                .topic_selector = topic,
                .on_topic_message = on_topic_message_2
        };

        /*
         * Register two subscription handlers for the same topic. The
         * first handler is replaced by the second, so we will only
         * see on_topic_message_2() invoked.
         */
        TOPIC_HANDLER_T old_handlers = NULL;
        if((old_handlers = subscribe(session, sub_params_1)) != NULL) {
                puts("Replacing existing handlers for topic selector");
        }
        if((old_handlers = subscribe(session, sub_params_2)) != NULL) {
                puts("Replacing existing handlers for topic selector");
        }

        /*
         * Keep receiving messages for 10 seconds.
         */
        sleep(10);

        /*
         * Politely close the connection.
         */
        session_close(session, NULL);
        session_free(session);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
