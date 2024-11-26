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
    char *topic_path = "my/topic/path ";

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    TOPIC_SPECIFICATION_T *topic_specification = topic_specification_init(TOPIC_TYPE_JSON);
    BUF_T *value = buf_create();
    write_diffusion_json_value("{\"diffusion\": \"data\"}", value);

    DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T params = {
        .datatype = DATATYPE_JSON,
        .on_topic_update_add_and_set = on_topic_update_add_and_set,
        .topic_path = topic_path,
        .specification = topic_specification,
        .update = value,
        .on_error = on_error
    };

    diffusion_topic_update_add_and_set(session, params);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);
    buf_free(value);
    topic_specification_free(topic_specification);
    MUTEX_TERMINATE
}