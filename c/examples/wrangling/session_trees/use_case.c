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


static int on_security_store_updated(
    SESSION_T *session,
    const LIST_T *error_report,
    void *context)
{
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


    // Administrator
    SESSION_T *admin_session = utils_open_session(url, "admin", "password");

    utils_create_string_topic(
        admin_session, "my/topic/path/for/admin", "Good morning Administrator"
    );

    utils_create_string_topic(
        admin_session, "my/topic/path/for/control", "Good afternoon Control Client"
    );

    utils_create_string_topic(
        admin_session, "my/topic/path/for/anonymous", "Good night Anonymous"
    );

    UTILS_SESSION_TREES_MAPPING_T mappings[] = {
        { .rule = "$Principal is 'admin'", .topic_path =  "my/topic/path/for/admin" },
        { .rule = "$Principal is 'control'", .topic_path =  "my/topic/path/for/control" },
        { .rule = "$Principal is ''", .topic_path =  "my/topic/path/for/anonymous" }
    };

    utils_build_branch_mapping_table(
        admin_session, "my/personal/path", 3, mappings
    );

    VALUE_STREAM_T *admin_value_stream_ptr =
        utils_subscribe(admin_session, "my/personal/path", DATATYPE_STRING);

    // Allow CLIENT role to read and select my/personal/path
    SET_T *set_permissions = set_new(2);
    set_add(set_permissions, &SECURITY_PATH_PERMISSIONS_TABLE[PATH_PERMISSION_SELECT_TOPIC]);
    set_add(set_permissions, &SECURITY_PATH_PERMISSIONS_TABLE[PATH_PERMISSION_READ_TOPIC]);

    SCRIPT_T *script = script_create();
    update_security_store_path_permissions(
        script, "CLIENT", "my/personal/path", set_permissions
    );

    COORDINATOR_T *coordinator = coordinator_init();

    const UPDATE_SECURITY_STORE_PARAMS_T params = {
        .on_update = on_security_store_updated,
        .on_error = on_error,
        .update_script = script,
        .context = coordinator
    };

    update_security_store(admin_session, params);
    coordinator_wait(coordinator);

    script_free(script);
    set_free(set_permissions);

    // Control Client
    SESSION_T *control_session = utils_open_session(url, "control", "password");

    VALUE_STREAM_T *control_value_stream_ptr =
        utils_subscribe(control_session, "my/personal/path", DATATYPE_STRING);

    // Anonymous
    SESSION_T *anonymous_session = utils_open_session(url, "", NULL);

    VALUE_STREAM_T *anonymous_value_stream_ptr =
        utils_subscribe(anonymous_session, "my/personal/path", DATATYPE_STRING);

    // Sleep for a bit to see the notifications
    sleep(2);


    session_close(anonymous_session, NULL);
    session_free(anonymous_session);
    free(anonymous_value_stream_ptr);

    session_close(control_session, NULL);
    session_free(control_session);
    free(control_value_stream_ptr);

    session_close(admin_session, NULL);
    session_free(admin_session);
    free(admin_value_stream_ptr);
    }