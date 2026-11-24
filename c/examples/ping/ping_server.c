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

static int on_ping_response(SESSION_T * session, void *context_data)
{
    MUTEX_BROADCAST
    printf("Received ping response.\n");
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

    PING_USER_PARAMS_T ping_params = {
        .on_ping_response = on_ping_response
    };
    ping_user(session, ping_params);
    MUTEX_WAIT

    // Insert work here

    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}