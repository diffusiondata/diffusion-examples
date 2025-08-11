/**
 * Copyright © 2024 DiffusionData Ltd.
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
 */

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "diffusion.h"
#include "utils.h"
MUTEX_DEF

SESSION_T *g_control_session;

static int on_listener_registered(SESSION_T *session, void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_complete(SESSION_T *session, void *context)
{
    return HANDLER_SUCCESS;
}

static int on_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    printf("On error: %s\n", error->message);
    return HANDLER_SUCCESS;
}

static int on_session_open(
    SESSION_T *session,
    const SESSION_PROPERTIES_EVENT_T *request,
    void *context)
{
    char *principal = hash_get(request->properties, "$Principal");

    if (strcmp(principal, "client") != 0) {
        // Not the right type of principal, nothing to do.
        return HANDLER_SUCCESS;
    }

    SUBSCRIPTION_CONTROL_PARAMS_T params = {
        .on_complete = on_complete,
        .on_error = on_error,
        .session_id = request->session_id,
        .topic_selector = "?my/topic/path//"
    };
    subscribe_client(session, params);

    // Sleep for a bit to see the notifications
    sleep(5);

    unsubscribe_client(session, params);
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT

    g_control_session = utils_open_session(url, "admin", "password");

    utils_create_string_topic(
        g_control_session, "my/topic/path/hello", "Hello World!"
    );

    SET_T *required_properties = set_new_string(1);
    set_add(required_properties, PROPERTIES_SELECTOR_ALL_FIXED_PROPERTIES);

    SESSION_PROPERTIES_REGISTRATION_PARAMS_T params = {
        .on_registered = on_listener_registered,
        .on_session_open = on_session_open,
        .required_properties = required_properties
    };
    session_properties_listener_register(g_control_session, params);
    MUTEX_WAIT

    SESSION_T *session = utils_open_session(url, "client", "password");

    VALUE_STREAM_T *value_stream_ptr =
        utils_create_value_stream(session, "?.*//", DATATYPE_STRING);

    // Sleep for a bit to see the notifications
    sleep(10);

    session_close(g_control_session, NULL);
    session_free(g_control_session);

    session_close(session, NULL);
    session_free(session);

    set_free(required_properties);
    free(value_stream_ptr);
    MUTEX_TERMINATE
}