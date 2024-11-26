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

static int on_security_store_updated(
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

    SESSION_T *session = utils_open_session(url, "admin", "password");

    const char *topic_path = "my/topic/path";

    printf("Original Security Store Settings for role CLIENT\n");
    utils_print_security_store(session, "CLIENT");

    SET_T *set_permissions = set_new(2);
    set_add(set_permissions, &SECURITY_PATH_PERMISSIONS_TABLE[PATH_PERMISSION_UPDATE_TOPIC]);
    set_add(set_permissions, &SECURITY_PATH_PERMISSIONS_TABLE[PATH_PERMISSION_MODIFY_TOPIC]);

    SCRIPT_T *set_script = script_create();
    update_security_store_path_permissions(
        set_script, "CLIENT", topic_path, set_permissions
    );

    const UPDATE_SECURITY_STORE_PARAMS_T set_params = {
        .on_update = on_security_store_updated,
        .on_error = on_error,
        .update_script = set_script
    };

    printf("\nAllowing Role CLIENT to update and modify %s.\n", topic_path);
    utils_print_script(set_script);

    update_security_store(session, set_params);
    MUTEX_WAIT
    script_free(set_script);
    set_free(set_permissions);

    printf("Security Store settings for role CLIENT after new path permissions for %s\n", topic_path);
    utils_print_security_store(session, "CLIENT");

    SCRIPT_T *remove_script = script_create();
    update_security_store_remove_path_permissions(
        remove_script, "CLIENT", topic_path
    );

    const UPDATE_SECURITY_STORE_PARAMS_T remove_params = {
        .on_update = on_security_store_updated,
        .on_error = on_error,
        .update_script = remove_script
    };

    printf("\nRemoving path permissions for Role CLIENT at %s.\n", topic_path);
    utils_print_script(remove_script);

    update_security_store(session, remove_params);
    MUTEX_WAIT
    script_free(remove_script);

    printf("Security Store settings for role CLIENT after removal of path permissions\n");
    utils_print_security_store(session, "CLIENT");

    session_close(session, NULL);
    session_free(session);

    MUTEX_TERMINATE
}
