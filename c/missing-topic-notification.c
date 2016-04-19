/**
 * Copyright © 2016 Push Technology Ltd.
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
 * @author Push Technology Limited
 * @since 5.7
 */

/*
 * This example shows how to register a missing topic notification handler
 * and return a missing topic notification response - calling missing_topic_proceed()
 * once we've created the topic.
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

ARG_OPTS_T arg_opts[] = {
    ARG_OPTS_HELP,
    {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "dpt://localhost:8080"},
    {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
    {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
    {'r', "topic_root", "Topic root to process missing topic notifications on", ARG_OPTIONAL, ARG_HAS_VALUE, "foo"},
    END_OF_ARG_OPTS
};

static int
on_topic_added(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
    puts("Topic added");
    return HANDLER_SUCCESS;
}

static int
on_topic_add_failed(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
    puts("Topic add failed");
    printf("Reason code: %d\n", response->reason);
    return HANDLER_SUCCESS;
}

static int
on_topic_add_discard(SESSION_T *session, const SVC_ADD_TOPIC_RESPONSE_T *response, void *context)
{
    puts("Topic add discarded");
    return HANDLER_SUCCESS;
}

static int 
on_missing_topic(SESSION_T * session, const SVC_MISSING_TOPIC_REQUEST_T * request, void *context)
{
    printf("Missing topic: %s\n", request->topic_selector);

    BUF_T *sample_data_buf = buf_create();
    buf_write_string(sample_data_buf, "Hello, world");

    //Add the topic, that we received the missing topic request from
    ADD_TOPIC_PARAMS_T topic_params = {
        .on_topic_added = on_topic_added,
        .on_topic_add_failed = on_topic_add_failed,
        .on_discard = on_topic_add_discard,
        .topic_path = strdup(request->topic_selector+1),
        .details = create_topic_details_single_value(M_DATA_TYPE_STRING),
        .content = content_create(CONTENT_ENCODING_NONE, sample_data_buf)
    };

    add_topic(session, topic_params);

    //Proceed with the client's subscription to the topic
    missing_topic_proceed(session, (SVC_MISSING_TOPIC_REQUEST_T *) request);

    return HANDLER_SUCCESS;
}

/*
 * Entry point for the example.
 */
int
main(int argc, char **argv)
{
    // Standard command line parsing.
    HASH_T *options = parse_cmdline(argc, argv, arg_opts);
    if(options == NULL || hash_get(options, "help") != NULL) {
        show_usage(argc, argv, arg_opts);
        return 1;
    }
    
    const char *url = hash_get(options, "url");
    const char *principal = hash_get(options, "principal");
    const char *topic_root = hash_get(options, "topic_root");

    CREDENTIALS_T *credentials = NULL;
    const char *password = hash_get(options, "credentials");
    if(password != NULL) {
        credentials = credentials_create_password(password);
    }
    
    SESSION_T *session;
    DIFFUSION_ERROR_T error;
    
    session = session_create(url, principal, credentials, NULL, NULL, &error);
    if(session != NULL) {
        printf("Session created (state=%d, id=%s)\n", session->state, session_id_to_string(session->id));
    }
    else {
        printf("Failed to create session: %s\n", error.message);
        free(error.message);
    }

    //Create the params for the missing topic handler
    MISSING_TOPIC_PARAMS_T handler = {
        .on_missing_topic = on_missing_topic,
        .topic_path = topic_root,
        .context = NULL
    };

    //Register the missing topic handler
    missing_topic_register_handler(session, handler);
    
    sleep(1000);
    
    session_close(session, NULL);
    session_free(session);
    hash_free(options, NULL, free);
    
    return EXIT_SUCCESS;
}
