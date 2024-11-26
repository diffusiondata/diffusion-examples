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

static int on_roles_changed(
    int number_of_matching_sessions,
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

    SESSION_T *admin_session = utils_open_session(url, "admin", "password");

    SESSION_T *client_session = utils_open_session(url, "client", "password");

    printf("Original value\n");
    utils_print_session_properties(admin_session, client_session->id, "$Roles");

    SET_T *new_roles = set_new_string(1);
    set_add(new_roles, "TOPIC_CONTROL");

    DIFFUSION_CHANGE_ROLES_WITH_FILTER_PARAMS_T params = {
        .filter = "$Principal is 'client'",
        .roles_to_remove = NULL,
        .roles_to_add = new_roles,
        .on_roles_changed = on_roles_changed,
        .on_error = on_error
    };
    diffusion_change_roles_with_filter(admin_session, params, NULL);
    MUTEX_WAIT

    printf("\nChanged value\n");
    utils_print_session_properties(admin_session, client_session->id, "$Roles");

    session_close(client_session, NULL);
    session_free(client_session);

    session_close(admin_session, NULL);
    session_free(admin_session);
    MUTEX_TERMINATE
}