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

static int on_topic_update_add_and_set(
    DIFFUSION_TOPIC_CREATION_RESULT_T result,
    void *context)
{
    if (result == TOPIC_CREATED) {
        printf("Topic has been created.\n");
    }
    else if (result == TOPIC_EXISTS) {
        printf("Topic already exists.\n");
    }
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_json_subscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    void *context)
{
    printf("JSON stream subscribed to %s.\n", topic_path);
    return HANDLER_SUCCESS;
}

static int on_json_unsubscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    NOTIFY_UNSUBSCRIPTION_REASON_T reason,
    void *context)
{
    printf("JSON stream unsubscribed from %s.\n", topic_path);
    return HANDLER_SUCCESS;
}

static int on_json_value(
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
        "JSON stream %s changed from %s to %s.\n",
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

static int on_string_subscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    void *context)
{
    printf("String stream subscribed to %s.\n", topic_path);
    return HANDLER_SUCCESS;
}

static int on_string_unsubscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    NOTIFY_UNSUBSCRIPTION_REASON_T reason,
    void *context)
{
    printf("String stream unsubscribed from %s.\n", topic_path);
    return HANDLER_SUCCESS;
}

static int on_string_value(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    DIFFUSION_DATATYPE datatype,
    const DIFFUSION_VALUE_T *const old_value,
    const DIFFUSION_VALUE_T *const new_value,
    void *context)
{
    char *old_value_string;
    if (old_value == NULL) {
        old_value_string = "NULL";
    }
    else {
        read_diffusion_string_value(old_value, &old_value_string, NULL);
    }

    char *new_value_string;
    if (new_value == NULL) {
        new_value_string = "NULL";
    }
    else {
        read_diffusion_string_value(new_value, &new_value_string, NULL);
    }

    printf(
        "String stream %s changed from %s to %s.\n",
        topic_path, old_value_string, new_value_string
    );

    if (old_value != NULL) {
        free(old_value_string);
    }

    if (new_value != NULL) {
        free(new_value_string);
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

static int on_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    printf("On error: %s\n", error->message);
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    char *topic_path = "my/int/topic/path";

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    TOPIC_SPECIFICATION_T *topic_specification = topic_specification_init(TOPIC_TYPE_INT64);
    BUF_T *value = buf_create();
    write_diffusion_int64_value(123, value);

    DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T add_and_set_params = {
        .datatype = DATATYPE_INT64,
        .on_topic_update_add_and_set = on_topic_update_add_and_set,
        .topic_path = topic_path,
        .specification = topic_specification,
        .update = value,
        .on_error = on_error
    };
    diffusion_topic_update_add_and_set(session, add_and_set_params);
    MUTEX_WAIT
    buf_free(value);
    topic_specification_free(topic_specification);

    // JSON is compatible with INT64 datatype
    // So this stream will receive notifications from the INT64 topic
    VALUE_STREAM_T json_value_stream = {
        .datatype = DATATYPE_JSON,
        .on_subscription = on_json_subscription,
        .on_unsubscription = on_json_unsubscription,
        .on_value = on_json_value
    };

    add_stream(session, topic_path, &json_value_stream);

    // String is not compatible with INT64 datatype
    // This stream will receive notifications of
    // on_subscription and on_unsubscription, but
    // no on_value notifications
    VALUE_STREAM_T string_value_stream = {
        .datatype = DATATYPE_STRING,
        .on_subscription = on_string_subscription,
        .on_unsubscription = on_string_unsubscription,
        .on_value = on_string_value
    };

    add_stream(session, topic_path, &string_value_stream);

    SUBSCRIPTION_PARAMS_T params = {
            .topic_selector = topic_path,
            .on_subscribe = on_subscribe
    };

    subscribe(session, params);
    MUTEX_WAIT

    // Sleep for a bit to see the notifications
    sleep(2);


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}