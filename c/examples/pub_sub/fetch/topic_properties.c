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

static int on_fetch_result(
        const DIFFUSION_FETCH_RESULT_T *fetch_result,
        void *context)
{
    LIST_T *results = diffusion_fetch_result_get_topic_results(fetch_result);
    int size = list_get_size(results);

    for (int i = 0; i < size; i++) {
        DIFFUSION_TOPIC_RESULT_T *topic_result = list_get_data_indexed(results, i);

        char *topic_path = diffusion_topic_result_get_path(topic_result);
        TOPIC_SPECIFICATION_T *spec = diffusion_topic_result_get_specification(topic_result);
        HASH_T *properties = topic_specification_get_properties(spec);

        if (properties->size == 0) {
            printf("%s has no non-default properties.\n", topic_path);
        }
        else {
            printf("%s properties:\n", topic_path);

            char **keys = hash_keys(properties);
            for(char **key = keys; *key != NULL; key++) {
                printf("   %s: %s\n", *key, (char *) hash_get(properties, *key));
            }
            free(keys);
        }
        hash_free(properties, NULL, NULL);
    }
    list_free(results, (void (*)(void *))diffusion_topic_result_free);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    HASH_T *properties = hash_new(3);
    hash_add(properties, DIFFUSION_DONT_RETAIN_VALUE, "true");
    hash_add(properties, DIFFUSION_PERSISTENT, "false");
    hash_add(properties, DIFFUSION_PUBLISH_VALUES_ONLY, "true");

    for (int i = 1; i <= 5; i++) {
        char *json_topic_path = calloc(100, sizeof(char));
        sprintf(json_topic_path, "my/topic/path/with/properties/%d", i);

        char *json_value = calloc(100, sizeof(char));
        sprintf(json_value, "{\"diffusion\": \"data #%d\" }", i);

        utils_create_json_topic_with_properties(
            session, json_topic_path, json_value, properties
        );

        free(json_value);
        free(json_topic_path);

        char *string_topic_path = calloc(100, sizeof(char));
        sprintf(string_topic_path, "my/topic/path/with/default/properties/%d", i);

        char *string_value = calloc(100, sizeof(char));
        sprintf(string_value, "diffusion data #%d", i);

        utils_create_string_topic(session, string_topic_path, string_value);

        free(string_value);
        free(string_topic_path);
    }
    hash_free(properties, NULL, NULL);

    const char *topic_selector = "?my/topic/path//";

    SET_T *json_topic_type_set = set_new_int(1);
    int json_topic_type = TOPIC_TYPE_JSON;
    set_add(json_topic_type_set, &json_topic_type);

    DIFFUSION_FETCH_REQUEST_T *json_fetch_request =
        diffusion_fetch_request_init(session);
        diffusion_fetch_request_topic_types(json_fetch_request, json_topic_type_set, NULL);
    diffusion_fetch_request_with_properties(json_fetch_request, NULL);

    DIFFUSION_FETCH_REQUEST_PARAMS_T json_fetch_request_params = {
        .topic_selector = topic_selector,
        .fetch_request = json_fetch_request,
        .on_fetch_result = on_fetch_result
    };
    diffusion_fetch_request_fetch(session, json_fetch_request_params);
    MUTEX_WAIT
    diffusion_fetch_request_free(json_fetch_request);
    set_free(json_topic_type_set);

    SET_T *string_topic_type_set = set_new_int(1);
    int string_topic_type = TOPIC_TYPE_STRING;
    set_add(string_topic_type_set, &string_topic_type);

    DIFFUSION_FETCH_REQUEST_T *string_fetch_request =
        diffusion_fetch_request_init(session);
    diffusion_fetch_request_topic_types(string_fetch_request, string_topic_type_set, NULL);
    diffusion_fetch_request_with_properties(string_fetch_request, NULL);

    DIFFUSION_FETCH_REQUEST_PARAMS_T string_fetch_request_params = {
        .topic_selector = topic_selector,
        .fetch_request = string_fetch_request,
        .on_fetch_result = on_fetch_result
    };
    set_free(string_topic_type_set);

    diffusion_fetch_request_fetch(session, string_fetch_request_params);
    diffusion_fetch_request_free(string_fetch_request);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}