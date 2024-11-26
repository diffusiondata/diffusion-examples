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

static int on_session_tree_branches_received(
        const LIST_T *session_tree_branches,
        void *context)
{
    int size = list_get_size(session_tree_branches);
    printf("Session Tree Branches:\n");
    for (int i = 0; i < size; i++) {
        char *session_tree_branch = list_get_data_indexed(session_tree_branches, i);
        printf("\t%s\n", session_tree_branch);
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

    DIFFUSION_SESSION_TREES_GET_SESSION_TREE_BRANCHES_PARAMS_T params = {
        .on_session_tree_branches_received = on_session_tree_branches_received,
        .on_error = on_error
    };

    diffusion_session_trees_get_session_tree_branches(session, params, NULL);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}