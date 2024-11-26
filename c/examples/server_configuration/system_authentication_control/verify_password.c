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

    // Verify with current password
    // And set new password for admin principal

    SCRIPT_T *script = script_create();
    update_auth_store_verify_password(
        script, "control", "password"
    );
    update_auth_store_set_password(
        script, "control", "12345"
    );

    const UPDATE_SYSTEM_AUTHENTICATION_STORE_PARAMS_T params = {
        .on_update = on_system_authentication_store_updated,
        .on_error = on_error,
        .update_script = script
    };

    update_system_authentication_store(admin_session, params);
    MUTEX_WAIT
    script_free(script);

    // Login with the new password
    SESSION_T *new_control_session = utils_open_session(url, "control", "12345");

    // Attempt to verify with invalid password
    // And set new password for admin principal

    SCRIPT_T *invalid_verification_script = script_create();
    update_auth_store_verify_password(
        invalid_verification_script, "control", "this_is_not_the_right_password"
    );
    update_auth_store_set_password(
        invalid_verification_script, "control", "new_password"
    );

    const UPDATE_SYSTEM_AUTHENTICATION_STORE_PARAMS_T invalid_verification_params = {
        .on_update = on_system_authentication_store_updated,
        .on_error = on_error,
        .update_script = invalid_verification_script
    };

    update_system_authentication_store(admin_session, invalid_verification_params);
    MUTEX_WAIT

    script_free(invalid_verification_script);
    session_close(new_control_session, NULL);
    session_free(new_control_session);

    // Attempt to login with new password
    CREDENTIALS_T *new_invalid_control_credentials =
        credentials_create_password("new_password");

    DIFFUSION_ERROR_T error = { 0 };
    SESSION_T *new_invalid_control_session = session_create(
        url, "control", new_invalid_control_credentials, NULL, NULL, &error
    );
    printf("Error while attempting to establish session:\n");
    printf("\t%d: %s\n", error.code, error.message);
    credentials_free(new_invalid_control_credentials);


    session_close(admin_session, NULL);
    session_free(admin_session);

    MUTEX_TERMINATE
}
