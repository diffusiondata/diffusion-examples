/**
 * Copyright © 2014, 2022 Push Technology Ltd.
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
 * @since 5.5
 */

/*
 * This examples demonstrates how to interact with the system
 * authentication store.
 */

#include <stdio.h>

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

/*
 * This callback is invoked when the system authentication store is
 * received, and prints the contents of the store.
 */
int
on_get_system_authentication_store(SESSION_T *session,
                                   const SYSTEM_AUTHENTICATION_STORE_T store,
                                   void *context)
{
        puts("on_get_system_authentication_store()");

        printf("Got %ld principals\n", store.system_principals->size);

        char **names = get_principal_names(store);
        for(char **name = names; *name != NULL; name++) {
                printf("Principal: %s\n", *name);

                char **roles = get_roles_for_principal(store, *name);
                for(char **role = roles; *role != NULL; role++) {
                    printf("  |- Role: %s\n", *role);
                }
                free(roles);
        }
        free(names);

        switch(store.anonymous_connection_action) {
        case ANONYMOUS_CONNECTION_ACTION_ALLOW:
                puts("Allow anonymous connections");
                break;
        case ANONYMOUS_CONNECTION_ACTION_DENY:
                puts("Deny anonymous connections");
                break;
        case ANONYMOUS_CONNECTION_ACTION_ABSTAIN:
                puts("Abstain from making anonymous connection decision");
                break;
        }

        puts("Anonymous connection roles:");
        char **roles = get_anonymous_roles(store);
        for(char **role = roles; *role != NULL; role++) {
                printf("  |- Role: %s\n", *role);
        }
        free(roles);

        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);

        return HANDLER_SUCCESS;
}

int
main(int argc, char **argv)
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
         * Setup for condition variable
         */
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

        /*
         * Create a session with Diffusion.
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
         * Request the system authentication store.
         */
        const GET_SYSTEM_AUTHENTICATION_STORE_PARAMS_T params = {
                .on_get = on_get_system_authentication_store
        };

        apr_thread_mutex_lock(mutex);

        get_system_authentication_store(session, params);

        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Close the session and tidy up.
         */
        session_close(session, NULL);
        session_free(session);
        hash_free(options, NULL, free);

        apr_thread_mutex_destroy(mutex);
        apr_thread_cond_destroy(cond);
        apr_pool_destroy(pool);
        apr_terminate();

        return EXIT_SUCCESS;
}
