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
    printf("Session tree mapping table and mappings removed.\n");
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

    // Administrator
    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    UTILS_SESSION_TREES_MAPPING_T mappings[] = {
        { .rule = "$Principal is 'admin'", .topic_path =  "my/topic/path/for/admin" },
        { .rule = "$Principal is 'control'", .topic_path =  "my/topic/path/for/control" },
        { .rule = "$Principal is ''", .topic_path =  "my/topic/path/for/anonymous" }
    };

    const char *topic_path = "my/personal/path";
    utils_build_branch_mapping_table(
        session, topic_path, 3, mappings
    );

    // To remove a branch mapping table, set it to empty
    DIFFUSION_BRANCH_MAPPING_TABLE_BUILDER_T *builder =
        diffusion_branch_mapping_table_builder_init();

    DIFFUSION_BRANCH_MAPPING_TABLE_T *table =
        diffusion_branch_mapping_table_builder_create_table(
            builder, (char *) topic_path
        );

    DIFFUSION_SESSION_TREES_PUT_BRANCH_MAPPING_TABLE_PARAMS_T params = {
        .on_table_set = on_branch_mapping_table_set,
        .on_error = on_error,
        .table = table
    };
    diffusion_session_trees_put_branch_mapping_table(session, params, NULL);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}