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


static int on_session_properties(
    SESSION_T *session,
    const SVC_GET_SESSION_PROPERTIES_RESPONSE_T *response,
    void *context)
{
    printf("Received the following session properties:\n");
    utils_print_hash(
        response->properties, context, utils_print_string, false
    );
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


    SESSION_T *admin_session = utils_open_session(url, "admin", "password");

    SESSION_T *client_session = utils_open_session(url, "client", "password");

    SET_T *required_properties = set_new(1);
    set_add(required_properties, PROPERTIES_SELECTOR_ALL_FIXED_PROPERTIES);

    COORDINATOR_T *coordinator = coordinator_init();

    GET_SESSION_PROPERTIES_PARAMS_T get_session_properties_params = {
        .on_session_properties = on_session_properties,
        .on_error = on_error,
        .session_id = client_session->id,
        .required_properties = required_properties,
        .context = coordinator
    };

    get_session_properties(admin_session, get_session_properties_params);
    coordinator_wait(coordinator);

    session_close(client_session, NULL);
    session_free(client_session);

    session_close(admin_session, NULL);
    session_free(admin_session);

    coordinator_free(coordinator);
    set_free(required_properties);
}