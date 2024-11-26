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

static int on_listener_registered(SESSION_T *session, void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_registration_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_session_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    return HANDLER_SUCCESS;
}

static int on_session_open(
    SESSION_T *session,
    const SESSION_PROPERTIES_EVENT_T *request,
    void *context)
{
    char *session_id = session_id_to_string(&request->session_id);
    printf("Session %s is now open with the following properties:\n", session_id);
    utils_print_hash(
        request->properties, NULL, utils_print_string
    );

    free(session_id);
    return HANDLER_SUCCESS;
}

static int on_session_update(
    SESSION_T *session,
    const SESSION_PROPERTIES_EVENT_T *request,
    void *context)
{
    HASH_NUM_T *update_map = hash_num_new(10);
    hash_num_add(update_map, SESSION_PROPERTIES_UPDATE_TYPE_UPDATED, "Properties Update");
    hash_num_add(update_map, SESSION_PROPERTIES_UPDATE_TYPE_RECONNECTED, "Reconnected");
    hash_num_add(update_map, SESSION_PROPERTIES_UPDATE_TYPE_FAILED_OVER, "Failed Over");
    hash_num_add(update_map, SESSION_PROPERTIES_UPDATE_TYPE_DISCONNECTED, "Disconnected");

    char *update_type = hash_num_get(update_map, request->update_type);
    char *session_id = session_id_to_string(&request->session_id);
    printf("Session %s has been updated (%s) with the following properties:\n", session_id, update_type);
    utils_print_hash(
        request->properties, NULL, utils_print_string
    );

    free(session_id);
    hash_num_free(update_map, NULL);
    return HANDLER_SUCCESS;
}

static int on_session_close(
    SESSION_T *session,
    const SESSION_PROPERTIES_EVENT_T *request,
    void *context)
{
    HASH_NUM_T *close_reason_map = hash_num_new(10);
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_CONNECTION_LOST, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_IO_EXCEPTION, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_CLIENT_UNRESPONSIVE, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_MESSAGE_QUEUE_LIMIT_REACHED, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_CLOSED_BY_CLIENT, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_MESSAGE_TOO_LARGE, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_INTERNAL_ERROR, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_INVALID_INBOUND_MESSAGE, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_INVALID_INBOUND_MESSAGE, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_ABORTED, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_LOST_MESSAGES, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_SERVER_CLOSING, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_CLOSED_BY_CONTROLLER, "Connection Lost");
    hash_num_add(close_reason_map, SESSION_CLOSE_REASON_FAILED_OVER, "Connection Lost");

    char *close_reason = hash_num_get(close_reason_map, request->close_reason);
    char *session_id = session_id_to_string(&request->session_id);
    printf("Session %s has been closed (%s) with the following properties:\n", session_id, close_reason);
    utils_print_hash(
        request->properties, NULL, utils_print_string
    );

    free(session_id);
    hash_num_free(close_reason_map, NULL);
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT

    SESSION_T *session = utils_open_session(url, "admin", "password");

    SET_T *required_properties = set_new_string(5);
    set_add(required_properties, PROPERTIES_SELECTOR_ALL_FIXED_PROPERTIES);
    set_add(required_properties, PROPERTIES_SELECTOR_ALL_USER_PROPERTIES);

    SESSION_PROPERTIES_REGISTRATION_PARAMS_T params = {
        .on_registered = on_listener_registered,
        .on_registration_error = on_registration_error,
        .on_session_open = on_session_open,
        .on_session_close = on_session_close,
        .on_session_error = on_session_error,
        .on_session_update = on_session_update,
        .required_properties = required_properties
    };
    session_properties_listener_register(session, params);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);


    set_free(required_properties);
    MUTEX_TERMINATE
}