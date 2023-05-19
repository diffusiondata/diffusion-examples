/**
 * Copyright © 2021 Push Technology Ltd.
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
 * @since 6.7
 */

/*
 * This example creates, lists, checks and removes a remote server.
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

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "admin"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};


static char *get_connection_option_string(
        DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_T option)
{
        switch(option) {
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RECONNECTION_TIMEOUT:
                        return "reconnection_timeout";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RETRY_DELAY:
                        return "retry_delay";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RECOVERY_BUFFER_SIZE:
                        return "recovery_buffer_size";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_INPUT_BUFFER_SIZE:
                        return "input_buffer_size";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_OUTPUT_BUFFER_SIZE:
                        return "output_buffer_size";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_MAXIMUM_QUEUE_SIZE:
                        return "maximum_queue_size";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_CONNECTION_TIMEOUT:
                        return "connection_timeout";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_WRITE_TIMEOUT:
                        return "write_timeout";
                default:
                        return "unknown";
        }
}


static void print_connection_options(
        HASH_NUM_T *connection_options)
{
        unsigned long *keys = hash_num_keys(connection_options);
        for (unsigned long i = 0; i < connection_options->size; i++) {
                char *val = (char *)hash_num_get(connection_options, keys[i]);
                printf("\t%s: %s\n", get_connection_option_string(keys[i]), val);
        }
        free(keys);
}


static void print_remote_server(
        DIFFUSION_REMOTE_SERVER_T *remote_server)
{
        char *name = diffusion_remote_server_get_name(remote_server);
        char *principal = diffusion_remote_server_get_principal(remote_server);
        char *url = diffusion_remote_server_get_url(remote_server);
        char *missing_topic_notification_filter =
                diffusion_remote_server_get_missing_topic_notification_filter(remote_server);
        HASH_NUM_T *connection_options = diffusion_remote_server_get_connection_options(remote_server);

        printf("Name: %s\n", name);
        printf("URL: %s\n", url);
        printf("Principal: %s\n", principal);
        printf("Missing Topic Notification Filter: %s\n", missing_topic_notification_filter);
        printf("Connection Options:\n");
        print_connection_options(connection_options);

        free(name);
        free(principal);
        free(url);
        free(missing_topic_notification_filter);
        hash_num_free(connection_options, free);
}


static int on_remote_server_created(
        DIFFUSION_REMOTE_SERVER_T *remote_server,
        LIST_T *errors,
        void *context)
{
        if (remote_server == NULL) {
                printf("The following errors occurred while creating the remote server:\n");
                for (int i = 0; i < list_get_size(errors); i++) {
                    ERROR_REPORT_T *report = list_get_data_indexed(errors, i);
                    printf("\t[%d, %d] %s\n", report->line, report->column, report->message);
                }
        }
        else {
                printf("Remote Server successfully created\n");
                print_remote_server(remote_server);
        }
        return HANDLER_SUCCESS;
}


static int on_remote_servers_listed(
        LIST_T *remote_servers,
        void *context)
{
        int list_size = list_get_size(remote_servers);
        printf("Remote Servers found: %d\n", list_size);
        for (int i = 0; i < list_size; i++) {
            DIFFUSION_REMOTE_SERVER_T *remote_server = list_get_data_indexed(remote_servers, i);
            print_remote_server(remote_server);
            printf("\n");
        }
        return HANDLER_SUCCESS;
}


static char *get_server_state_string(
        DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_T state)
{
        switch(state) {
                case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_INACTIVE:
                        return "inactive";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_CONNECTED:
                        return "connected";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_RETRYING:
                        return "retrying";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_FAILED:
                        return "failed";
                case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_MISSING:
                        return "missing";
                default:
                        return "unknown";
        }
}


static int on_remote_server_checked(
        DIFFUSION_CHECK_REMOTE_SERVER_RESPONSE_T *response,
        void *context)
{
        DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_T state =
                diffusion_check_remote_server_response_get_state(response);

        printf("Received remote server status: %s.\n", get_server_state_string(state));
        if (state == DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_FAILED) {
                char *failure_message =
                        diffusion_check_remote_server_response_get_failure_message(response);
                printf("Failure message: %s\n", failure_message);
                free(failure_message);
        }
        return HANDLER_SUCCESS;
}


static int on_remote_server_removed(void *context)
{
        printf("Remote server has been successfully removed.\n");
        return HANDLER_SUCCESS;
}


static int on_error(
        SESSION_T *session,
        const DIFFUSION_ERROR_T *error)
{
        printf("Error: %s\n", error->message);
        return HANDLER_SUCCESS;
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

        /*
         * Create a remote server, using its builder
         */
        DIFFUSION_REMOTE_SERVER_BUILDER_T *builder =
                diffusion_remote_server_builder_init();

        builder = diffusion_remote_server_builder_principal(
                builder,
                "admin");
        builder = diffusion_remote_server_builder_missing_topic_notification_filter(
                builder,
                "*/A/B/C/D//");
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RECONNECTION_TIMEOUT,
                "120000"); // milliseconds
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RETRY_DELAY,
                "2000"); // milliseconds
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RECOVERY_BUFFER_SIZE,
                "5000");
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_INPUT_BUFFER_SIZE,
                "1024"); // kilobytes
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_OUTPUT_BUFFER_SIZE,
                "2048"); // kilobytes
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_MAXIMUM_QUEUE_SIZE,
                "7500");
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_CONNECTION_TIMEOUT,
                "120000"); // milliseconds
        builder = diffusion_remote_server_builder_connection_option(
                builder,
                DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_WRITE_TIMEOUT,
                "300000"); // milliseconds

        CREDENTIALS_T *remote_server_credentials = credentials_create_password("password");
        builder = diffusion_remote_server_builder_credentials(
                builder,
                remote_server_credentials);

        DIFFUSION_API_ERROR api_error = { 0 };
        DIFFUSION_REMOTE_SERVER_T *remote_server =
                diffusion_remote_server_builder_create(
                        builder,
                        "remote server 1",          // remote server name
                        "ws://localhost:9091",      // remote server URL
                        &api_error);                // api error in case of invalid parameters

        /*
         * Create the remote server definition in the Diffusion server
         */
        DIFFUSION_CREATE_REMOTE_SERVER_PARAMS_T create_remote_server_params = {
                .remote_server = remote_server,
                .on_remote_server_created = on_remote_server_created,
                .on_error = on_error
        };
        diffusion_create_remote_server(session, create_remote_server_params, NULL);
        sleep(2);

        /*
         * List all remote servers defined in the Diffusion server
         */
        DIFFUSION_LIST_REMOTE_SERVERS_PARAMS_T list_remote_servers_params = {
                .on_remote_servers_listed = on_remote_servers_listed,
                .on_error = on_error
        };
        diffusion_list_remote_servers(session, list_remote_servers_params, NULL);
        sleep(2);

        /*
         * Check remote server we created.
         */
        DIFFUSION_CHECK_REMOTE_SERVER_PARAMS_T check_remote_server_params = {
                .name = "remote server 1",
                .on_remote_server_checked = on_remote_server_checked,
                .on_error = on_error
        };
        diffusion_check_remote_server(session, check_remote_server_params, NULL);
        sleep(2);

        /*
         * Remove remote server we created.
         */
        DIFFUSION_REMOVE_REMOTE_SERVER_PARAMS_T remove_remote_server_params = {
                .name = "remote server 1",
                .on_remote_server_removed = on_remote_server_removed,
                .on_error = on_error
        };
        diffusion_remove_remote_server(session, remove_remote_server_params, NULL);
        sleep(2);

        /*
         * Close session and free resources.
         */
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        credentials_free(remote_server_credentials);
        hash_free(options, NULL, free);
        diffusion_remote_server_free(remote_server);
        diffusion_remote_server_builder_free(builder);

        return EXIT_SUCCESS;
}
