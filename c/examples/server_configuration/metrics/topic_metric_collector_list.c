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


static int on_topic_metric_collectors_received(
    const LIST_T *collectors,
    void *context)
{
    int total_collectors = list_get_size(collectors);

    printf("Received the following topic metric collectors:\n");
    for (int i = 0; i < total_collectors; i++) {
        DIFFUSION_TOPIC_METRIC_COLLECTOR_T *collector =
            list_get_data_indexed(collectors, i);

        char *name;
        diffusion_topic_metric_collector_get_name (
            collector, &name
        );

        char *topic_selector;
        diffusion_topic_metric_collector_get_topic_selector(
            collector, &topic_selector
        );

        bool groups_by_topic_type;
        diffusion_topic_metric_collector_groups_by_topic_type(
            collector, &groups_by_topic_type
        );

        bool groups_by_topic_view;
        diffusion_topic_metric_collector_groups_by_topic_view(
            collector, &groups_by_topic_view
        );

        bool exports_to_prometheus;
        diffusion_topic_metric_collector_exports_to_prometheus(
            collector, &exports_to_prometheus
        );

        int maximum_groups;
        diffusion_topic_metric_collector_maximum_groups(
            collector, &maximum_groups
        );

        int group_by_path_prefix_parts;
        diffusion_topic_metric_collector_group_by_path_prefix_parts(
            collector, &group_by_path_prefix_parts
        );

        printf(
            "\t%s: %s (%d, %s, %s, %s, %d)\n",
            name, topic_selector, maximum_groups,
            exports_to_prometheus ? "true" : "false",
            groups_by_topic_type ? "true" : "false",
            groups_by_topic_view ? "true" : "false",
            group_by_path_prefix_parts
        );

        free(topic_selector);
        free(name);
    }
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT

    SESSION_T *session = utils_open_session(url, "admin", "password");

    DIFFUSION_TOPIC_METRIC_COLLECTOR_BUILDER_T *builder =
        diffusion_topic_metric_collector_builder_init();

    diffusion_topic_metric_collector_builder_export_to_prometheus(builder, false);
    diffusion_topic_metric_collector_builder_maximum_groups(builder, 10);
    diffusion_topic_metric_collector_builder_group_by_topic_type(builder, true);
    diffusion_topic_metric_collector_builder_group_by_topic_view(builder, true);
    diffusion_topic_metric_collector_builder_group_by_path_prefix_parts(builder, 15);

    DIFFUSION_TOPIC_METRIC_COLLECTOR_T *collector_1 =
        diffusion_topic_metric_collector_builder_create(
            builder, "Topic Metric Collector 1", "?my/topic//", NULL
        );

    DIFFUSION_METRICS_PUT_TOPIC_METRIC_COLLECTOR_PARAMS_T put_params_1 = {
        .collector = collector_1,
        .on_collector_set = on_collector_set,
        .on_error = on_error
    };

    diffusion_metrics_put_topic_metric_collector(session, put_params_1, NULL);
    MUTEX_WAIT

    diffusion_topic_metric_collector_builder_export_to_prometheus(builder, true);
    diffusion_topic_metric_collector_builder_maximum_groups(builder, 250);
    diffusion_topic_metric_collector_builder_group_by_topic_type(builder, false);
    diffusion_topic_metric_collector_builder_group_by_topic_view(builder, true);
    diffusion_topic_metric_collector_builder_group_by_path_prefix_parts(builder, 15);

    DIFFUSION_TOPIC_METRIC_COLLECTOR_T *collector_2 =
        diffusion_topic_metric_collector_builder_create(
            builder, "Topic Metric Collector 2", "?my/topic//", NULL
        );

    diffusion_topic_metric_collector_builder_free(builder);

    DIFFUSION_METRICS_PUT_TOPIC_METRIC_COLLECTOR_PARAMS_T put_params_2 = {
        .collector = collector_2,
        .on_collector_set = on_collector_set,
        .on_error = on_error
    };

    diffusion_metrics_put_topic_metric_collector(session, put_params_2, NULL);
    MUTEX_WAIT

    DIFFUSION_METRICS_LIST_TOPIC_METRIC_COLLECTORS_PARAMS_T list_params = {
        .on_collectors_received = on_topic_metric_collectors_received,
        .on_error = on_error
    };

    diffusion_metrics_list_topic_metric_collectors(session, list_params, NULL);
    MUTEX_WAIT


    session_close(session, NULL);
    session_free(session);

    MUTEX_TERMINATE
}