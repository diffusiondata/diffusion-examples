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

static int on_branch_mapping_table_received(
    const DIFFUSION_BRANCH_MAPPING_TABLE_T *table,
    void *context)
{
    char *session_tree_branch =
        diffusion_branch_mapping_table_get_session_tree_branch(
            (DIFFUSION_BRANCH_MAPPING_TABLE_T *) table
        );
    printf("%s:\n", session_tree_branch);
    free(session_tree_branch);

    LIST_T *branch_mappings =
        diffusion_branch_mapping_table_get_branch_mappings(
            (DIFFUSION_BRANCH_MAPPING_TABLE_T *) table
        );

    int mappings_length = list_get_size(branch_mappings);
    for (int i = 0; i < mappings_length; i++) {
        DIFFUSION_BRANCH_MAPPING_T *mapping = list_get_data_indexed(branch_mappings, i);

        char *session_filter = diffusion_branch_mapping_get_session_filter(mapping);
        char *topic_tree_branch =  diffusion_branch_mapping_get_topic_tree_branch(mapping);

        printf("\t%s: %s\n", session_filter, topic_tree_branch);
        free(session_filter);
        free(topic_tree_branch);
    }
    diffusion_branch_mapping_table_free_branch_mappings(branch_mappings);
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

    UTILS_SESSION_TREES_MAPPING_T mappings_1[] = {
        { .rule = "$Principal is 'admin'", .topic_path =  "my/topic/path/for/admin" },
        { .rule = "$Principal is 'control'", .topic_path =  "my/topic/path/for/control" },
        { .rule = "$Principal is ''", .topic_path =  "my/topic/path/for/anonymous" }
    };

    utils_build_branch_mapping_table(
        session, "my/personal/path", 3, mappings_1
    );

    UTILS_SESSION_TREES_MAPPING_T mappings_2[] = {
        { .rule = "$Transport is 'WEBSOCKET'", .topic_path = "my/alternate/path/for/websocket" },
        { .rule = "$Transport is 'HTTP_LONG_POLL'", .topic_path = "my/alternate/path/for/http" },
        { .rule = "$Transport is 'TCP'", .topic_path = "my/alternate/path/for/tcp" }
    };

    utils_build_branch_mapping_table(
        session, "my/alternate/path", 3, mappings_2
    );

    DIFFUSION_SESSION_TREES_GET_BRANCH_MAPPING_TABLE_PARAMS_T get_table_params_1 = {
        .on_table_received = on_branch_mapping_table_received,
        .on_error = on_error,
        .session_tree_branch = "my/personal/path"
    };

    diffusion_session_trees_get_branch_mapping_table(session, get_table_params_1, NULL);
    MUTEX_WAIT

    DIFFUSION_SESSION_TREES_GET_BRANCH_MAPPING_TABLE_PARAMS_T get_table_params_2 = {
        .on_table_received = on_branch_mapping_table_received,
        .on_error = on_error,
        .session_tree_branch = "my/alternate/path"
    };

    diffusion_session_trees_get_branch_mapping_table(session, get_table_params_2, NULL);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}