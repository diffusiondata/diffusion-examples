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
 * @author Push Technology Limited
 * @since 6.2
 */

/*
 * This example creates a String topic and periodically updates
 * the data it contains.
 *
 * In order to perform the update, a constraint is evaluated - in this
 * example, the constraint is an acquired session lock.
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


DIFFUSION_SESSION_LOCK_T *g_session_lock = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        {'t', "topic", "Topic name to create and update", ARG_OPTIONAL, ARG_HAS_VALUE, "time"},
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


static int on_topic_update(void *context)
{
        printf("topic update success\n");
        return HANDLER_SUCCESS;
}


static int on_error(
        SESSION_T *session,
        const DIFFUSION_ERROR_T *error)
{
        printf("topic update error: %s\n", error->message);
        return HANDLER_SUCCESS;
}


static int on_lock_acquired(
        const DIFFUSION_SESSION_LOCK_T *session_lock,
        void *context)
{
        char *lock_name = diffusion_session_lock_get_name(session_lock);
        printf("session lock acquired: %s\n", lock_name);
        free(lock_name);

        g_session_lock = diffusion_session_lock_dup(session_lock);

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

        add_topic_from_specification(session, topic_name, spec, callback);

        // Sleep for a while
        sleep(5);

        topic_specification_free(spec);

        DIFFUSION_SESSION_LOCK_PARAMS_T lock_params = {
                .on_lock_acquired = on_lock_acquired
        };

        // Acquire the session lock.
        diffusion_session_lock(session, "topic-update-lock", lock_params);

        // Sleep for a while
        sleep(5);

        // Create the session lock topic update constraint.
        DIFFUSION_TOPIC_UPDATE_CONSTRAINT_T *session_lock_constraint =
                diffusion_topic_update_constraint_locked(g_session_lock);

        time_t end_time = time(NULL) + seconds;

        while(time(NULL) < end_time) {
                // Compose the update content.
                const time_t time_now = time(NULL);
                const char *time_str = ctime(&time_now);

                // Create a BUF_T and write the string datatype value into it.
                BUF_T *update_buf = buf_create();
                write_diffusion_string_value(time_str, update_buf);

                DIFFUSION_TOPIC_UPDATE_SET_PARAMS_T topic_update_params = {
                        .topic_path = topic_name,
                        .datatype = DATATYPE_STRING,
                        .update = update_buf,
                        .on_topic_update = on_topic_update,
                        .on_error = on_error
                };

                // Update the topic with the constraint to be evaluated.
                diffusion_topic_update_set_with_constraint(
                        session,
                        session_lock_constraint,
                        topic_update_params);
                buf_free(update_buf);

                sleep(1);
        }

        // Close session and free resources.
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        diffusion_topic_update_constraint_free(session_lock_constraint);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
