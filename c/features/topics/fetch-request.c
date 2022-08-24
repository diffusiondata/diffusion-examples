/**
 * Copyright © 2019, 2021 Push Technology Ltd.
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
 * @since 6.4
 */

/*
 * This is a sample client which connects to Diffusion and demonstrates
 * the following features:
 *
 * 1. Connect to Diffusion with a username and password.
 * 2. Fetch topic state using a user-specified topic path.
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
        {'t', "topic_path", "Topic path", ARG_REQUIRED, ARG_HAS_VALUE, NULL},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "client"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

/*
 * This callback is used when the session state changes, e.g. when a session
 * moves from a "connecting" to a "connected" state, or from "connected" to
 * "closed".
 */
static void
on_session_state_changed(SESSION_T *session, const SESSION_STATE_T old_state, const SESSION_STATE_T new_state)
{
        printf("Session state changed from %s (%d) to %s (%d)\n",
               session_state_as_string(old_state), old_state,
               session_state_as_string(new_state), new_state);
        if(new_state == CONNECTED_ACTIVE) {
                char *session_id = session_id_to_string(session->id);
                printf("Session ID=%s\n", session_id);
                free(session_id);
        }
}

static int
on_fetch_result(const DIFFUSION_FETCH_RESULT_T *fetch_result, void *context)
{
        LIST_T *results = diffusion_fetch_result_get_topic_results(fetch_result);

        DIFFUSION_TOPIC_RESULT_T *topic_result = list_get_data_indexed(results, 0);
        DIFFUSION_VALUE_T *value = diffusion_topic_result_get_value(topic_result);

        char *topic_path = diffusion_topic_result_get_path(topic_result);
        printf("Fetching value from \"%s\"\n", topic_path);

        if(value != NULL) {
                switch(diffusion_topic_result_get_topic_type(topic_result)) {

                char *json_value;
                int64_t int64_value;
                void *binary_value;
                double double_value;
                char *string_value;
                char *recordv2_value;

                case TOPIC_TYPE_JSON:
                        to_diffusion_json_string(value, &json_value, NULL);
                        printf("JSON topic type, fetch value: %s\n", json_value);
                        free(json_value);
                        break;
                case TOPIC_TYPE_INT64:
                        read_diffusion_int64_value(value, &int64_value, NULL);
                        printf("Int64 topic type, fetch value: " "%"PRId64 "\n", int64_value);
                        break;
                case TOPIC_TYPE_BINARY:
                        read_diffusion_binary_value(value, &binary_value, NULL);
                        printf("Binary topic type, fetch value: %s\n", (char *)binary_value);
                        free(binary_value);
                        break;
                case TOPIC_TYPE_DOUBLE:
                        read_diffusion_double_value(value, &double_value, NULL);
                        printf("Double topic type, fetch value: %f\n", double_value);
                        break;
                case TOPIC_TYPE_STRING:
                        read_diffusion_string_value(value, &string_value, NULL);
                        printf("String topic type, fetch value: %s\n", string_value);
                        free(string_value);
                        break;
                case TOPIC_TYPE_RECORDV2:
                        diffusion_recordv2_to_string(value, &recordv2_value, NULL);
                        printf("RecordV2 topic type, fetch value: %s\n", recordv2_value);
                        free(recordv2_value);
                        break;
                default:
                        break;
                }
        }
        else {
                printf("No fetch value\n");
        }

        list_free(results, (void (*)(void *))diffusion_topic_result_free);
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
        char *topic = hash_get(options, "topic_path");
        const char *principal = hash_get(options, "principal");

        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        /*
         * A SESSION_LISTENER_T holds callbacks to inform the client
         * about changes to the state. Used here for informational
         * purposes only.
         */
        SESSION_LISTENER_T session_listener = {
                .on_state_changed = &on_session_state_changed
        };

        /*
         * Creating a session requires at least a URL. Creating a session
         * initiates a connection with Diffusion.
         */
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, &session_listener, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * Create the fetch request
         */
        DIFFUSION_FETCH_REQUEST_T *fetch_request = diffusion_fetch_request_init(session);
        diffusion_fetch_request_with_values(fetch_request, NULL, NULL);
        diffusion_fetch_request_from(fetch_request, topic, NULL);
        diffusion_fetch_request_to(fetch_request, topic, NULL);
        diffusion_fetch_request_first(fetch_request, 1, NULL);
        diffusion_fetch_request_maximum_result_size(fetch_request, 1000, NULL);
        diffusion_fetch_request_limit_deep_branches(fetch_request, 3 ,3, NULL); // this limits results to a max depth of 3, with each result having maximum 3 results.

        DIFFUSION_FETCH_REQUEST_PARAMS_T params = {
                .topic_selector = topic,
                .fetch_request = fetch_request,
                .on_fetch_result = on_fetch_result
        };

        /*
         * Issue the fetch request.
         */
        diffusion_fetch_request_fetch(session, params);

        /*
         * Wait for 5 seconds for the results to come in.
         */
        sleep(5);

        /*
         * Clean up.
         */
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);
        diffusion_fetch_request_free(fetch_request);

        return EXIT_SUCCESS;
}
