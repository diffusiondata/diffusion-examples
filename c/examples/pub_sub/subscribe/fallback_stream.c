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

static int on_subscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    void *context)
{
    printf("Subscribed to %s.\n", topic_path);
    return HANDLER_SUCCESS;
}

static int on_unsubscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    NOTIFY_UNSUBSCRIPTION_REASON_T reason,
    void *context)
{
    printf("Unsubscribed from %s.\n", topic_path);
    return HANDLER_SUCCESS;
}

static int on_value(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    DIFFUSION_DATATYPE datatype,
    const DIFFUSION_VALUE_T *const old_value,
    const DIFFUSION_VALUE_T *const new_value,
    void *context)
{
    char *old_value_json_string;
    if (old_value == NULL) {
        old_value_json_string = "NULL";
    }
    else {
        to_diffusion_json_string(old_value, &old_value_json_string, NULL);
    }

    char *new_value_json_string;
    if (new_value == NULL) {
        new_value_json_string = "NULL";
    }
    else {
        to_diffusion_json_string(new_value, &new_value_json_string, NULL);
    }

    printf(
        "%s changed from %s to %s.\n",
        topic_path, old_value_json_string, new_value_json_string
    );

    if (old_value != NULL) {
        free(old_value_json_string);
    }

    if (new_value != NULL) {
        free(new_value_json_string);
    }
    return HANDLER_SUCCESS;
}

static int on_subscribe(
    SESSION_T*session,
    void *context)
{
    printf("Subscription request received and approved by the server.\n");
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    utils_create_json_topic(
        session,
        "my/topic/path",
        "{\"diffusion\": [ \"data\", \"some data\" ] }"
    );

    utils_create_json_topic(
        session,
        "my/other/topic/path",
        "{\"diffusion\": [ \"data\", \"more data\" ] }"
    );

    VALUE_STREAM_T value_stream = {
        .datatype = DATATYPE_JSON,
        .on_subscription = on_subscription,
        .on_unsubscription = on_unsubscription,
        .on_value = on_value
    };

    add_fallback_stream(session, &value_stream);

    const char *topic_selector = "?my//";

    SUBSCRIPTION_PARAMS_T params = {
            .topic_selector = topic_selector,
            .on_subscribe = on_subscribe
    };

    subscribe(session, params);
    MUTEX_WAIT

    // Sleep for a bit to see the notifications
    sleep(2);

    utils_create_json_topic(
        session,
        "my/additional/topic/path",
        "{\"diffusion\": [ \"data\", \"even more data\" ] }"
    );

    utils_create_json_topic(
        session,
        "this/topic/path/will/not/be/picked/up",
        "{\"diffusion\": [ \"no data\" ] }"
    );

    // Sleep for a bit to see the notifications
    sleep(2);


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}