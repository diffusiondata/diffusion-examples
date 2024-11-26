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

static int on_topic_view_created(
    const DIFFUSION_TOPIC_VIEW_T *topic_view,
    void *context)
{
    char *name = diffusion_topic_view_get_name(topic_view);
    printf("Topic view %s was created.\n", name);
    free(name);
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

    HASH_NUM_T *connection_options = hash_num_new(5);
    hash_num_add(connection_options, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RECONNECTION_TIMEOUT, "120000");
    hash_num_add(connection_options, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_MAXIMUM_QUEUE_SIZE, "1000");
    hash_num_add(connection_options, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_CONNECTION_TIMEOUT, "15000");

    utils_create_remote_server(
        session,
        "Remote Server 1",
        "ws://new.server.url.com",
        "admin",
        "password",
        connection_options
    );
    hash_num_free(connection_options, NULL);

    utils_create_topic_view(
        session,
        "remote_topic_view_1",
        "map my/topic/path from 'Remote Server 1' to views/remote/<path(0)>"
    );


    session_close(session, NULL);
    session_free(session);

    MUTEX_TERMINATE
}