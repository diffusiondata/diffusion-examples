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

LIST_T *g_remote_server_names;

static int on_remote_server_created(
    DIFFUSION_REMOTE_SERVER_T *remote_server,
    LIST_T *errors,
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


static int on_remote_server_removed(void *context)
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

    const char *remote_server_name = "Remote Server 1";

    CREDENTIALS_T *server_credentials = credentials_create_password("password");

    DIFFUSION_REMOTE_SERVER_BUILDER_T *builder =
        diffusion_remote_server_builder_init();

    diffusion_remote_server_builder_principal(builder, "admin");
    diffusion_remote_server_builder_credentials(builder, server_credentials);

    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RECONNECTION_TIMEOUT, "120000"
    );
    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_MAXIMUM_QUEUE_SIZE, "1000"
    );
    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_CONNECTION_TIMEOUT, "15000"
    );

    DIFFUSION_REMOTE_SERVER_T *remote_server =
        diffusion_remote_server_builder_create_secondary_initiator(
            builder,
            (char *) remote_server_name,
            "ws://new.server.url.com",
            NULL
        );

    DIFFUSION_CREATE_REMOTE_SERVER_PARAMS_T params_create = {
        .remote_server = remote_server,
        .on_remote_server_created = on_remote_server_created,
        .on_error = on_error
    };

    diffusion_create_remote_server(session, params_create, NULL);
    MUTEX_WAIT

    DIFFUSION_REMOVE_REMOTE_SERVER_PARAMS_T params_remove = {
        .name = (char *) remote_server_name,
        .on_remote_server_removed = on_remote_server_removed,
        .on_error = on_error
    };

    diffusion_remove_remote_server(session, params_remove, NULL);
    MUTEX_BROADCAST
    printf("%s has been removed\n", remote_server_name);

    session_close(session, NULL);
    session_free(session);

    credentials_free(server_credentials);

    MUTEX_TERMINATE
}
