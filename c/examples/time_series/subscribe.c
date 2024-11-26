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
    char *old_value_string;
    if (old_value == NULL) {
        old_value_string = "NULL";
    }
    else {
        DIFFUSION_TIME_SERIES_EVENT_T *old_event;
        read_diffusion_time_series_event(old_value, &old_event, NULL);

        DIFFUSION_VALUE_T *old_event_value =
             diffusion_time_series_event_get_value(old_event);

        double old_value_double;
        read_diffusion_double_value(old_event_value, &old_value_double, NULL);

        old_value_string = calloc(20, sizeof(char));
        sprintf(old_value_string, "%g", old_value_double);
    }

    char *new_value_string;
    if (new_value == NULL) {
        new_value_string = "NULL";
    }
    else {
        DIFFUSION_TIME_SERIES_EVENT_T *new_event;
        read_diffusion_time_series_event(new_value, &new_event, NULL);

        DIFFUSION_VALUE_T *new_event_value =
             diffusion_time_series_event_get_value(new_event);

        double new_value_double;
        read_diffusion_double_value(new_event_value, &new_value_double, NULL);
        new_value_string = calloc(20, sizeof(char));
        sprintf(new_value_string, "%g", new_value_double);
    }

    printf(
        "%s changed from %s to %s.\n",
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


static int on_append(
        const DIFFUSION_TIME_SERIES_EVENT_METADATA_T *event_metadata,
        void *context)
{
        return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    char *topic_path = "my/time/series/topic/path";

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    HASH_T *properties = hash_new(5);
    hash_add(properties, DIFFUSION_TIME_SERIES_EVENT_VALUE_TYPE, "double");
    hash_add(properties, DIFFUSION_TIME_SERIES_RETAINED_RANGE, "limit 15 last 10s");
    hash_add(properties, DIFFUSION_TIME_SERIES_SUBSCRIPTION_RANGE, "limit 3");

    utils_create_topic_with_properties(
        session,
        topic_path,
        TOPIC_TYPE_TIME_SERIES,
        DATATYPE_DOUBLE,
        NULL,
        properties
    );
    hash_free(properties, NULL, NULL);

    VALUE_STREAM_T value_stream = {
        .datatype = DATATYPE_DOUBLE,
        .on_subscription = on_subscription,
        .on_unsubscription = on_unsubscription,
        .on_value = on_value
    };

    add_time_series_stream(session, topic_path, &value_stream);

    SUBSCRIPTION_PARAMS_T params = {
            .topic_selector = "?/my/time/series//",
            .on_subscribe = on_subscribe
    };

    subscribe(session, params);
    MUTEX_WAIT

    for (int i = 0; i < 25; i++) {
        double random_value = utils_random_double();

        BUF_T *value = buf_create();
        write_diffusion_double_value(random_value, value);

        DIFFUSION_TIME_SERIES_APPEND_PARAMS_T params = {
                .on_append = on_append,
                .topic_path = topic_path,
                .datatype = DATATYPE_DOUBLE,
                .value = value
        };

        diffusion_time_series_append(session, params, NULL);
        MUTEX_WAIT
        buf_free(value);
    }

    // Sleep for a bit to see the notifications
    sleep(2);

    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}