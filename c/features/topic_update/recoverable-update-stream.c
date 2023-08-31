/**
 * Copyright © 2023 DiffusionData Ltd.
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
 * @author DiffusionData Limited
 * @since 6.10
 */

/*
 * This example creates a recoverable update stream.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#ifndef WIN32
        #include <unistd.h>
#else
        #define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "args.h"
#include "conversation.h"



ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "time"},
        {'s', "seconds", "Number of seconds to run for before exiting", ARG_OPTIONAL, ARG_HAS_VALUE, "30"},
        END_OF_ARG_OPTS
};


static int on_callback(
        const SESSION_T *session,
        const DIFFUSION_RECOVERABLE_UPDATE_STREAM_T *recoverable_update_stream,
        const DIFFUSION_RECOVERABLE_UPDATE_STREAM_CALLBACK_RESPONSE_T *response,
        const DIFFUSION_ERROR_T *error,
        void *context)
{
        if (error != NULL && diffusion_recoverable_update_stream_is_error_recoverable(error)) {
                printf("Recoverable error detected. Attempting to recover.\n");

                diffusion_recoverable_update_stream_recover(
                        session, recoverable_update_stream
                );
                return HANDLER_SUCCESS;
        }

        if (response != NULL) {
                printf("Topic update was successful.\n");
        }
        else {
                printf("An error occurred while updating the topic: %s (%d)\n", error->message, error->code);
        }
        return HANDLER_SUCCESS;
}


// Program entry point.
int main(int argc, char** argv)
{
        // Standard command-line parsing.
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return EXIT_FAILURE;
        }

        const char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        const char *password = hash_get(options, "credentials");
        const char *topic_name = hash_get(options, "topic");
        const long seconds = atol(hash_get(options, "seconds"));

        CREDENTIALS_T *credentials = (password != NULL) ?
                credentials_create_password(password) :
                NULL;

        // Create a session with the Diffusion server.
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        // Create a new recoverable update stream for the topic.
        TOPIC_SPECIFICATION_T *spec = topic_specification_init(TOPIC_TYPE_STRING);
        DIFFUSION_UPDATE_STREAM_BUILDER_T *builder = diffusion_update_stream_builder_init();
        diffusion_update_stream_builder_topic_specification(builder, spec, NULL);

        DIFFUSION_RETRY_STRATEGY_T *retry_strategy =
                diffusion_retry_strategy_create(250, 100, NULL);

        // The update stream will have the following recovery capabilities
        // 100 retry attempts, 250ms delay between attempts
        DIFFUSION_RECOVERABLE_UPDATE_STREAM_T *update_stream =
                diffusion_update_stream_builder_create_recoverable_update_stream(
                        builder, topic_name, DATATYPE_STRING, retry_strategy, NULL
                );
        diffusion_update_stream_builder_free(builder);
        topic_specification_free(spec);

        // begin timed loop, updating the topic with the current timestamp
        time_t end_time = time(NULL) + seconds;

        while(time(NULL) < end_time) {
                // Compose the update content.
                const time_t time_now = time(NULL);
                const char *time_str = ctime(&time_now);

                // Get the update stream's current value.
                DIFFUSION_VALUE_T *current_value = diffusion_recoverable_update_stream_get(update_stream, NULL);
                if(current_value != NULL) {
                        char *value;
                        read_diffusion_string_value(current_value, &value, NULL);
                        printf("current topic value: %s", value);
                        diffusion_value_free(current_value);
                        free(value);
                }

                // Create a BUF_T and write the string datatype value into it.
                BUF_T *update_buf = buf_create();
                write_diffusion_string_value(time_str, update_buf);

                DIFFUSION_RECOVERABLE_UPDATE_STREAM_PARAMS_T params = {
                        .on_callback = on_callback
                };

                // Update the topic with the update stream.
                diffusion_recoverable_update_stream_set(session, update_stream, update_buf, params, NULL);
                buf_free(update_buf);

                sleep(1);
        }

        // Close session and free resources.
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        diffusion_recoverable_update_stream_free(update_stream);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
