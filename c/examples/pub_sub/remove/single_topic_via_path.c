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

static int on_topic_removed(
    SESSION_T *session,
    const DIFFUSION_TOPIC_REMOVAL_RESULT_T *response,
    void *context)
{
    int removal_count = diffusion_topic_removal_result_removed_count(response);
    printf("Removed %d topic(s).\n", removal_count);
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
    char *topic_path = "my/topic/path/to/be/removed";
;

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    utils_create_json_topic(
        session,
        topic_path,
        "{\"diffusion\": [ \"data\", \"more data\" ] }"
    );

    utils_create_json_topic(
        session,
        "my/topic/path/will/not/be/removed",
        "{\"diffusion\": [ \"no data\" ] }"
    );

    utils_create_json_topic(
        session,
        "my/topic/path/will/not/be/removed/either",
        "{\"diffusion\": [ \"no data either\" ] }"
    );

    TOPIC_REMOVAL_PARAMS_T remove_params = {
        .topic_selector = topic_path,
        .on_removed = on_topic_removed,
        .on_error = on_error
    };

    topic_removal(session, remove_params);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}