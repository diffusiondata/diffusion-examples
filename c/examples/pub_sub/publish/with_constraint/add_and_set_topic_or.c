/**
 * Copyright © 2025 DiffusionData Ltd.
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
    coordinator_broadcast((COORDINATOR_T *) context);
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

    char *topic_path = "my/topic/path ";

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    TOPIC_SPECIFICATION_T *topic_specification = topic_specification_init(TOPIC_TYPE_JSON);
    BUF_T *value_1 = buf_create();
    write_diffusion_json_value("{\"diffusion\": \"data\"}", value_1);

    BUF_T *value_2 = buf_create();
    write_diffusion_json_value("{\"diffusion\": \"data2\"}", value_2);

    COORDINATOR_T *coordinator = coordinator_init();

    DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T params = {
        .datatype = DATATYPE_JSON,
        .on_topic_update_add_and_set = on_topic_update_add_and_set,
        .topic_path = topic_path,
        .specification = topic_specification,
        .update = value_1,
        .on_error = on_error,
        .context = coordinator
    };

    diffusion_topic_update_add_and_set(session, params);
    coordinator_wait(coordinator);

    DIFFUSION_UPDATE_CONSTRAINT_VALUE_T *constraint_value_1 =
        diffusion_update_constraint_value_from_string("data");

    DIFFUSION_TOPIC_UPDATE_CONSTRAINT_T *constraint_1 =
        diffusion_topic_update_constraint_partial_json_comparison(
            "/diffusion", DIFFUSION_TOPIC_UPDATE_CONSTRAINT_OPERATOR_IS, constraint_value_1
        );

    DIFFUSION_UPDATE_CONSTRAINT_VALUE_T *constraint_value_2 =
        diffusion_update_constraint_value_from_string("data2");

    DIFFUSION_TOPIC_UPDATE_CONSTRAINT_T *constraint_2 =
        diffusion_topic_update_constraint_partial_json_comparison(
            "/diffusion", DIFFUSION_TOPIC_UPDATE_CONSTRAINT_OPERATOR_IS, constraint_value_2
        );

    DIFFUSION_TOPIC_UPDATE_CONSTRAINT_T *constraint =
        diffusion_topic_update_constraint_or(constraint_1, constraint_2);

    DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T new_set_params_1 = {
        .datatype = DATATYPE_JSON,
        .on_topic_update_add_and_set = on_topic_update_add_and_set,
        .topic_path = topic_path,
        .specification = topic_specification,
        .update = value_1,
        .on_error = on_error,
        .context = coordinator
    };

    diffusion_topic_update_add_and_set_with_constraint(session, constraint, new_set_params_1);
    coordinator_wait(coordinator);

    DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T new_set_params_2 = {
        .datatype = DATATYPE_JSON,
        .on_topic_update_add_and_set = on_topic_update_add_and_set,
        .topic_path = topic_path,
        .specification = topic_specification,
        .update = value_2,
        .on_error = on_error,
        .context = coordinator
    };

    diffusion_topic_update_add_and_set_with_constraint(session, constraint, new_set_params_2);
    coordinator_wait(coordinator);

    session_close(session, NULL);
    session_free(session);

    coordinator_free(coordinator);

    diffusion_topic_update_constraint_free(constraint);
    diffusion_topic_update_constraint_free(constraint_2);
    diffusion_topic_update_constraint_free(constraint_1);
    diffusion_update_constraint_value_free(constraint_value_1);
    diffusion_update_constraint_value_free(constraint_value_2);

    buf_free(value_1);
    buf_free(value_2);
    topic_specification_free(topic_specification);
}