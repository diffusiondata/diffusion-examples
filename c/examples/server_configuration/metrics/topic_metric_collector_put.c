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

static int on_collector_set(void *context)
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

    char *collector_name = "Topic Metric Collector 1";

    DIFFUSION_TOPIC_METRIC_COLLECTOR_BUILDER_T *builder =
        diffusion_topic_metric_collector_builder_init();

    diffusion_topic_metric_collector_builder_export_to_prometheus(builder, false);
    diffusion_topic_metric_collector_builder_maximum_groups(builder, 10);
    diffusion_topic_metric_collector_builder_group_by_topic_type(builder, true);
    diffusion_topic_metric_collector_builder_group_by_topic_view(builder, true);
    diffusion_topic_metric_collector_builder_group_by_path_prefix_parts(builder, 15);

    DIFFUSION_TOPIC_METRIC_COLLECTOR_T *collector =
        diffusion_topic_metric_collector_builder_create(
            builder, collector_name, "?my/topic//", NULL
        );

    diffusion_topic_metric_collector_builder_free(builder);

    DIFFUSION_METRICS_PUT_TOPIC_METRIC_COLLECTOR_PARAMS_T params = {
        .collector = collector,
        .on_collector_set = on_collector_set,
        .on_error = on_error
    };

    diffusion_metrics_put_topic_metric_collector(session, params, NULL);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);

    MUTEX_TERMINATE
}