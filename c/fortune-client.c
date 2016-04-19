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
 * @since 5.0
 */

/**
 * This is a minimal "fetch" client which can be used with rc-fortune.
 */

#include <stdio.h>
#include <unistd.h>

#include "diffusion.h"
#include "args.h"

extern void topic_message_debug();

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "dpt://localhost:8080"},
        {'t', "topic_selector", "Topic selector", ARG_OPTIONAL, ARG_HAS_VALUE, ">fortune"},
        END_OF_ARG_OPTS
};

/**
 * Callback for displaying the results of a successful "fetch" request.
 */
static int
fortune_topic_handler(SESSION_T *session, const TOPIC_MESSAGE_T *msg)
{
        printf("Your fortune: %.*s\n", (int)msg->payload->len, msg->payload->data);

        return 0;
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
        char *topic = hash_get(options, "topic_selector");

        // Create a session with the Diffusion server.
        SESSION_T *session;
        DIFFUSION_ERROR_T error;
        session = session_create(url, NULL, NULL, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "Failed to create session: %s\n", error.message);
                return 1;
        }

        // Fetch a fortune, and specify that the asynchronously returned
        // message is handled by fortune_topic_handler().
        fetch(session, (FETCH_PARAMS_T) { .selector = topic, .on_topic_message = fortune_topic_handler });

        // Wait a few seconds for the message to be returned.
        sleep(10);

        // Gracefully close the client session.
        session_close(session, &error);

        return 0;
}
