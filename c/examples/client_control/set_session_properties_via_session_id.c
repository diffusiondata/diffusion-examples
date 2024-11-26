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

static int on_session_properties_set(
    const HASH_T *properties,
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

    printf("Original session properties\n");
    utils_print_session_properties(admin_session, client_session->id, NULL);

    HASH_T *new_session_properties = hash_new(2);
    hash_add(new_session_properties, "$Language", "en-gb");

    DIFFUSION_SET_SESSION_PROPERTIES_PARAMS_T params = {
        .session_id = client_session->id,
        .properties = new_session_properties,
        .on_session_properties_set = on_session_properties_set,
        .on_error = on_error
    };

    diffusion_set_session_properties(admin_session, params, NULL);
    MUTEX_WAIT

    printf("\nChanged session properties\n");
    utils_print_session_properties(admin_session, client_session->id, NULL);

    session_close(client_session, NULL);
    session_free(client_session);

    session_close(admin_session, NULL);
    session_free(admin_session);

    MUTEX_TERMINATE
}