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
    hash_add(properties, DIFFUSION_TIME_SERIES_RETAINED_RANGE, "limit 15 last 10s");
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

    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}