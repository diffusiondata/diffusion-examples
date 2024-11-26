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
SESSION_T *g_session;

static int on_connected(
    SESSION_T *session)
{
    g_session = session;
    char *sid = session_id_to_string(session->id);
    printf("Connected. Session Identifier: %s\n", sid);
    free(sid);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


static int on_error(
    SESSION_T *session,
    DIFFUSION_ERROR_T *error)
{
    printf("An error has occurred: %s\n", error->message);
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    SESSION_CREATE_CALLBACK_T *callbacks = calloc(1, sizeof(SESSION_CREATE_CALLBACK_T));
    callbacks->on_connected = &on_connected;
    callbacks->on_error = &on_error;

    DIFFUSION_ERROR_T error = { 0 };
    session_create_async(
        url, principal, credentials, NULL, NULL, callbacks, &error
    );
    MUTEX_WAIT

    // Insert work here

    session_close(g_session, NULL);
    session_free(g_session);

    free(callbacks);
    MUTEX_TERMINATE
}