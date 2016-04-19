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
 * This is a sample client which connects to Diffusion v5 and demonstrates
 * the following features:
 *
 * 1. Fetch topic state using a user-specified topic selector.
 * 2. Connect to Diffusion with a username and password.
 * 3. Automatic retry of a connection if unable to connect at the first
 *    attempt.
 */

#include <stdio.h>
#include <unistd.h>

#include "diffusion.h"
#include "args.h"

extern void topic_message_debug();

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "dpt://localhost:8080"},
        {'t', "topic_selector", "Topic selector", ARG_REQUIRED, ARG_HAS_VALUE, NULL},
        {'r', "retries", "Number of connection retries", ARG_OPTIONAL, ARG_HAS_VALUE, "3"},
        {'d', "retry_delay", "Delay (in ms) between connection attempts", ARG_OPTIONAL, ARG_HAS_VALUE, "1000"},
	{'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
	{'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
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

/**
 * This callback is invoked when Diffusion acknowledges that it has received
 * the fetch request. It does not indicate that there will be any subsequent
 * messages; see on_topic_message() and on_fetch_status_message() for that.
 */
static int
on_fetch(SESSION_T *session, void *context)
{
	puts("Fetch acknowledged by server");
	return HANDLER_SUCCESS;
}

/**
 * This callback is invoked when all messages for a topic selector have
 * been received, or there was some kind of server-side error during the
 * fetch processing.
 */
static int
on_fetch_status_message(SESSION_T *session,
                        const SVC_FETCH_STATUS_RESPONSE_T *status,
                        void *context)
{
	switch(status->status_flag) {
	case DIFFUSION_TRUE:
		puts("Fetch succeeded");
		break; //exit(0);
	case DIFFUSION_FALSE:
		puts("Fetch failed");
		break; //exit(1);
	default:
		printf("Unknown fetch status: %d\n", status->status_flag);
		break;
	}

	return HANDLER_SUCCESS;
}

/**
 * When a fetched message is received, this callback in invoked.
 */
static int
on_topic_message(SESSION_T *session, const TOPIC_MESSAGE_T *msg)
{
	printf("Received message for topic %s\n", msg->name);
        printf("Payload: %.*s\n", (int)msg->payload->len, msg->payload->data);

#ifdef DEBUG
	        topic_message_debug(response->payload);
#endif

	return HANDLER_SUCCESS;
}

int
main(int argc, char **argv)
{
	// Standard command line parsing.
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return 1;
        }

        char *url = hash_get(options, "url");
        char *topic = hash_get(options, "topic_selector");
        int retries = atoi(hash_get(options, "retries"));
        long retry_delay = atol(hash_get(options, "retry_delay"));

	// A SESSION_LISTENER_T holds callbacks to inform the client
        // about changes to the state. Used here for informational
        // purposes only.
        SESSION_LISTENER_T foo_listener = {
                foo_listener.on_state_changed = &on_session_state_changed
        };

	// The client-side API can automatically keep retrying to connect
	// to the Diffusion server if it's not immediately available.
        SESSION_FAILOVER_STRATEGY_T failover_strategy = {
                failover_strategy.retry_count = retries,
                failover_strategy.retry_delay = retry_delay
        };

	// Creating a session requires at least a URL. Creating a session
        // initiates a connection with Diffusion.
        SESSION_T *session;
        DIFFUSION_ERROR_T error;
        session = session_create(url,
                                 hash_get(options, "principal"),
                                 credentials_create_password(hash_get(options, "credentials")),
                                 &foo_listener, &failover_strategy, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return 1;
        }

	// Register handlers for callbacks we're interested in relating to
	// the fetch request. In particular, we want to know about the topic
	// messages that are returned, and the status message which tells
	// us when all messages have been received for the selector (or, if
	// something went wrong.)
        FETCH_PARAMS_T params = {
                .selector = topic,
                .on_topic_message = on_topic_message,
                .on_fetch = on_fetch,
                .on_status_message = on_fetch_status_message
        };

	// Issue the fetch request.
	fetch(session, params);

	// Wait for up to 5 seconds for the results to come in.
        sleep(5);

	// Clean up politely.
        session_close(session, &error);

        return 0;
}
