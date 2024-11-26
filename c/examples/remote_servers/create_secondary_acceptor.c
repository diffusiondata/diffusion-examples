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

static int on_remote_server_created(
    DIFFUSION_REMOTE_SERVER_T *remote_server,
    LIST_T *errors,
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

    DIFFUSION_REMOTE_SERVER_BUILDER_T *builder =
        diffusion_remote_server_builder_init();

    CREDENTIALS_T *no_credentials = credentials_create_none();

    diffusion_remote_server_builder_principal(builder, "");
    diffusion_remote_server_builder_credentials(builder, no_credentials);
    diffusion_remote_server_builder_missing_topic_notification_filter(builder, "?abc");

    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_WRITE_TIMEOUT, "2000"
    );
    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_MAXIMUM_QUEUE_SIZE, "1000"
    );
    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_CONNECTION_TIMEOUT, "15000"
    );

    DIFFUSION_REMOTE_SERVER_T *remote_server =
        diffusion_remote_server_builder_create_secondary_acceptor(
            builder,
            "Remote Server 1",
            "ws://new.server.url.com",
            NULL
        );

    DIFFUSION_CREATE_REMOTE_SERVER_PARAMS_T params = {
        .remote_server = remote_server,
        .on_remote_server_created = on_remote_server_created,
        .on_error = on_error
    };

    diffusion_create_remote_server(session, params, NULL);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);

    MUTEX_TERMINATE
}
