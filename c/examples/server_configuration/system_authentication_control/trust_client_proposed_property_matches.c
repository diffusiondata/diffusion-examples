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

    SCRIPT_T *script = script_create();
    update_auth_store_trust_client_proposed_property_matches(
        script, "Flintstone", ".*_Flintstone"
    );

    const UPDATE_SYSTEM_AUTHENTICATION_STORE_PARAMS_T params = {
        .on_update = on_system_authentication_store_updated,
        .on_error = on_error,
        .update_script = script
    };

    update_system_authentication_store(admin_session, params);
    MUTEX_WAIT
    script_free(script);

    DIFFUSION_SESSION_FACTORY_T *session_factory = diffusion_session_factory_init();
    diffusion_session_factory_principal(session_factory, principal);
    diffusion_session_factory_credentials(session_factory, credentials);

    // Establishing session with invalid values for the session property
    // Due to the invalid value, the session property will not appear for the session
    diffusion_session_factory_property(session_factory, "Flintstone", "Barney_Rubble");

    SESSION_T *invalid_value_session = session_create_with_session_factory(session_factory, url);

    utils_print_session_properties(
        admin_session, invalid_value_session->id, NULL
    );

    session_close(invalid_value_session, NULL);
    session_free(invalid_value_session);

    // Establishing session with valid value for the session property
    diffusion_session_factory_property(session_factory, "Flintstone", "Fred_Flintstone");

    SESSION_T *valid_value_session = session_create_with_session_factory(session_factory, url);

    utils_print_session_properties(
        admin_session, valid_value_session->id, NULL
    );

    session_close(valid_value_session, NULL);
    session_free(valid_value_session);


    session_close(admin_session, NULL);
    session_free(admin_session);

    MUTEX_TERMINATE
}
