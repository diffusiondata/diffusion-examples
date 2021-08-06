/**
 * Copyright © 2021 Push Technology Ltd.
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
 *
 * @author Push Technology Limited
 * @since 6.7
 */

/*
 * This example creates, lists and removes topic metric collectors.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "args.h"

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "admin"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

static int on_collector_set(void *context)
{
        printf("Topic metric collector has been set.");
        return HANDLER_SUCCESS;
}


static int on_collector_removed(void *context)
{
        printf("Topic metric collector has been removed.");
        return HANDLER_SUCCESS;
}


static int on_collectors_received(const LIST_T *collectors, void *context)
{
        int total_collectors = list_get_size(collectors);

        printf("Topic metric collectors received:\n");
        for (int i = 0; i < total_collectors; i++) {
            DIFFUSION_TOPIC_METRIC_COLLECTOR_T *collector = list_get_data_indexed(collectors, i);

            char *name;
            diffusion_topic_metric_collector_get_name(collector, &name);
            printf("\t%s\n", name);
            free(name);

            char *topic_selector;
            diffusion_topic_metric_collector_get_topic_selector(collector, &topic_selector);
            printf("\t\tTopic selector: %s\n", topic_selector);
            free(topic_selector);

            bool exports_to_prometheus;
            diffusion_topic_metric_collector_exports_to_prometheus(collector, &exports_to_prometheus);
            printf("\t\tExports to Prometheus: %s\n", exports_to_prometheus ? "YES" : "NO");

            bool group_by_topic_type;
            diffusion_topic_metric_collector_groups_by_topic_type(collector, &group_by_topic_type);
            printf("\t\tGroup by topic type: %s\n", group_by_topic_type ? "YES" : "NO");
        }
        return HANDLER_SUCCESS;
}


static int on_error(
        SESSION_T *session,
        const DIFFUSION_ERROR_T *error)
{
        printf("Error: %s\n", error->message);
        return HANDLER_SUCCESS;
}

/*
 * Program entry point.
 */
int main(int argc, char** argv)
{
        /*
         * Standard command-line parsing.
         */
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if(options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return EXIT_FAILURE;
        }

        const char *url = hash_get(options, "url");
        const char *principal = hash_get(options, "principal");
        CREDENTIALS_T *credentials = NULL;
        const char *password = hash_get(options, "credentials");
        if(password != NULL) {
                credentials = credentials_create_password(password);
        }

        /*
         * Create a session with the Diffusion server.
         */
        SESSION_T *session;
        DIFFUSION_ERROR_T error = { 0 };
        session = session_create(url, principal, credentials, NULL, NULL, &error);
        if(session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * Create a topic metric collector, using its builder
         */
        DIFFUSION_TOPIC_METRIC_COLLECTOR_BUILDER_T *builder =
                diffusion_topic_metric_collector_builder_init();

        builder = diffusion_topic_metric_collector_builder_export_to_prometheus(
                builder,
                true);
        builder = diffusion_topic_metric_collector_builder_group_by_topic_type(
                builder,
                true);

        DIFFUSION_TOPIC_METRIC_COLLECTOR_T *collector =
                diffusion_topic_metric_collector_builder_create_collector(
                        builder,
                        "Collector 1",
                        "*A/B/C/D//");
        /*
         * Put the topic metric collector in the Diffusion server.
         */
        DIFFUSION_METRICS_PUT_TOPIC_METRIC_COLLECTOR_PARAMS_T put_metric_collector_params = {
                .on_collector_set = on_collector_set,
                .on_error = on_error,
                .collector = collector
        };
        diffusion_metrics_put_topic_metric_collector(session, put_metric_collector_params, NULL);
        sleep(2);

        /*
         * List all topic metric collectors present in the server.
         */
        DIFFUSION_METRICS_LIST_TOPIC_METRIC_COLLECTORS_PARAMS_T list_metric_collectors_params = {
                .on_collectors_received = on_collectors_received,
                .on_error = on_error
        };
        diffusion_metrics_list_topic_metric_collectors(session, list_metric_collectors_params, NULL);
        sleep(2);

        /*
         * Remove topic metric collection we created in this example.
         */
        DIFFUSION_METRICS_REMOVE_TOPIC_METRIC_COLLECTOR_PARAMS_T remove_metric_collector_params = {
                .on_collector_removed = on_collector_removed,
                .on_error = on_error,
                .collector_name = "Collector 1",
        };
        diffusion_metrics_remove_topic_metric_collector(session, remove_metric_collector_params, NULL);
        sleep(2);

        /*
         * Close session and free resources.
         */
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);
        diffusion_topic_metric_collector_free(collector);
        diffusion_topic_metric_collector_builder_free(builder);

        return EXIT_SUCCESS;
}
