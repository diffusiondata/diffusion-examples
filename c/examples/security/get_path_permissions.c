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

static int on_path_permissions_received(
    const SET_T *path_permissions,
    void *context)
{
        void **values = set_values(path_permissions);
        printf("Received Path Permissions (%ld):\n", path_permissions->size);
        for(int i = 0; values[i] != NULL; i++) {
            PATH_PERMISSIONS_T *permission = (PATH_PERMISSIONS_T *) values[i];
            printf("\t%s\n", utils_print_path_permission(permission));
        }
        free(values);
        return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, NULL);

    DIFFUSION_GET_PATH_PERMISSIONS_PARAMS_T params = {
        .path = ".*//",
        .on_path_permissions = on_path_permissions_received
    };

    diffusion_get_path_permissions(session, params, NULL);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}