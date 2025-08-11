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


static int on_change_success(
    SESSION_T * session,
    void *context)
{
    printf("Principal has been changed to control.\n");
    coordinator_broadcast((COORDINATOR_T *) context);
    return HANDLER_SUCCESS;
}

static int on_change_failure(
    SESSION_T *session,
    void *context)
{
    printf("An error has occurred while changing principal to control.\n");
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{

    SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, NULL);

    COORDINATOR_T *coordinator = coordinator_init();

    CHANGE_PRINCIPAL_PARAMS_T params = {
        .principal = "control",
        .credentials = credentials,
        .on_change_principal = on_change_success,
        .on_change_principal_failure = on_change_success,
        .context = coordinator
    };

    change_principal(session, params);
    coordinator_wait(coordinator);

    session_close(session, NULL);
    session_free(session);

    coordinator_free(coordinator);
}