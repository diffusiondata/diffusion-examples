/**
 * Copyright © 2016, 2021 Push Technology Ltd.
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
 * @since 5.7
 */

/*
 * This example shows how to register a missing topic notification handler.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "apr.h"
#include "apr_thread_mutex.h"
#include "apr_thread_cond.h"

#include "diffusion.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'r', "topic_root", "Topic root to process missing topic notifications on", ARG_OPTIONAL, ARG_HAS_VALUE, "foo"},
        END_OF_ARG_OPTS
};


/*
 * Handlers for add_topic_from_specification() function.
 */
static int on_topic_added_with_specification(
        SESSION_T *session,
        TOPIC_ADD_RESULT_CODE result_code,
        void *context)
{
        puts("Topic added");
        return HANDLER_SUCCESS;
}


static int on_topic_add_failed_with_specification(
        SESSION_T *session,
        TOPIC_ADD_FAIL_RESULT_CODE result_code,
        const DIFFUSION_ERROR_T *error,
        void *context)
{
        puts("Topic add failed");
        printf("Reason code: %d\n", result_code);
        return HANDLER_SUCCESS;
}


static int on_topic_add_discard(
        SESSION_T *session,
        void *context)
{
        puts("Topic add discarded");
        return HANDLER_SUCCESS;
}


static ADD_TOPIC_CALLBACK_T create_topic_callback()
{
        ADD_TOPIC_CALLBACK_T callback = {
                .on_topic_added_with_specification = on_topic_added_with_specification,
                .on_topic_add_failed_with_specification = on_topic_add_failed_with_specification,
                .on_discard = on_topic_add_discard
        };

        return callback;
}


/*
 * A request has been made for a topic that doesn't exist.
 * This handler will create the missing topic.
 */
static int on_missing_topic(
        SESSION_T *session,
        const SVC_MISSING_TOPIC_REQUEST_T *request,
        void *context)
{
        printf("Missing topic: %s\n", request->topic_selector);

        ADD_TOPIC_CALLBACK_T callback = create_topic_callback();
        TOPIC_SPECIFICATION_T *spec = topic_specification_init(TOPIC_TYPE_JSON);

        add_topic_from_specification(session, request->topic_selector + 1, spec, callback);
        topic_specification_free(spec);

        return HANDLER_SUCCESS;
}


/*
 * Entry point for the example.
 */
int main(int argc, char **argv)
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
        const char *topic_root = hash_get(options, "topic_root");

        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };

        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session != NULL) {
                char *session_id = session_id_to_string(session->id);
                printf("Session created (state=%d, id=%s)\n",
                       session_state_get(session), session_id);
                free(session_id);
        }
        else {
                printf("Failed to create session: %s\n", error.message);
                free(error.message);
                return EXIT_FAILURE;
        }

        /*
         * Register the missing topic handler
         */
        MISSING_TOPIC_PARAMS_T handler = {
                .on_missing_topic = on_missing_topic,
                .topic_path = topic_root,
                .context = NULL
        };

        missing_topic_register_handler(session, handler);

        /*
         * Run for 5 minutes.
         */
        sleep(5 * 60);

        /*
         * Close session and clean up.
         */
        session_close(session, NULL);
        session_free(session);

        hash_free(options, NULL, free);
        credentials_free(credentials);

        return EXIT_SUCCESS;
}
