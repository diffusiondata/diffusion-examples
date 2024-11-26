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


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT
    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, NULL
    );

    const char *topic_path = "my/topic/path";
    utils_create_int64_topic(
        session, topic_path, 0
    );

    utils_create_topic_view(
        session,
        "topic_view_1",
        "map my/topic/path to views/archive/<path(0)> type TIME_SERIES"
    );

    for (int i = 0; i < 15; i++) {
        utils_set_int64_topic_value(session, topic_path, (int64_t) time(NULL));
        sleep(1);
    }

    utils_time_series_range_query_int64(
        session, "views/archive/my/topic/path"
    );


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}