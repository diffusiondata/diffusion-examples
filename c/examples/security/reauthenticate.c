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


static int on_reauthenticate(
    const SESSION_T * session,
    bool success,
    void *context)
{
    printf("Session has %sbeen reauthenticated.\n", success ? "" : "not ");
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

    SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, NULL);

    COORDINATOR_T *coordinator = coordinator_init();

    DIFFUSION_REAUTHENTICATE_PARAMS_T params = {
        .principal = "control",
        .credentials = credentials,
        .properties = NULL,
        .on_reauthenticate = on_reauthenticate,
        .on_error = on_error,
        .context = coordinator
    };

    diffusion_reauthenticate(session, params, NULL);
    coordinator_wait(coordinator);

    session_close(session, NULL);
    session_free(session);

    coordinator_free(coordinator);
}