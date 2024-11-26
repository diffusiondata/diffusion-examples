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

static int on_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    printf("On error: %s\n", error->message);
    return HANDLER_SUCCESS;
}

static int on_missing_topic(
    SESSION_T * session,
    const SVC_MISSING_TOPIC_REQUEST_T *request,
    void *context)
{
    printf("Received missing topic notification:\n");

    char *session_id = session_id_to_string(request->session_id);
    printf("\tSession ID: %s\n", session_id);
    free(session_id);

    printf("\tTopic Selector: %s\n", request->topic_selector);

    utils_create_string_topic(
        g_control_session, "my/topic/path/does/not/exist/yet", "Hello");

    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT

    SESSION_T *admin_session = utils_open_session(url, "admin", "password");

    const char *topic_path_root = "my/topic/path";
    MISSING_TOPIC_PARAMS_T missing_topic_params = {

        .topic_path = topic_path_root,
        .on_missing_topic = on_missing_topic,
        .on_error = on_error
    };
    CONVERSATION_ID_T *missing_topic_cid =
        missing_topic_register_handler(admin_session, missing_topic_params);

    g_control_session = utils_open_session(url, "control", "password");

    VALUE_STREAM_T *value_stream_ptr = utils_subscribe(
        g_control_session, "my/topic/path/does/not/exist/yet", DATATYPE_STRING
    );
    MUTEX_WAIT

    // Sleep for a bit to see the notifications
    sleep(2);

    missing_topic_deregister_handler(admin_session, missing_topic_cid);

    // Sleep for a bit to see the notifications
    sleep(2);

    session_close(g_control_session, NULL);
    session_free(g_control_session);
    free(value_stream_ptr);

    session_close(admin_session, NULL);
    session_free(admin_session);

    MUTEX_TERMINATE
}