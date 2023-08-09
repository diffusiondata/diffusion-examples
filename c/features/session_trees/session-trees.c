/**
 * Copyright © 2021, 2022 Push Technology Ltd.
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
 * @author DiffusionData Limited
 * @since 6.7
 */

/*
 * This example creates branch mapping tables.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "apr.h"
#include "apr_thread_mutex.h"
#include "apr_thread_cond.h"

#include "diffusion.h"
#include "args.h"

apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

static int on_branch_mapping_table_set(
        void *context)
{
        printf("Branch mapping table has been set.\n");
        return HANDLER_SUCCESS;
}


static int on_session_tree_branches_received(
        const LIST_T *session_tree_branches,
        void *context)
{
        printf("Session tree branches have been received.\n");
        for (int i = 0; i < list_get_size(session_tree_branches); i++) {
                char *session_tree_branch = list_get_data_indexed(session_tree_branches, i);
                printf("\t%d --> %s\n", i+1, session_tree_branch);
        }
        return HANDLER_SUCCESS;
}


static int on_branch_mapping_table_received(
        const DIFFUSION_BRANCH_MAPPING_TABLE_T *table,
        void *context)
{
        char *session_tree_branch =
                diffusion_branch_mapping_table_get_session_tree_branch(
                        (DIFFUSION_BRANCH_MAPPING_TABLE_T *) table);

        LIST_T *branch_mappings =
                diffusion_branch_mapping_table_get_branch_mappings(
                        (DIFFUSION_BRANCH_MAPPING_TABLE_T *) table);

        printf("Branch mapping table for '%s'\n", session_tree_branch);
        for (int i = 0; i < list_get_size(branch_mappings); i++) {
                DIFFUSION_BRANCH_MAPPING_T *mapping = list_get_data_indexed(branch_mappings, i);
                char *session_filter = diffusion_branch_mapping_get_session_filter(mapping);
                char *topic_tree_branch =  diffusion_branch_mapping_get_topic_tree_branch(mapping);

                printf("\t%30s --> %s\n", session_filter, topic_tree_branch);

                free(session_filter);
                free(topic_tree_branch);
        }
        diffusion_branch_mapping_table_free_branch_mappings(branch_mappings);
        free(session_tree_branch);
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
         * Setup for condition variable.
         */
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

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
         * Create a branch mapping table.
         */
        DIFFUSION_BRANCH_MAPPING_TABLE_BUILDER_T *builder =
                diffusion_branch_mapping_table_builder_init();

        builder = diffusion_branch_mapping_table_builder_add_branch_mapping(
                builder,
                "$Principal is 'control'",
                "target/content/control");
        builder = diffusion_branch_mapping_table_builder_add_branch_mapping(
                builder,
                "all",
                "target/content/other");

        DIFFUSION_BRANCH_MAPPING_TABLE_T *table =
                diffusion_branch_mapping_table_builder_create_table(builder, "public/content");

        /*
         * Put the branch mapping table in the Diffusion server.
         */
        DIFFUSION_SESSION_TREES_PUT_BRANCH_MAPPING_TABLE_PARAMS_T put_branch_mapping_table_params = {
                .on_table_set = on_branch_mapping_table_set,
                .on_error = on_error,
                .table = table
        };
        diffusion_session_trees_put_branch_mapping_table(session, put_branch_mapping_table_params, NULL);
        sleep(2);

        /*
         * Retrieve all session tree branches with mappings
         */
         DIFFUSION_SESSION_TREES_GET_SESSION_TREE_BRANCHES_PARAMS_T get_session_tree_branches_params = {
                 .on_session_tree_branches_received = on_session_tree_branches_received,
                 .on_error = on_error
         };
         diffusion_session_trees_get_session_tree_branches(session, get_session_tree_branches_params, NULL);
         sleep(2);

        /*
         * Retrieve branch mapping table for session tree branch
         */
         DIFFUSION_SESSION_TREES_GET_BRANCH_MAPPING_TABLE_PARAMS_T get_table_params = {
                 .on_table_received = on_branch_mapping_table_received,
                 .on_error = on_error,
                 .session_tree_branch = "public/content"
         };
         diffusion_session_trees_get_branch_mapping_table(session, get_table_params, NULL);
         sleep(2);

        /*
         * Close session and free resources.
         */
        session_close(session, NULL);
        session_free(session);

        credentials_free(credentials);
        hash_free(options, NULL, free);
        diffusion_branch_mapping_table_free(table);
        diffusion_branch_mapping_table_builder_free(builder);

        apr_thread_mutex_destroy(mutex);
        apr_thread_cond_destroy(cond);
        apr_pool_destroy(pool);
        apr_terminate();

        return EXIT_SUCCESS;
}
