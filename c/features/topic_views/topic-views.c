/**
 * Copyright © 2019 - 2022 Push Technology Ltd.
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
 * @since 6.3
 */

/*
 * This example creates a topic view.
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
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "source"},
        {'r', "reference-topic", "Reference topic name to be mapped", ARG_OPTIONAL, ARG_HAS_VALUE, "reference"},
        {'s', "seconds", "Number of seconds to run for before exiting", ARG_OPTIONAL, ARG_HAS_VALUE, "30"},
        END_OF_ARG_OPTS
};

// Handlers for add topic feature.
static int on_topic_added_with_specification(
        SESSION_T *session,
        TOPIC_ADD_RESULT_CODE result_code,
        void *context)
{
        printf("Added topic \"%s\"\n", (const char *)context);
        return HANDLER_SUCCESS;
}


static int on_topic_add_failed_with_specification(
        SESSION_T *session,
        TOPIC_ADD_FAIL_RESULT_CODE result_code,
        const DIFFUSION_ERROR_T *error,
        void *context)
{
        printf("Failed to add topic \"%s\" (%d)\n", (const char *)context, result_code);
        return HANDLER_SUCCESS;
}


static int on_topic_add_discard(SESSION_T *session, void *context)
{
        printf("Topic add discarded\n");
        return HANDLER_SUCCESS;
}


static ADD_TOPIC_CALLBACK_T create_topic_callback(const char *topic_name)
{
        ADD_TOPIC_CALLBACK_T callback = {
                .on_topic_added_with_specification = on_topic_added_with_specification,
                .on_topic_add_failed_with_specification = on_topic_add_failed_with_specification,
                .on_discard = on_topic_add_discard,
                .context = (char *)topic_name
        };

        return callback;
}


static int on_topic_view_created(
        const DIFFUSION_TOPIC_VIEW_T *topic_view,
        void *context)
{
        char *view_name = diffusion_topic_view_get_name(topic_view);
        char *spec = diffusion_topic_view_get_specification(topic_view);

        printf("Topic view \"%s\" created with specification \"%s\"\n", view_name, spec);

        free(view_name);
        free(spec);
        return HANDLER_SUCCESS;
}


static int on_topic_update(void *context)
{
        printf("Topic update success\n");
        return HANDLER_SUCCESS;
}


static int on_subscription(
        const char *const topic_path,
        const TOPIC_SPECIFICATION_T *const specification,
        void *context)
{
        printf("Subscribed to \"%s\"\n", topic_path);
        return HANDLER_SUCCESS;
}


static int on_value(
        const char *const topic_path,
        const TOPIC_SPECIFICATION_T *const specification,
        DIFFUSION_DATATYPE datatype,
        const DIFFUSION_VALUE_T *const old_value,
        const DIFFUSION_VALUE_T *const new_value,
        void *context)
{
        char *result;
        read_diffusion_string_value(new_value, &result, NULL);

        printf("Value from \"%s\" topic: %s\n", topic_path, result);

        free(result);
        return HANDLER_SUCCESS;
}


static int on_error(
        SESSION_T *session,
        const DIFFUSION_ERROR_T *error)
{
        printf("Error: %s\n", error->message);
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
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }
        const char *topic_name = hash_get(options, "topic");
        const char *reference_topic_name = hash_get(options, "reference-topic");
        const long seconds = atol(hash_get(options, "seconds"));

        // Create a session with the Diffusion server.
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        ADD_TOPIC_CALLBACK_T callback = create_topic_callback(topic_name);
        TOPIC_SPECIFICATION_T *spec = topic_specification_init(TOPIC_TYPE_STRING);

        // Create the source topic.
        add_topic_from_specification(session, topic_name, spec, callback);

        // Sleep for a while
        sleep(5);

        topic_specification_free(spec);

        // Create the topic view specification string.
        // Maps topic to reference topic.
        BUF_T *buf = buf_create();
        buf_sprintf(buf, "map %s to %s", topic_name, reference_topic_name);

        char *topic_view_spec = buf_as_string(buf);
        buf_free(buf);

        DIFFUSION_CREATE_TOPIC_VIEW_PARAMS_T topic_view_params = {
                .view = "example-view",
                .specification = topic_view_spec,
                .on_topic_view_created = on_topic_view_created,
                .on_error = on_error
        };

        // Send the request to create the topic view.
        diffusion_topic_views_create_topic_view(session, topic_view_params, NULL);
        free(topic_view_spec);

        VALUE_STREAM_T value_stream = {
                .datatype = DATATYPE_STRING,
                .on_subscription = on_subscription,
                .on_value = on_value
        };

        // Add a value stream for the reference topic to receive update values.
        add_stream(session, reference_topic_name, &value_stream);

        SUBSCRIPTION_PARAMS_T subscribe_params = {
                .topic_selector = reference_topic_name,
                .on_topic_message = NULL
        };

        // Subscribe to the reference topic.
        subscribe(session, subscribe_params);

        time_t end_time = time(NULL) + seconds;

        while(time(NULL) < end_time) {
                // Compose the update content
                const time_t time_now = time(NULL);
                const char *time_str = ctime(&time_now);

                // Create a BUF_T and write the string datatype value into it.
                BUF_T *value = buf_create();
                write_diffusion_string_value(time_str, value);

                DIFFUSION_TOPIC_UPDATE_SET_PARAMS_T topic_update_params = {
                        .topic_path = topic_name,
                        .datatype = DATATYPE_STRING,
                        .update = value,
                        .on_topic_update = on_topic_update,
                        .on_error = on_error
                };

                // Update the source topic.
                diffusion_topic_update_set(session, topic_update_params);
                buf_free(value);

                sleep(1);
        }

        // Close session and free resources.
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
