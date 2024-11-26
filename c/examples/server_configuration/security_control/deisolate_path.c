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

    printf("Original Store\n");
    utils_print_security_store(session, NULL);

    const char *topic_path = "my/topic/path";

    SCRIPT_T *isolate_path_script = script_create();
    update_security_store_isolate_path(isolate_path_script, topic_path);

    const UPDATE_SECURITY_STORE_PARAMS_T isolate_path_params = {
        .on_update = on_security_store_updated,
        .on_error = on_error,
        .update_script = isolate_path_script
    };

    printf("\nIsolating %s permissions from parent and default path permissions.\n", topic_path);
    utils_print_script(isolate_path_script);

    update_security_store(session, isolate_path_params);
    MUTEX_WAIT
    script_free(isolate_path_script);

    SCRIPT_T *deisolate_path_script = script_create();
    update_security_store_deisolate_path(deisolate_path_script, topic_path);

    const UPDATE_SECURITY_STORE_PARAMS_T deisolate_path_params = {
        .on_update = on_security_store_updated,
        .on_error = on_error,
        .update_script = deisolate_path_script
    };

    printf("\nRemoving %s permission isolation.\n", topic_path);
    utils_print_script(deisolate_path_script);

    update_security_store(session, deisolate_path_params);
    MUTEX_WAIT
    script_free(deisolate_path_script);

    session_close(session, NULL);
    session_free(session);

    MUTEX_TERMINATE
}
