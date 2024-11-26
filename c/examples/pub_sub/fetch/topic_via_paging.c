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

SESSION_T *g_session;
const char *g_topic_selector = "?my/topic/path//";
DIFFUSION_FETCH_REQUEST_T *g_fetch_request;


static int on_fetch_result(
    const DIFFUSION_FETCH_RESULT_T *fetch_result,
    void *context)
{
    int results_size = diffusion_fetch_result_size(fetch_result);
    LIST_T *results = diffusion_fetch_result_get_topic_results(fetch_result);

    for (int i = 0; i < results_size; i++) {
        DIFFUSION_TOPIC_RESULT_T *topic_result = list_get_data_indexed(results, i);

        char *topic_path = diffusion_topic_result_get_path(topic_result);
        DIFFUSION_VALUE_T *value = diffusion_topic_result_get_value(topic_result);

        char *string_value;
        read_diffusion_string_value(value, &string_value, NULL);

        printf("%s: %s\n", topic_path, string_value);
    }

    if (diffusion_fetch_result_has_more(fetch_result)) {
        printf("Loading next page.\n");

        DIFFUSION_TOPIC_RESULT_T *last_topic_result =
            list_get_data_last(results);

        diffusion_fetch_request_after(
            g_fetch_request,
            diffusion_topic_result_get_path(last_topic_result),
            NULL
        );

        DIFFUSION_FETCH_REQUEST_PARAMS_T fetch_params = {
            .topic_selector = g_topic_selector,
            .fetch_request = g_fetch_request,
            .on_fetch_result = on_fetch_result
        };

        diffusion_fetch_request_fetch(g_session, fetch_params);
    }
    list_free(results, (void (*)(void *))diffusion_topic_result_free);

    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    g_session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    for (int i = 1; i <= 25; i++) {
        char *topic_path = calloc(100, sizeof(char));
        sprintf(topic_path, "my/topic/path/%d", i);

        char *value = calloc(100, sizeof(char));
        sprintf(value, "diffusion data #%d", i);

        utils_create_string_topic(g_session, topic_path, value);

        free(value);
        free(topic_path);
    }

    DIFFUSION_DATATYPE datatype = DATATYPE_STRING;
    g_fetch_request = diffusion_fetch_request_init(g_session);
    diffusion_fetch_request_with_values(g_fetch_request, &datatype, NULL);
    diffusion_fetch_request_first(g_fetch_request, 10, NULL);

    DIFFUSION_FETCH_REQUEST_PARAMS_T fetch_request_params = {
        .topic_selector = g_topic_selector,
        .fetch_request = g_fetch_request,
        .on_fetch_result = on_fetch_result
    };

    diffusion_fetch_request_fetch(g_session, fetch_request_params);
    MUTEX_WAIT
    diffusion_fetch_request_free(g_fetch_request);


    session_close(g_session, NULL);
    session_free(g_session);
    MUTEX_TERMINATE
}