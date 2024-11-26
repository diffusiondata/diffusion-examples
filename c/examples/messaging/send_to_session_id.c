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

static int on_active(
    SESSION_T *session,
    const char *path,
    const DIFFUSION_REGISTRATION_T *registered_handler)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_request (
    SESSION_T *session,
    const char *request_path,
    DIFFUSION_DATATYPE request_datatype,
    const DIFFUSION_VALUE_T *request,
    const DIFFUSION_RESPONDER_HANDLE_T *handle,
    void *context)
{
    char *request_string_value;
    read_diffusion_string_value(request, &request_string_value, NULL);
    printf("Received message: %s\n", request_string_value);
    free(request_string_value);

    BUF_T *response = buf_create();
    write_diffusion_string_value("Goodbye.", response);
    diffusion_respond_to_request(session, handle, response, NULL);
    buf_free(response);
    return HANDLER_SUCCESS;
}

static int on_request_unexpected(
    SESSION_T *session,
    const char *request_path,
    DIFFUSION_DATATYPE request_datatype,
    const DIFFUSION_VALUE_T *request,
    const DIFFUSION_RESPONDER_HANDLE_T *handle,
    void *context)
{
    char *request_string_value;
    read_diffusion_string_value(request, &request_string_value, NULL);
    printf("Received message: %s\n", request_string_value);
    free(request_string_value);

    BUF_T *response = buf_create();
    write_diffusion_string_value("I'm not supposed to receive a message.", response);
    diffusion_respond_to_request(session, handle, response, NULL);
    buf_free(response);
    return HANDLER_SUCCESS;
}

static int on_response(
    DIFFUSION_DATATYPE response_datatype,
    const DIFFUSION_VALUE_T *response,
    void *context)
{
    char *response_string_value;
    read_diffusion_string_value(response, &response_string_value, NULL);
    printf("Received response: %s\n", response_string_value);
    free(response_string_value);
    MUTEX_BROADCAST

    return HANDLER_SUCCESS;
}

void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    SESSION_T *receiving_session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );
    SESSION_ID_T *receiving_session_id = receiving_session->id;

    const char *message_path = "my/message/path";

    DIFFUSION_REQUEST_STREAM_T receiving_request_stream = {
        .on_request = on_request
    };
    set_request_stream(
        receiving_session,
        message_path,
        DATATYPE_STRING,
        DATATYPE_STRING,
        &receiving_request_stream
    );

    SESSION_T *non_receiving_session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    DIFFUSION_REQUEST_STREAM_T non_receiving_request_stream = {
        .on_request = on_request_unexpected
    };
    set_request_stream(
        non_receiving_session,
        message_path,
        DATATYPE_STRING,
        DATATYPE_STRING,
        &non_receiving_request_stream
    );

    SESSION_T *sending_session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    BUF_T *request = buf_create();
    write_diffusion_string_value("Hello.", request);

    SEND_REQUEST_TO_SESSION_PARAMS_T send_request_params = {
        .recipient_session = receiving_session_id,
        .path = message_path,
        .request = request,
        .on_response = on_response,
        .request_datatype = DATATYPE_STRING,
        .response_datatype = DATATYPE_STRING
    };
    send_request_to_session(sending_session, send_request_params);
    buf_free(request);
    MUTEX_WAIT

    session_close(receiving_session, NULL);
    session_free(receiving_session);

    session_close(non_receiving_session, NULL);
    session_free(non_receiving_session);

    session_close(sending_session, NULL);
    session_free(sending_session);
    MUTEX_TERMINATE
}