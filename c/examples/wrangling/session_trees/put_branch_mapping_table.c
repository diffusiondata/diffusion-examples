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

static int on_branch_mapping_table_set(
        void *context)
{
    printf("Branch mapping table updated\n");
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
    char *topic_path = "my/personal/path";

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    DIFFUSION_BRANCH_MAPPING_TABLE_BUILDER_T *builder =
        diffusion_branch_mapping_table_builder_init();

    diffusion_branch_mapping_table_builder_add_branch_mapping(
        builder, "$Principal is 'admin'", "my/topic/path/for/admin"
    );

    diffusion_branch_mapping_table_builder_add_branch_mapping(
        builder, "$Principal is 'control'", "my/topic/path/for/control"
    );

    diffusion_branch_mapping_table_builder_add_branch_mapping(
        builder, "$Principal is ''", "my/topic/path/for/anonymous"
    );

    DIFFUSION_BRANCH_MAPPING_TABLE_T *table =
        diffusion_branch_mapping_table_builder_create_table(builder, topic_path);

    DIFFUSION_SESSION_TREES_PUT_BRANCH_MAPPING_TABLE_PARAMS_T params = {
        .on_table_set = on_branch_mapping_table_set,
        .on_error = on_error,
        .table = table
    };
    diffusion_session_trees_put_branch_mapping_table(session, params, NULL);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);

    diffusion_branch_mapping_table_free(table);
    diffusion_branch_mapping_table_builder_free(builder);
    MUTEX_TERMINATE
}