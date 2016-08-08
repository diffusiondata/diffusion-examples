/**
 * Copyright © 2014, 2016 Push Technology Ltd.
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
 * This is an example of a state provider. It reads a BSD-style fortune file,
 * and every time the topic state is requested, a new fortune is returned
 * at random. Otherwise, it is largely the same as rc-state.c
 *
 * A client program, "fortune-client" can be used in conjunction with this
 * to issue the fetch request and display the fortune.
 */

#include <stdio.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/timeb.h>

#include "diffusion.h"
#include "args.h"

static char **fortunes = NULL;
static int fortune_count = 0;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'t', "topic", "Topic on which to supply fortunes", ARG_OPTIONAL, ARG_HAS_VALUE, "fortune"},
        {'f', "fortune_file", "File containing fortunes", ARG_OPTIONAL, ARG_HAS_VALUE, "/usr/share/fortune/fortunes"},
        END_OF_ARG_OPTS
};

/*
 * Reads a fortune file into an array of char *, so we can easily select an
 * individual fortune when asked to do so.
 */
static void
read_fortunes(const char *path)
{
        FILE *fp = fopen(path, "r");
        if(fp == NULL) {
                perror("Unable to open fortune file");
                return;
        }

        char linebuf[BUFSIZ];

        char *fortune = NULL;
        long len = 0;
        while((fgets(linebuf, sizeof(linebuf), fp)) != NULL) {
                linebuf[strlen(linebuf)-1] = ' '; // newlines to spaces
                if(linebuf[0] == '%') {
                        if(fortune != NULL) {
                                fortune[len-2] = '\0'; // Removes trailing space (added above)

                                fortunes = realloc(fortunes, (fortune_count+1)*sizeof(char *));

                                fortunes[fortune_count] = strdup(fortune);
                                fortune_count++;

                                free(fortune);
                                fortune = NULL;
                                len = 0;
                        }
                        continue;
                }

                len += strlen(linebuf) + 1;

                // Ensure fortune is NULL terminated
                int f_len = (fortune == NULL) ? 0 : strlen(fortune);
                fortune = realloc(fortune, len);
                fortune[f_len] = '\0';

                strcat(fortune, linebuf);
        }

        free(fortune);
        fclose(fp);

        printf("Loaded %d fortunes\n", fortune_count);
}

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
                printf("Session ID=%s\n", session_id_to_string(session->id));
        }
}

/*
 * This will be called when the state provider is registered with Diffusion.
 */
static int
on_registration(SESSION_T *session, const char *path, void *context)
{
        printf("Registered\n");
        return HANDLER_SUCCESS;
}

/*
 * This is the main handler which is called when a request for state is
 * received from Diffusion.
 *
 * Choose a random fortune and write it to the response parameter.
 */
static int
fortune_state_handler(SESSION_T *session, const SVC_STATE_REQUEST_T *request, SVC_STATE_RESPONSE_T *response, void *context)
{
        long r = random() % fortune_count;
        char *fortune = fortunes[r];

        printf("fortune_state_handler(): %s\n", fortune);

        buf_write_bytes(response->payload, fortune, strlen(fortune));

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

        read_fortunes(hash_get(options, "fortune_file"));

        /*
         * Seed the random number generator.
         */
        struct timeb t;
        ftime(&t);
        srandom((t.time * 1000 + t.millitm));

        SESSION_T *session = NULL;

        char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }
        char *topic = hash_get(options, "topic");

        /*
         * A SESSION_LISTENER_T holds callbacks to inform the client
         * about changes to the state. Used here for informational
         * purposes only.
         */
        SESSION_LISTENER_T session_listener = { 0 };
        session_listener.on_state_changed = &on_session_state_changed;

        /*
         * Create a session with Diffusion.
         */
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, &session_listener, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * Add the "fortune" topic.
         */
        TOPIC_DETAILS_T *details = create_topic_details_stateless();
        add_topic(session, (ADD_TOPIC_PARAMS_T) { .topic_path = "fortune", .details = details });

        /*
         * Register a handler for the named topic, so that requests
         * for that topic's state are routed to this handler by
         * Diffusion.
         */
        STATE_PARAMS_T state_params = {
                .topic_path = topic,
                .on_topic_control_registration = on_registration,
                .on_state_provider = fortune_state_handler
        };

        register_state_provider(session, state_params);

        // Never exit.
        while(1) {
                sleep(10);
        }

        /*
         * Not called, but this is how we would gracefully close the
         * connection with Diffusion.
         */
        session_close(session, NULL);
        session_free(session);

        return EXIT_SUCCESS;
}
