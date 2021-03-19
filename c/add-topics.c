/**
 * Copyright © 2014, 2020 Push Technology Ltd.
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
 * This example shows how to connect to Diffusion as a control client and
 * create various topics on the server.
 */

#include <stdio.h>
#include <stdlib.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "args.h"
#include "utils.h"

// Topic selector, selector set delimiter
#define DELIM "////"

static int default_sleep_time = 3;


ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

// Various handlers which are common to all add_topic() functions.
static int
on_topic_added(SESSION_T *session, TOPIC_ADD_RESULT_CODE result_code, void *context)
{
        printf("on_topic_added: %s\n", (const char *)context);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_failed(SESSION_T *session, TOPIC_ADD_FAIL_RESULT_CODE result_code, const DIFFUSION_ERROR_T *error, void *context)
{
        printf("on_topic_add_failed: %s -> %d\n", (const char *)context, result_code);
        return HANDLER_SUCCESS;
}

static int
on_topic_add_discard(SESSION_T *session, void *context)
{
        puts("on_topic_add_discard");
        return HANDLER_SUCCESS;
}

static int
on_topic_removed(SESSION_T *session, const DIFFUSION_TOPIC_REMOVAL_RESULT_T *response, void *context)
{
        int removed_topic_count = diffusion_topic_removal_result_removed_count(response);
        printf("on_topic_removed: removed %d topic(s).\n", removed_topic_count);
        return HANDLER_SUCCESS;
}

static int
on_topic_remove_discard(SESSION_T *session, void *context)
{
        puts("on_topic_remove_discard");
        return HANDLER_SUCCESS;
}

static int
on_topic_view_created(const DIFFUSION_TOPIC_VIEW_T *topic_view, void *context)
{
        char *view_name = diffusion_topic_view_get_name(topic_view);
        char *spec = diffusion_topic_view_get_specification(topic_view);

        printf("Topic view \"%s\" created with specification \"%s\"\n", view_name, spec);
        free(view_name);
        free(spec);

        return HANDLER_SUCCESS;
}

static int
on_error(SESSION_T *session, const DIFFUSION_ERROR_T *error)
{
        printf("Error: %s\n", error->message);

        return HANDLER_SUCCESS;
}

static ADD_TOPIC_CALLBACK_T
create_topic_callback(char *topic_name)
{
        ADD_TOPIC_CALLBACK_T callback = {
                .on_topic_added_with_specification = on_topic_added,
                .on_topic_add_failed_with_specification = on_topic_add_failed,
                .on_discard = on_topic_add_discard,
                .context = topic_name
        };

        return callback;
}

/*
 *
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

        // Setup for session
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
        * Create a JSON topic.
        */
        {
                char *json_topic_name = "json";
                TOPIC_SPECIFICATION_T *json_specification = topic_specification_init(TOPIC_TYPE_JSON);

                add_topic_from_specification(session, json_topic_name, json_specification, create_topic_callback(json_topic_name));
                sleep(default_sleep_time);

                topic_specification_free(json_specification);
        }

        /*
        * Create a topic view which is an alias for the "source"
        * topic.
        */
        {
                char *source_topic_name = "source_topic";
                TOPIC_SPECIFICATION_T *string_specification = topic_specification_init(TOPIC_TYPE_STRING);

                add_topic_from_specification(session, source_topic_name, string_specification, create_topic_callback(source_topic_name));
                sleep(default_sleep_time);

                DIFFUSION_CREATE_TOPIC_VIEW_PARAMS_T topic_view_params = {
                        .view = "view0",
                        .specification = "map source_topic to topic_view",
                        .on_topic_view_created = on_topic_view_created,
                        .on_error = on_error
                };

                diffusion_topic_views_create_topic_view(session, topic_view_params, NULL);
                sleep(default_sleep_time);

                topic_specification_free(string_specification);
        }

        /*
         * This adds a topic with a record containing multiple fields
         * of different types.
         */
        {
                DIFFUSION_RECORDV2_SCHEMA_BUILDER_T *schema_builder = diffusion_recordv2_schema_builder_init();
                diffusion_recordv2_schema_builder_record(schema_builder, "Record1", NULL);
                diffusion_recordv2_schema_builder_string(schema_builder, "Field1", NULL);
                diffusion_recordv2_schema_builder_integer(schema_builder, "Field2", NULL);
                diffusion_recordv2_schema_builder_decimal(schema_builder, "Field3", 2, NULL);

                DIFFUSION_RECORDV2_SCHEMA_T *schema = diffusion_recordv2_schema_builder_build(schema_builder, NULL);
                char *schema_string = diffusion_recordv2_schema_as_json_string(schema);

                HASH_T *properties = hash_new(2);
                hash_add(properties, DIFFUSION_VALIDATE_VALUES, "true");
                hash_add(properties, DIFFUSION_SCHEMA, schema_string);

                char *recordv2_topic_name = "recordv2";
                TOPIC_SPECIFICATION_T *recordv2_specification = topic_specification_init_with_properties(TOPIC_TYPE_RECORDV2, properties);

                add_topic_from_specification(session, recordv2_topic_name, recordv2_specification, create_topic_callback(recordv2_topic_name));
                sleep(default_sleep_time);

                diffusion_recordv2_schema_builder_free(schema_builder);
                diffusion_recordv2_schema_free(schema);
                free(schema_string);

                topic_specification_free(recordv2_specification);
                hash_free(properties, NULL, NULL);
        }

        /*
         * Create a binary topic
         */
        {
                char *binary_topic_name = "binary";
                TOPIC_SPECIFICATION_T *binary_specification = topic_specification_init(TOPIC_TYPE_BINARY);

                add_topic_from_specification(session, binary_topic_name, binary_specification, create_topic_callback(binary_topic_name));
                sleep(default_sleep_time);

                topic_specification_free(binary_specification);
        }

        /*
         * We can also remove topics.
         */
        {
                puts("Removing topics in 5 seconds...");
                sleep(5);

                TOPIC_REMOVAL_PARAMS_T remove_params = {
                        .on_removed = on_topic_removed,
                        .on_discard = on_topic_remove_discard,
                        .topic_selector = "#json" DELIM "topic_view" DELIM "recordv2" DELIM "binary"
                };

                topic_removal(session, remove_params);
                sleep(default_sleep_time);
        }

        /*
         * Close our session, and release resources and memory.
         */
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
