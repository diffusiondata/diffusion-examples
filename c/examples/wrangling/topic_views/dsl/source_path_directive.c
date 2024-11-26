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

static int on_topic_view_created(
    const DIFFUSION_TOPIC_VIEW_T *topic_view,
    void *context)
{
    char *name = diffusion_topic_view_get_name(topic_view);
    printf("Topic view %s was created.\n", name);
    free(name);
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
    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    utils_create_json_topic(
        session,
        "a/b/c/d/e/f/g",
        "{                                  \
            \"account\": \"1234\",          \
            \"balance\": {                  \
                \"amount\": 12.57,          \
                \"currency\": \"USD\"       \
            }                               \
        }"
    );

    const char *topic_selector = "?views//";
    VALUE_STREAM_T *value_stream_ptr =
        utils_subscribe(session, topic_selector, DATATYPE_JSON);

    utils_create_topic_view(
        session,
        "topic_view_1",
        "map a/b/c/d/e/f/g to views/<path(0)>"
    );

    utils_create_topic_view(
        session,
        "topic_view_2",
        "map a/b/c/d/e/f/g to views/<path(2)>"
    );

    utils_create_topic_view(
        session,
        "topic_view_3",
        "map a/b/c/d/e/f/g to views/<path(3,5)>"
    );

    // Sleep for a bit to see the notifications
    sleep(2);


    session_close(session, NULL);
    session_free(session);
    free(value_stream_ptr);
    MUTEX_TERMINATE
}