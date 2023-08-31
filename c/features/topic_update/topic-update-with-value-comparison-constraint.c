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
 * This example creates a String topic and periodically updates it.
 *
 * In order to perform the update, a constraint is evaluated - in this
 * example, the string topic value is compared with a monotonically increasing Int64 value.
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


char *g_topic_value;


ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "time"},
        {'s', "seconds", "Number of seconds to run for before exiting", ARG_OPTIONAL, ARG_HAS_VALUE, "5"},
        END_OF_ARG_OPTS
};


// Handlers for add and set topic.
static int on_topic_update_add_and_set(
        DIFFUSION_TOPIC_CREATION_RESULT_T result,
        void *context)
{
        char *topic_path = (char *) context;
        printf("Topic %s has been updated.\n\n", topic_path);

        return HANDLER_SUCCESS;
}


static int on_error(
        SESSION_T *session,
        const DIFFUSION_ERROR_T *error)
{
        char *topic_path = (char *) error->context;
        printf("Error while attempting to update topic %s: %s\n\n", topic_path, error->message);

        return HANDLER_SUCCESS;
}


// Handler for value stream
static int on_value(
        const char* topic_path,
        const TOPIC_SPECIFICATION_T *const specification,
        const DIFFUSION_DATATYPE datatype,
        const DIFFUSION_VALUE_T *const old_value,
        const DIFFUSION_VALUE_T *const new_value,
        void *context)
{
        DIFFUSION_API_ERROR api_error;
        bool success = read_diffusion_string_value(new_value, &g_topic_value, &api_error);

        if(success) {
                printf("[%s] --> %s\n", topic_path, g_topic_value);
        }
        else {
                printf("Error during diffusion value read: %s\n", get_diffusion_api_error_description(api_error));
                diffusion_api_error_free(api_error);
        }
        return HANDLER_SUCCESS;
}


static int on_subscription(
        const char* topic_path,
        const TOPIC_SPECIFICATION_T *specification,
        void *context)
{
        printf("Subscribed to topic: %s\n", topic_path);
        return HANDLER_SUCCESS;
}


static int on_unsubscription(
        const char* topic_path,
        const TOPIC_SPECIFICATION_T *specification,
        NOTIFY_UNSUBSCRIPTION_REASON_T reason,
        void *context)
{
        printf("Unsubscribed from topic: %s\n", topic_path);
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

        // create value stream and subscribe to topic
        VALUE_STREAM_T value_stream = {
                .datatype = DATATYPE_STRING,
                .on_value = on_value,
                .on_subscription = on_subscription,
                .on_unsubscription = on_unsubscription
        };
        add_stream(session, topic_name, &value_stream);

        SUBSCRIPTION_PARAMS_T subscribe_params = {
                .topic_selector = topic_name
        };
        subscribe(session, subscribe_params);

        // Sleep for a while
        sleep(2);

        // create the topic and set its initial value
        TOPIC_SPECIFICATION_T *topic_specification =
                topic_specification_init(TOPIC_TYPE_STRING);

        BUF_T *value_buf = buf_create();
        write_diffusion_string_value("0", value_buf);

        DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T initial_params = {
                .topic_path = (char *) topic_name,
                .update = value_buf,
                .specification = topic_specification,
                .datatype = DATATYPE_STRING,
                .on_topic_update_add_and_set = on_topic_update_add_and_set,
                .on_error = on_error,
                .context = (char *) topic_name
        };
        diffusion_topic_update_add_and_set(session, initial_params);
        buf_free(value_buf);

        // Sleep for a while
        sleep(2);

        // Begin loop, updating the string topic, using a value comparison update constraint
        time_t end_time = time(NULL) + seconds;
        int64_t comparison_value = 1;

        printf("Loop has started.\n");
        while(time(NULL) < end_time) {
                DIFFUSION_UPDATE_CONSTRAINT_VALUE_T *constraint_value =
                        diffusion_update_constraint_value_from_int64(comparison_value);

                DIFFUSION_TOPIC_UPDATE_CONSTRAINT_T *update_constraint =
                        diffusion_topic_update_constraint_value_comparison(
                                DIFFUSION_TOPIC_UPDATE_CONSTRAINT_OPERATOR_LT,
                                constraint_value
                        );
                printf(
                        "Update Constraint --> current topic value (%s) < constraint_value (%ld)\n",
                        g_topic_value,
                        (long) comparison_value
                );

                // Write the [comparison_value] in a string to update the topic
                char string_value[10];
                sprintf(string_value, "%ld", (long) comparison_value);

                BUF_T *value_update_buf = buf_create();
                write_diffusion_string_value(string_value, value_update_buf);

                DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T update_params = {
                        .topic_path = (char *) topic_name,
                        .update = value_update_buf,
                        .specification = topic_specification,
                        .datatype = DATATYPE_STRING,
                        .on_topic_update_add_and_set = on_topic_update_add_and_set,
                        .on_error = on_error,
                        .context = (char *) topic_name
                };

                printf("Updating Topic '%s' with value '%ld'\n", topic_name, (long) comparison_value);
                diffusion_topic_update_add_and_set_with_constraint(
                        session,
                        update_constraint,
                        update_params
                );
                buf_free(value_update_buf);
                diffusion_update_constraint_value_free(constraint_value);
                diffusion_topic_update_constraint_free(update_constraint);

                sleep(1);

                comparison_value -= 1;
        }
        printf("Loop has terminated. Closing session.\n");

        // Close session and free resources.
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        topic_specification_free(topic_specification);
        hash_free(options, NULL, free);
        free(g_topic_value);

        return EXIT_SUCCESS;
}
