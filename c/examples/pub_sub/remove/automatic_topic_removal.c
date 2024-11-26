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

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/time/after",
        "when time after 'Tue, 4 May 2077 11:05:30 GMT'"
    );

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/subscriptions",
        "when subscriptions < 1 for 10m"
    );

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/local/subscriptions",
        "when local subscriptions < 1 for 10m"
    );

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/no/updates",
        "when no updates for 10m"
    );

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/no/session",
        "when no session has '$Principal is \"client\"' for 1h"
    );

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/no/local/session",
        "when no local session has 'Department is \"Accounts\"' for 1h after 1d"
    );

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/subcriptions/or/updates",
        "when subscriptions < 1 for 10m or no updates for 20m"
    );

    utils_create_json_topic_with_topic_removal_policy(
        session,
        "my/topic/path/to/be/removed/subcriptions/and/updates",
        "when subscriptions < 1 for 10m and no updates for 20m"
    );


    session_close(session, NULL);
    session_free(session);
    MUTEX_TERMINATE
}