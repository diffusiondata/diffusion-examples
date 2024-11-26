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

DIFFUSION_REGISTRATION_T *g_registration;

static void print_topic_notification(
    const char *topic_path,
    DIFFUSION_TOPIC_NOTIFICATION_TYPE_T type,
    bool is_descendant)
{
    HASH_NUM_T *verb_map = hash_num_new(5);
    hash_num_add(verb_map, TOPIC_ADDED, "added");
    hash_num_add(verb_map, TOPIC_SELECTED, "selected");
    hash_num_add(verb_map, TOPIC_REMOVED, "removed");
    hash_num_add(verb_map, TOPIC_DESELECTED, "deselected");

    char *verb = hash_num_get(verb_map, type);
    char *prefix = (is_descendant ? "Descendant ": "");
    printf("%sTopic %s has been %s.\n", prefix, topic_path, verb);
    hash_num_free(verb_map, NULL);
}

static int on_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    printf("On error: %s\n", error->message);
    return HANDLER_SUCCESS;
}

static int on_listener_registered(
    const DIFFUSION_REGISTRATION_T *registration,
    void *context)
{
    g_registration = diffusion_registration_dup(registration);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_topic_notification(
    const char *topic_path,
    const TOPIC_SPECIFICATION_T *specification,
    DIFFUSION_TOPIC_NOTIFICATION_TYPE_T type,
    void *context)
{
    print_topic_notification(topic_path, type, false);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_descendant_notification(
    const char *topic_path,
    DIFFUSION_TOPIC_NOTIFICATION_TYPE_T type,
    void *context)
{
    print_topic_notification(topic_path, type, true);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_selected(void *context)
{
    printf("Topic notification is now selected.\n");
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_deselected(void *context)
{
    printf("Topic notification has been deselected.\n");
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static void on_close(void)
{
    printf("Topic notification listener has been closed.\n");
    MUTEX_BROADCAST
}


void run_example(
    const char *url,
    const char *principal,
    CREDENTIALS_T *credentials)
{
    MUTEX_INIT

    SESSION_T *session = utils_open_session(url, "admin", "password");

    DIFFUSION_TOPIC_NOTIFICATION_LISTENER_T listener = {
        .on_registered = on_listener_registered,
        .on_topic_notification = on_topic_notification,
        .on_descendant_notification = on_descendant_notification,
        .on_close = on_close
    };

    diffusion_topic_notification_add_listener(session, listener, NULL);
    MUTEX_WAIT

    DIFFUSION_TOPIC_NOTIFICATION_REGISTRATION_PARAMS_T params = {
        .on_selected = on_selected,
        .on_deselected = on_deselected,
        .on_error = on_error,
        .registration = g_registration,
        .selector = ">my/topic/path"
    };

    diffusion_topic_notification_registration_select(session, params, NULL);
    MUTEX_WAIT

    utils_create_string_topic(
        session, "my/topic/path", "Good morning"
    );

    utils_create_string_topic(
        session, "my/topic/path/descendant", "Good afternoon"
    );

    utils_create_string_topic(
        session, "other/path/of/the/topic/tree", "This will not generate a notification"
    );

    utils_remove_topic(
        session, "my/topic/path/descendant"
    );

    diffusion_topic_notification_registration_deselect(session, params, NULL);
    MUTEX_WAIT

    session_close(session, NULL);
    session_free(session);


    MUTEX_TERMINATE
}