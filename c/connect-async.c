/**
 * Copyright © 2014, 2016 Push Technology Ltd.
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
 * @since 5.0
 */

/*
 * This examples shows how to make an asynchronous connection to Diffusion.
 */
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#include <apr.h>
#include <apr_thread_mutex.h>
#include <apr_thread_cond.h>

#include "diffusion.h"
#include "args.h"

/*
 * Used to synchronise connection callbacks with the main flow of control,
 * so we can close the program in a timely fashion.
 */
apr_pool_t *pool = NULL;
apr_thread_mutex_t *mutex = NULL;
apr_thread_cond_t *cond = NULL;

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        END_OF_ARG_OPTS
};

SESSION_T *g_session = NULL;

/*
 * This callback is used when the session state changes, e.g. when a session
 * moves from a "connecting" to a "connected" state, or from "connected" to
 * "closed".
 */
static void
on_session_state_changed(SESSION_T *session,
        const SESSION_STATE_T old_state,
        const SESSION_STATE_T new_state)
{
        printf("Session state changed from %s (%d) to %s (%d)\n",
               session_state_as_string(old_state), old_state,
               session_state_as_string(new_state), new_state);
}

/*
 * This is the callback that is invoked if the client can successfully
 * connect to Diffusion, and a session instance is ready for use.
 */
static int
on_connected(SESSION_T *session)
{
        char *sid = session_id_to_string(session->id);
        printf("on_connected(), state=%d, session id=%s\n",
               session_state_get(session),
               sid);
        free(sid);

        apr_thread_mutex_lock(mutex);

        g_session = session;

        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

/*
 * This is the callback that is invoked if there is an error connection
 * to the Diffusion instance.
 */
static int
on_error(SESSION_T *session, DIFFUSION_ERROR_T *error)
{
        g_session = session;

        char *sid = session_id_to_string(session->id);
        printf("on_error(), session_id=%s, error=%s\n",
               sid,
               error->message);
        free(sid);
        apr_thread_mutex_lock(mutex);
        apr_thread_cond_broadcast(cond);
        apr_thread_mutex_unlock(mutex);
        return HANDLER_SUCCESS;
}

/*
 * Entry point for the example.
 */
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

        // Setup synchronisation variables.
        apr_initialize();
        apr_pool_create(&pool, NULL);
        apr_thread_mutex_create(&mutex, APR_THREAD_MUTEX_UNNESTED, pool);
        apr_thread_cond_create(&cond, pool);

        DIFFUSION_ERROR_T error = { 0 };

        SESSION_LISTENER_T session_listener = { 0 };
        session_listener.on_state_changed = &on_session_state_changed;

        /*
         * Asynchronous connections have callbacks for notifying that
         * a connection has been made, or that an error occurred.
         */
        SESSION_CREATE_CALLBACK_T *callbacks = calloc(1, sizeof(SESSION_CREATE_CALLBACK_T));
        callbacks->on_connected = &on_connected;
        callbacks->on_error = &on_error;

        RECONNECTION_STRATEGY_T reconnection_strategy = {
                .retry_count = 3,
                .retry_delay = 1000
        };

        /*
         * Although we're connecting asynchronously, we are using a
         * mutex and a condition variable to signal when a
         * session_create_async callback has been invoked, and we can
         * then close & free the session.
         */
        apr_thread_mutex_lock(mutex);
        session_create_async(url, principal, credentials, &session_listener, &reconnection_strategy, callbacks, &error);

        apr_thread_cond_wait(cond, mutex);
        apr_thread_mutex_unlock(mutex);

        /*
         * Close/free session (if we have one) and release resources
         * and memory.
         */
        if(g_session != NULL) {
                session_close(g_session, NULL);
                session_free(g_session);
        }

        apr_thread_mutex_destroy(mutex);
        apr_thread_cond_destroy(cond);
        apr_pool_destroy(pool);
        apr_terminate();

        return EXIT_SUCCESS;
}
