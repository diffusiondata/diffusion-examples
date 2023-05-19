/**
 * Copyright © 2020 - 2022 Push Technology Ltd.
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
 * @since 6.6
 */

/*
 * This example creates a Time series topic (of String datatype), appends a sequence of
 * values to it and edits the first value.
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
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "time-series-edit"},
        END_OF_ARG_OPTS
};

/*
 * Handlers for add topic feature.
 */
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


static int on_append(
        const DIFFUSION_TIME_SERIES_EVENT_METADATA_T *event_metadata,
        void *context)
{
        printf("time series append success\n");
        return HANDLER_SUCCESS;
}


static int on_edit(
    const DIFFUSION_TIME_SERIES_EVENT_METADATA_T *event_metadata,
    void *context)
{
        printf("time series edit success\n");
        return HANDLER_SUCCESS;
}


static int on_error(
        SESSION_T *session,
        const DIFFUSION_ERROR_T *error)
{
        printf("time series append error: %s\n", error->message);
        return HANDLER_SUCCESS;
}


static int on_error_edit(
        SESSION_T *session,
        const DIFFUSION_ERROR_T *error)
{
        printf("time series edit error: %s\n", error->message);
        return HANDLER_SUCCESS;
}


static void append_value_to_time_series_topic(
        SESSION_T *session,
        char *topic_path,
        char *value)
{
        BUF_T *buf = buf_create();
        write_diffusion_string_value(value, buf);

        DIFFUSION_TIME_SERIES_APPEND_PARAMS_T params = {
                .on_append = on_append,
                .on_error = on_error,
                .topic_path = topic_path,
                .datatype = DATATYPE_STRING,
                .value = buf
        };

        /*
         * Append to the time series topic
         */
        diffusion_time_series_append(session, params, NULL);

        // Sleep for a while
        sleep(1);

        buf_free(buf);
}

/*
 * Program entry point.
 */
int main(int argc, char** argv)
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
         * Create a session with the Diffusion server.
         */
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        ADD_TOPIC_CALLBACK_T callback = create_topic_callback(topic_name);

        HASH_T *properties = hash_new(2);
        hash_add(properties, DIFFUSION_TIME_SERIES_EVENT_VALUE_TYPE, "string");

        TOPIC_SPECIFICATION_T *spec = topic_specification_init(TOPIC_TYPE_TIME_SERIES);
        topic_specification_set_properties(spec, properties);

        add_topic_from_specification(session, topic_name, spec, callback);

        // Sleep for a while
        sleep(5);

        topic_specification_free(spec);
        hash_free(properties, NULL, NULL);

        /*
         * Append 3 values to the time series topic
         */
        append_value_to_time_series_topic(session, (char *)topic_name, "hello world!");
        append_value_to_time_series_topic(session, (char *)topic_name, "Diffusion");
        append_value_to_time_series_topic(session, (char *)topic_name, "Push Technology");

        /*
         * Edit the first event in the times series topic
         */
         BUF_T *buf = buf_create();
         write_diffusion_string_value("edited hello world!", buf);

        DIFFUSION_TIME_SERIES_EDIT_PARAMS_T edit_params = {
                .on_edit = on_edit,
                .on_error = on_error_edit,
                .topic_path = topic_name,
                .original_sequence = 0,
                .datatype = DATATYPE_STRING,
                .value = buf
        };
        diffusion_time_series_edit(session, edit_params, NULL);

        // Sleep for a while
        sleep(5);

        buf_free(buf);

        /*
         * Close session and free resources.
         */
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
