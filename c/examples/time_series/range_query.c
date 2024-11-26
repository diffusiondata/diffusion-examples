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

static int on_append(
        const DIFFUSION_TIME_SERIES_EVENT_METADATA_T *event_metadata,
        void *context)
{
        return HANDLER_SUCCESS;
}

static int on_edit(
    const DIFFUSION_TIME_SERIES_EVENT_METADATA_T *event_metadata,
    void *context)
{
        return HANDLER_SUCCESS;
}

static int on_query_result(
        const DIFFUSION_TIME_SERIES_QUERY_RESULT_T *query_result,
        void *context)
{
    LIST_T *events = diffusion_time_series_query_result_get_events(query_result);
    int size = diffusion_time_series_query_result_get_selected_count(query_result);

    for(int i = 0; i < size; i++) {
        DIFFUSION_TIME_SERIES_EVENT_T *event = list_get_data_indexed(events, i);

        long sequence = diffusion_time_series_event_get_sequence(event);
        long timestamp = diffusion_time_series_event_get_timestamp(event);
        DIFFUSION_VALUE_T *value = diffusion_time_series_event_get_value(event);

        double double_value;
        DIFFUSION_API_ERROR error;
        bool value_converted = read_diffusion_double_value(value, &double_value, &error);
        if (value_converted == false) {
            printf("Error while reading value %d: %d %s\n", i, get_diffusion_api_error_code(error), get_diffusion_api_error_description(error));
            break;
        }

        printf("\t%ld (%ld): %g\n", sequence, timestamp, double_value);

        diffusion_value_free(value);
    }
    MUTEX_BROADCAST
    list_free(events, (void (*)(void *))diffusion_time_series_event_free);
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    char *topic_path = "my/time/series/topic/path";

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    HASH_T *properties = hash_new(5);
    hash_add(properties, DIFFUSION_TIME_SERIES_EVENT_VALUE_TYPE, "double");
    hash_add(properties, DIFFUSION_TIME_SERIES_RETAINED_RANGE, "limit 50 last 120s");
    hash_add(properties, DIFFUSION_TIME_SERIES_SUBSCRIPTION_RANGE, "limit 3");

    utils_create_topic_with_properties(
        session,
        topic_path,
        TOPIC_TYPE_TIME_SERIES,
        DATATYPE_DOUBLE,
        NULL,
        properties
    );
    hash_free(properties, NULL, NULL);

    for (int i = 0; i < 25; i++) {
        double random_value = utils_random_double();

        BUF_T *value = buf_create();
        write_diffusion_double_value(random_value, value);

        DIFFUSION_TIME_SERIES_APPEND_PARAMS_T params = {
            .on_append = on_append,
            .topic_path = topic_path,
            .datatype = DATATYPE_DOUBLE,
            .value = value
        };

        diffusion_time_series_append(session, params, NULL);
        MUTEX_WAIT
        buf_free(value);
    }

    BUF_T *edit_value = buf_create();
    write_diffusion_double_value(3.14, edit_value);
    DIFFUSION_TIME_SERIES_EDIT_PARAMS_T edit_params = {
        .on_edit = on_edit,
        .original_sequence = 10,
        .topic_path = topic_path,
        .datatype = DATATYPE_DOUBLE,
        .value = edit_value
    };
    diffusion_time_series_edit(session, edit_params, NULL);
    MUTEX_WAIT
    buf_free(edit_value);

    DIFFUSION_TIME_SERIES_RANGE_QUERY_T *range_query = diffusion_time_series_range_query();
    diffusion_time_series_range_query_for_values(range_query, NULL);
    diffusion_time_series_range_query_from(range_query, 5, NULL);
    diffusion_time_series_range_query_to(range_query, 15, NULL);

    DIFFUSION_TIME_SERIES_RANGE_QUERY_PARAMS_T query_params = {
        .topic_path = topic_path,
        .range_query = range_query,
        .on_query_result = on_query_result
    };

    diffusion_time_series_select_from(session, query_params, NULL);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);
    diffusion_time_series_range_query_free(range_query);
    MUTEX_TERMINATE
}