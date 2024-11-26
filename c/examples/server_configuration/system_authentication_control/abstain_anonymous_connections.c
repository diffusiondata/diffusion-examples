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

static int on_system_authentication_store_updated(
    SESSION_T *session,
    const LIST_T *error_report,
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

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
    else {
        diffusion_authenticator_allow(session, authenticator, NULL);
    }
    return HANDLER_SUCCESS;
}

static int on_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    printf("On error: %s\n", error->message);
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

    SESSION_T *admin_session = utils_open_session(url, "admin", "password");
    SCRIPT_T *script = script_create();
    update_auth_store_abstain_anonymous_connections(script);

    const UPDATE_SYSTEM_AUTHENTICATION_STORE_PARAMS_T params = {
        .on_update = on_system_authentication_store_updated,
        .on_error = on_error,
        .update_script = script
    };

    update_system_authentication_store(admin_session, params);
    MUTEX_WAIT
    script_free(script);

    SESSION_T *auth_handler_session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    DIFFUSION_AUTHENTICATION_HANDLER_T auth_handler = {
        .handler_name = "after-system-handler",
        .on_active = on_auth_handler_active,
        .on_authenticate = on_authenticate,
        .on_error = on_auth_handler_error
    };

    DIFFUSION_AUTHENTICATION_HANDLER_PARAMS_T auth_handler_params = {
        .handler = &auth_handler,
        .on_error = on_error
    };

    diffusion_set_authentication_handler(auth_handler_session, auth_handler_params);
    MUTEX_WAIT

    CREDENTIALS_T *anonymous_credentials =
        credentials_create_none();

    DIFFUSION_ERROR_T error = { 0 };
    SESSION_T *anonymous_session = session_create(
        url, "", anonymous_credentials, NULL, NULL, &error
    );
    printf("Error while attempting to establish anonymous session:\n");
    printf("\t%d: %s\n", error.code, error.message);


    session_close(admin_session, NULL);
    session_free(admin_session);

    session_close(auth_handler_session, NULL);
    session_free(auth_handler_session);


    credentials_free(anonymous_credentials);
    MUTEX_TERMINATE
}
