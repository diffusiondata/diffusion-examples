/**
 * Copyright © 2014, 2015 Push Technology Ltd.
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
 * @author Push Technology Limited
 * @since 5.0
 */

/*
 * This is an application which connects to Diffusion, and provides state
 * for a particular topic.
 * 
 * The topic must exist within Diffusion, and be of simple type (ie, does not
 * contain TopicData).
 */

#include <stdio.h>
#include <unistd.h>

#include "diffusion.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "dpt://localhost:8080"}, 
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'t', "topic", "Topic", ARG_OPTIONAL, ARG_HAS_VALUE, "foo"},
        END_OF_ARG_OPTS
};

/**
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

static int
topic_control_registration_handler(SESSION_T *session, const char *path, void *context)
{
        printf("Registered handler for topic path %s\n", path);
        return HANDLER_SUCCESS;
}

/**
 * When a request for topic state is received, this handler is called.
 * 
 * The topic name for which state is being requested is available in the
 * request parameter, but here we are just responding with a hardcoded string,
 * regardless of the request.
 * 
 * Note that the response data is written into the buffer of response parameter,
 * and the handler itself returns HANDLER_SUCCESS if the request is handled
 * successfully.
 */
static int
topic_state_handler(SESSION_T *session, const SVC_STATE_REQUEST_T *request, SVC_STATE_RESPONSE_T *response, void *context)
{
        printf("Responding with state for topic path %s\n", request->topic_path);
	buf_write_bytes(response->payload, "Hello, world!!", 14);
	
	return HANDLER_SUCCESS;
}

int
main(int argc, char **argv)
{
	// Standard command line parsing
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return 1;
        }

        char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }
        char *topic = hash_get(options, "topic");

	// A SESSION_LISTENER_T holds callbacks to inform the client
        // about changes to the state. Used here for informational
        // purposes only.
        SESSION_LISTENER_T state_listener;
        state_listener.on_state_changed = &on_session_state_changed;

	// Creating a session requires at least a URL. Creating a session
        // initiates a connection with Diffusion.
        DIFFUSION_ERROR_T error;
        SESSION_T *session;
        session = session_create(url, principal, credentials, &state_listener, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return 1;
        }

        // Add the topic we're going to be providing state for.
        ADD_TOPIC_PARAMS_T add_params = {
                .topic_path = topic,
                .details = create_topic_details_stateless()
        };
        add_topic(session, add_params);
        
        // Register a handler for the named topic, so that requests for that
	// topic's state are routed to this handler by Diffusion.
	STATE_HANDLERS_T *handlers = calloc(1, sizeof(STATE_HANDLERS_T));
        handlers->on_topic_control_registration = topic_control_registration_handler;
	handlers->on_state_provider = topic_state_handler;

        STATE_PARAMS_T params = {
                .on_topic_control_registration = topic_control_registration_handler,
                .on_state_provider = topic_state_handler,
                .topic_path = topic
        };
        register_state_provider(session, params);
        
	// Provide state forever.
	while(1) {
		sleep(120);
	}

	// Not called, but this is how we would close the connection with
	// Diffusion gracefully.
        session_close(session, &error);

        return 0;
}
