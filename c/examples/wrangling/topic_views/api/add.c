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
    char *topic_path = "my/topic/path";

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    utils_create_json_topic(
        session,
        topic_path,
        "{\"diffusion\": \"data\"}"
    );

    char *topic_view_name = "topic_view_1";
    char *topic_view_specification = "map my/topic/path to views/<path(0)>";

    DIFFUSION_CREATE_TOPIC_VIEW_PARAMS_T create_topic_view_params = {
        .view = topic_view_name,
        .specification = topic_view_specification,
        .on_topic_view_created = on_topic_view_created,
        .on_error = on_error
    };
    diffusion_topic_views_create_topic_view(session, create_topic_view_params, NULL);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}