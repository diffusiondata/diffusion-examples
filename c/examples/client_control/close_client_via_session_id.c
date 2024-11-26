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

static int on_closed(void *context)
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

static void on_session_state_changed(
    SESSION_T *session,
    const SESSION_STATE_T old_state,
    const SESSION_STATE_T new_state)
{
    printf(
        "Session state changed from %s (%d) to %s (%d)\n",
        session_state_as_string(old_state), old_state,
        session_state_as_string(new_state), new_state
    );
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    SESSION_T *admin_session = utils_open_session(url, "admin", "password");

    CREDENTIALS_T *client_credentials =
        credentials_create_password("password");

    SESSION_LISTENER_T session_listener = {
        .on_state_changed = &on_session_state_changed
    };

    SESSION_T *client_session = session_create(
        url, "client", client_credentials, &session_listener, NULL, NULL
    );

    DIFFUSION_CLIENT_CLOSE_WITH_SESSION_PARAMS_T params = {
        .session_id = client_session->id,
        .on_closed = on_closed,
        .on_error = on_error
    };

    diffusion_client_close_with_session(admin_session, params, NULL);
    MUTEX_WAIT

    // Sleep for a bit to see the notifications
    sleep(2);

    session_free(client_session);
    credentials_free(client_credentials);

    session_close(admin_session, NULL);
    session_free(admin_session);
    MUTEX_TERMINATE
}