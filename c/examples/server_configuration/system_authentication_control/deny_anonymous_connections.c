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


static int on_system_authentication_store_updated(
    SESSION_T *session,
    const LIST_T *error_report,
    void *context)
{
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


    SESSION_T *session = utils_open_session(url, "admin", "password");

    SCRIPT_T *script = script_create();
    update_auth_store_deny_anonymous_connections(script);

    COORDINATOR_T *coordinator = coordinator_init();

    const UPDATE_SYSTEM_AUTHENTICATION_STORE_PARAMS_T params = {
        .on_update = on_system_authentication_store_updated,
        .on_error = on_error,
        .update_script = script,
        .context = coordinator
    };

    update_system_authentication_store(session, params);
    coordinator_wait(coordinator);

    coordinator_free(coordinator);
    script_free(script);

    CREDENTIALS_T *anonymous_credentials =
        credentials_create_none();

    DIFFUSION_ERROR_T *error = calloc(1, sizeof(DIFFUSION_ERROR_T));
    SESSION_T *anonymous_session = session_create(
        url, "", anonymous_credentials, NULL, NULL, error
    );
    printf("Error while attempting to establish anonymous session:\n");
    printf("\t%d: %s\n", error->code, error->message);
    diffusion_error_free(error);


    session_close(session, NULL);
    session_free(session);


    credentials_free(anonymous_credentials);
    }
