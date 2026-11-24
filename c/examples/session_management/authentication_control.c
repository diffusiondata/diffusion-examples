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

static int on_auth_handler_active(
    SESSION_T *session,
    const DIFFUSION_REGISTRATION_T *registration)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_authenticate(
    SESSION_T *session,
    const char *principal,
    const CREDENTIALS_T *credentials,
    const HASH_T *session_properties,
    const HASH_T *proposed_session_properties,
    const DIFFUSION_AUTHENTICATOR_T *authenticator)
{
    if (strlen(principal) == 0) {
        printf(
            "Anonymous connection attempt detected.\n"
            "Session establishment rejected.\n\n"
        );
        diffusion_authenticator_deny(session, authenticator, NULL);
    }
    else if(utils_string_starts_with("diffusion_", principal)) {
        printf(
            "Principal begins with diffusion_ prefix.\n"
            "Session establishment accepted.\n\n"
        );
        diffusion_authenticator_allow(session, authenticator, NULL);
    }
    else {
        printf(
            "Principal does not begin with diffusion_ prefix.\n"
            "Session establishment rejected.\n\n"
        );
        diffusion_authenticator_deny(session, authenticator, NULL);
    }
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_auth_handler_error(
    const DIFFUSION_ERROR_T *error)
{
    printf("On authentication handler error: %s\n", error->message);
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

    DIFFUSION_AUTHENTICATION_HANDLER_T handler = {
        .handler_name = "before-system-handler",
        .on_active = on_auth_handler_active,
        .on_authenticate = on_authenticate,
        .on_error = on_auth_handler_error
    };

    DIFFUSION_AUTHENTICATION_HANDLER_PARAMS_T params = {
        .handler = &handler
    };

    diffusion_set_authentication_handler(session, params);
    MUTEX_WAIT

    CREDENTIALS_T *control_credentials =
        credentials_create_password("password");

    SESSION_T *control_session = session_create(
        url, "control", control_credentials, NULL, NULL, NULL
    );
    MUTEX_WAIT

    CREDENTIALS_T *anonymous_credentials =
        credentials_create_none();

    SESSION_T *anonymous_session = session_create(
        url, "", anonymous_credentials, NULL, NULL, NULL
    );
    MUTEX_WAIT

    CREDENTIALS_T *diffusion_control_credentials =
        credentials_create_password("password");

    SESSION_T *diffusion_control_session = session_create(
        url, "diffusion_control", diffusion_control_credentials, NULL, NULL, NULL
    );
    MUTEX_WAIT

    session_close(diffusion_control_session, NULL);
    session_free(diffusion_control_session);

    session_close(session, NULL);
    session_free(session);

    credentials_free(control_credentials);
    credentials_free(anonymous_credentials);
    credentials_free(diffusion_control_credentials);
    MUTEX_TERMINATE
}