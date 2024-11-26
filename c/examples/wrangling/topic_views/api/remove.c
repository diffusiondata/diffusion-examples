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

static int on_topic_views_list_received(
    const LIST_T *topic_views,
    void *context)
{
    int total_views = list_get_size(topic_views);
    printf("Received %d topic view(s)\n", total_views);

    for(int i = 0; i < total_views; i++) {
        DIFFUSION_TOPIC_VIEW_T *topic_view =
            list_get_data_indexed(topic_views, i);

        char *name = diffusion_topic_view_get_name(topic_view);
        char *specification = diffusion_topic_view_get_specification(topic_view);
        SET_T *roles_set = diffusion_topic_view_get_roles(topic_view);
        char *roles = utils_set_to_string(roles_set, utils_print_string);

        printf("%s:\n\t%s\n\t%s\n", name, specification, roles);

        free(roles);
        free(specification);
        free(name);
        set_free(roles_set);
    }

    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_topic_view_removed(
        void *context)
{
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

    utils_create_topic_view(
        session,
        "topic_view_1",
        "map my/topic/path to views/<path(0)>"
    );

    utils_create_topic_view(
        session,
        "topic_view_2",
        "map my/topic/path/array to views/<path(0)>"
    );

    DIFFUSION_TOPIC_VIEWS_LIST_PARAMS_T list_topic_views_params = {
        .on_topic_views_list = on_topic_views_list_received
    };

    printf("Listing topic views before removal\n");
    diffusion_topic_views_list_topic_views(session, list_topic_views_params, NULL);
    MUTEX_WAIT

    DIFFUSION_REMOVE_TOPIC_VIEW_PARAMS_T remove_params = {
        .view = "topic_view_1",
        .on_topic_view_removed = on_topic_view_removed,
        .on_error = on_error
    };
    diffusion_topic_views_remove_topic_view(session, remove_params, NULL);
    MUTEX_WAIT

    printf("Listing topic views after removal\n");
    diffusion_topic_views_list_topic_views(session, list_topic_views_params, NULL);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}