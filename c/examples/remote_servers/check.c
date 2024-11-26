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


static int on_remote_servers_listed(
        LIST_T *remote_servers,
        void *context)
{
    g_remote_server_names = list_create();

    int size = list_get_size(remote_servers);

    for (int i = 0; i < size; i++) {
        DIFFUSION_REMOTE_SERVER_T *remote_server =
            list_get_data_indexed(remote_servers, i);

        char *name = diffusion_remote_server_get_name(remote_server);
        char *url = diffusion_remote_server_get_url(remote_server);

        printf("\t%s (%s)\n", name, url);

        list_append_last(g_remote_server_names, name);
        free(url);
    }
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


static int on_remote_server_checked(
        DIFFUSION_CHECK_REMOTE_SERVER_RESPONSE_T *response,
        void *context)
{
    DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_T state =
        diffusion_check_remote_server_response_get_state(response);

    switch(state) {
        case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_INACTIVE:
            printf("\tInactive\n");
            break;
        case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_CONNECTED:
            printf("\tConnected\n");
            break;
        case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_RETRYING:
            printf("\tRetrying\n");
            break;
        case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_FAILED:
            printf("\tConnection Failed\n");
            break;
        case DIFFUSION_REMOTE_SERVER_CONNECTION_STATE_MISSING:
            printf("\tMissing\n");
            break;
        default:
            printf("\tUnexpected\n");
            break;
    }

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

    DIFFUSION_REMOTE_SERVER_T *remote_server_1 =
        diffusion_remote_server_builder_create_secondary_initiator(
            builder,
            "Remote Server 1",
            "ws://new.server.url.com",
            NULL
        );

    DIFFUSION_CREATE_REMOTE_SERVER_PARAMS_T params_create_1 = {
        .remote_server = remote_server_1,
        .on_remote_server_created = on_remote_server_created,
        .on_error = on_error
    };

    diffusion_create_remote_server(session, params_create_1, NULL);
    MUTEX_WAIT

    diffusion_remote_server_builder_reset(builder);
    diffusion_remote_server_builder_principal(builder, "control");
    diffusion_remote_server_builder_credentials(builder, server_credentials);

    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_RECONNECTION_TIMEOUT, "6000"
    );
    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_MAXIMUM_QUEUE_SIZE, "10000"
    );
    diffusion_remote_server_builder_connection_option(
        builder, DIFFUSION_REMOTE_SERVER_CONNECTION_OPTION_CONNECTION_TIMEOUT, "5000"
    );

    DIFFUSION_REMOTE_SERVER_T *remote_server_2 =
        diffusion_remote_server_builder_create_secondary_initiator(
            builder,
            "Remote Server 2",
            "ws://another.server.url.com",
            NULL
        );

    DIFFUSION_CREATE_REMOTE_SERVER_PARAMS_T params_create_2 = {
        .remote_server = remote_server_2,
        .on_remote_server_created = on_remote_server_created,
        .on_error = on_error
    };

    diffusion_create_remote_server(session, params_create_2, NULL);
    MUTEX_WAIT

    DIFFUSION_LIST_REMOTE_SERVERS_PARAMS_T params_list = {
        .on_remote_servers_listed = on_remote_servers_listed,
        .on_error = on_error
    };

    diffusion_list_remote_servers(session, params_list, NULL);
    MUTEX_WAIT

    int size = list_get_size(g_remote_server_names);
    for (int i = 0; i < size; i++) {
        char *remote_server_name = list_get_data_indexed(g_remote_server_names, i);

        printf("%s\n", remote_server_name);

        DIFFUSION_CHECK_REMOTE_SERVER_PARAMS_T params_check = {
            .name = remote_server_name,
            .on_remote_server_checked = on_remote_server_checked,
            .on_error = on_error
        };

        diffusion_check_remote_server(session, params_check, NULL);
        MUTEX_WAIT
    }
    list_free(g_remote_server_names, free);


    session_close(session, NULL);
    session_free(session);
    credentials_free(server_credentials);

    MUTEX_TERMINATE
}
