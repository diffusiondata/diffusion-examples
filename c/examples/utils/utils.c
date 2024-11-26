#include "utils.h"
#include "regexp9.h"


static int on_topic_update_add_and_set(
    DIFFUSION_TOPIC_CREATION_RESULT_T result,
    void *context)
{
    if (result == TOPIC_CREATED) {
        printf("Topic has been created.\n");
    }
    else if (result == TOPIC_EXISTS) {
        printf("Topic already exists.\n");
    }
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_topic_added_with_specification(
    SESSION_T *session,
    TOPIC_ADD_RESULT_CODE result_code,
    void *context)
{
    if (result_code == TOPIC_ADD_RESULT_CREATED) {
        printf("Topic has been created.\n");
    }
    else if (result_code == TOPIC_ADD_RESULT_EXISTS) {
        printf("Topic already exists.\n");
    }
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_topic_add_failed_with_specification(
        SESSION_T *session,
        TOPIC_ADD_FAIL_RESULT_CODE result_code,
        const DIFFUSION_ERROR_T *error,
        void *context)
{
    printf("Topic creation failed: %d %d %s\n", result_code, error->code, error->message);
    return HANDLER_SUCCESS;
}

static int on_error(
    SESSION_T *session,
    const DIFFUSION_ERROR_T *error)
{
    printf("On error: %s\n", error->message);
    return HANDLER_SUCCESS;
}

static int on_topic_view_created(
    const DIFFUSION_TOPIC_VIEW_T *topic_view,
    void *context)
{
    char *name = diffusion_topic_view_get_name(topic_view);
    printf("Topic view %s was created.\n", name);
    free(name);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_subscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    void *context)
{
    printf("Subscribed to %s.\n", topic_path);
    return HANDLER_SUCCESS;
}

static int on_unsubscription(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    NOTIFY_UNSUBSCRIPTION_REASON_T reason,
    void *context)
{
    HASH_NUM_T *unsubscription_reason_map = hash_num_new(10);
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_CONTROL, "request by another client or server");
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_REMOVAL, "topic being removed");
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_AUTHORIZATION, "loss of authorisation to access topic");
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_UNKNOWN_UNSUBSCRIBE_REASON, "unkown reasons");
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_BACK_PRESSURE, "significant backlog of message on the server");
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_BRANCH_MAPPINGS, "session trees branch mapping alterations");
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_SUBSCRIPTION_REFRESH, "subscription refresh");
    hash_num_add(unsubscription_reason_map, UNSUBSCRIPTION_REASON_STREAM_CHANGE, "fallback stream being unsubscribed as new value stream was added");

    char *unsubscription_reason = hash_num_get(unsubscription_reason_map, reason);

    printf("Unsubscribed from %s due to %s.\n", topic_path, unsubscription_reason);
    return HANDLER_SUCCESS;
}

static int on_json_value(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    DIFFUSION_DATATYPE datatype,
    const DIFFUSION_VALUE_T *const old_value,
    const DIFFUSION_VALUE_T *const new_value,
    void *context)
{
    char *old_value_json_string;
    if (old_value == NULL) {
        old_value_json_string = "NULL";
    }
    else {
        bool read_old = to_diffusion_json_string(old_value, &old_value_json_string, NULL);
        if (read_old == false) {
            printf("error while reading old value\n");
            return HANDLER_SUCCESS;
        }
    }

    char *new_value_json_string;
    if (new_value == NULL) {
        new_value_json_string = "NULL";
    }
    else {
        bool read_new = to_diffusion_json_string(new_value, &new_value_json_string, NULL);
        if (read_new == false) {
            printf("error while reading new value\n");
            return HANDLER_SUCCESS;
        }
    }

    printf(
        "%s changed from %s to %s.\n",
        topic_path, old_value_json_string, new_value_json_string
    );

    if (old_value != NULL) {
        free(old_value_json_string);
    }

    if (new_value != NULL) {
        free(new_value_json_string);
    }
    return HANDLER_SUCCESS;
}

static int on_string_value(
    const char *const topic_path,
    const TOPIC_SPECIFICATION_T *const specification,
    DIFFUSION_DATATYPE datatype,
    const DIFFUSION_VALUE_T *const old_value,
    const DIFFUSION_VALUE_T *const new_value,
    void *context)
{
    char *old_value_string;
    if (old_value == NULL) {
        old_value_string = "NULL";
    }
    else {
        bool read_old = read_diffusion_string_value(old_value, &old_value_string, NULL);
        if (read_old == false) {
            printf("error while reading old value\n");
            return HANDLER_SUCCESS;
        }
    }

    char *new_value_string;
    if (new_value == NULL) {
        new_value_string = "NULL";
    }
    else {
        bool read_new = read_diffusion_string_value(new_value, &new_value_string, NULL);
        if (read_new == false) {
            printf("error while reading new value\n");
            return HANDLER_SUCCESS;
        }
    }

    printf(
        "%s changed from %s to %s.\n",
        topic_path, old_value_string, new_value_string
    );

    if (old_value != NULL) {
        free(old_value_string);
    }

    if (new_value != NULL) {
        free(new_value_string);
    }
    return HANDLER_SUCCESS;
}

static int on_subscribe(
    SESSION_T*session,
    void *context)
{
    printf("Subscription request received and approved by the server.\n");
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_remote_server_created(
    DIFFUSION_REMOTE_SERVER_T *remote_server,
    LIST_T *errors,
    void *context)
{
    char *name = diffusion_remote_server_get_name(remote_server);
    printf("Remote server %s created.\n", name);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_fetch_result_topic_properties(
        const DIFFUSION_FETCH_RESULT_T *fetch_result,
        void *context)
{
    LIST_T *results = diffusion_fetch_result_get_topic_results(fetch_result);
    int size = list_get_size(results);

    for (int i = 0; i < size; i++) {
        DIFFUSION_TOPIC_RESULT_T *topic_result = list_get_data_indexed(results, i);

        char *topic_path = diffusion_topic_result_get_path(topic_result);
        TOPIC_SPECIFICATION_T *spec = diffusion_topic_result_get_specification(topic_result);
        HASH_T *properties = topic_specification_get_properties(spec);
        if (properties == NULL) {
            continue;
        }

        if (properties->size == 0) {
            printf("%s has no non-default properties.\n", topic_path);
        }
        else {
            printf("%s properties:\n", topic_path);

            char **keys = hash_keys(properties);
            for(char **key = keys; *key != NULL; key++) {
                printf("   %s: %s\n", *key, (char *) hash_get(properties, *key));
            }
            free(keys);
        }
        hash_free(properties, NULL, NULL);
    }
    list_free(results, (void (*)(void *))diffusion_topic_result_free);
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_topic_update(
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_topic_removed(
    SESSION_T *session,
    const DIFFUSION_TOPIC_REMOVAL_RESULT_T *response,
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}


static int on_range_query_result_int64(
        const DIFFUSION_TIME_SERIES_QUERY_RESULT_T *query_result,
        void *context)
{
    LIST_T *events = diffusion_time_series_query_result_get_events(query_result);
    int size = diffusion_time_series_query_result_get_selected_count(query_result);

    for(int i = 0; i < size; i++) {
        DIFFUSION_TIME_SERIES_EVENT_T *event = list_get_data_indexed(events, i);

        long sequence = diffusion_time_series_event_get_sequence(event);
        long timestamp = diffusion_time_series_event_get_timestamp(event);
        DIFFUSION_VALUE_T *value = diffusion_time_series_event_get_value(event);

        int64_t int64_value;
        DIFFUSION_API_ERROR error;
        bool value_converted = read_diffusion_int64_value(value, &int64_value, &error);
        if (value_converted == false) {
            printf("Error while reading value %d: %d %s\n", i, get_diffusion_api_error_code(error), get_diffusion_api_error_description(error));
            break;
        }

        printf("\t%ld (%ld): %lld\n", sequence, timestamp, (long long) int64_value);

        diffusion_value_free(value);
    }
    MUTEX_BROADCAST
    list_free(events, (void (*)(void *))diffusion_time_series_event_free);
    return HANDLER_SUCCESS;
}

static int on_branch_mapping_table_set(
        void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_session_properties(
    SESSION_T *session,
    const SVC_GET_SESSION_PROPERTIES_RESPONSE_T *response,
    void *context)
{
    if (context != NULL)
    {
        printf(
            "Printing session properties that match '%s' pattern:\n",
            (char *) context
        );
    }
    else {
        printf("Received the following session properties:\n");
    }
    utils_print_hash(
        response->properties, context, utils_print_string
    );
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_collector_removed(void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_get_security_store(
    SESSION_T *session,
    const SECURITY_STORE_T store,
    void *context)
{
    if (context != NULL) {
        SECURITY_STORE_T **store_ptr = (SECURITY_STORE_T **) context;
        *store_ptr = security_store_dup(&store);
    }
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_topic_view_removed(
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_remote_server_removed(void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_lock_released(
    bool lock_owned,
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_security_store_updated(
    SESSION_T *session,
    const LIST_T *error_report,
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_system_authentication_store_updated(
    SESSION_T *session,
    const LIST_T *error_report,
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

static int on_json_patch_result(
    const DIFFUSION_JSON_PATCH_RESULT_T *result,
    void *context)
{
    MUTEX_BROADCAST
    return HANDLER_SUCCESS;
}

SESSION_T *utils_open_session(
    const char *url,
    const char *principal,
    const char *password)
{
    CREDENTIALS_T *credentials = (password == NULL) ?
        credentials_create_none() :
        credentials_create_password(password);

    DIFFUSION_ERROR_T error = { 0 };

    SESSION_T *session = session_create(
        url, principal, credentials, NULL, NULL, &error
    );
    if (session == NULL) {
        LOG("Failed to open session: %s\n", error.message);
        free(error.message);
    }
    credentials_free(credentials);
    return session;
}

void utils_remove_topic(
    SESSION_T *session,
    const char *topic_selector)
{
    TOPIC_REMOVAL_PARAMS_T remove_params = {
        .topic_selector = topic_selector,
        .on_removed = on_topic_removed,
        .on_error = on_error
    };

    topic_removal(session, remove_params);
    MUTEX_WAIT
}

void utils_create_topic_with_properties(
    SESSION_T *session,
    const char *topic_path,
    TOPIC_TYPE_T topic_type,
    DIFFUSION_DATATYPE datatype,
    BUF_T *value,
    HASH_T *properties)
{
    printf("Creating %s\n", topic_path);

    TOPIC_SPECIFICATION_T *topic_specification =
        (properties == NULL)
            ? topic_specification_init(topic_type)
            : topic_specification_init_with_properties(topic_type, properties);

    if (value != NULL) {
        // Add and set
        DIFFUSION_TOPIC_UPDATE_ADD_AND_SET_PARAMS_T add_and_set_params = {
            .datatype = datatype,
            .on_topic_update_add_and_set = on_topic_update_add_and_set,
            .topic_path = topic_path,
            .specification = topic_specification,
            .update = value,
            .on_error = on_error
        };
        diffusion_topic_update_add_and_set(session, add_and_set_params);
    }
    else {
        // Add only
        ADD_TOPIC_CALLBACK_T callback = {
            .on_topic_added_with_specification = on_topic_added_with_specification,
            .on_topic_add_failed_with_specification = on_topic_add_failed_with_specification
        };
        add_topic_from_specification(
            session, topic_path, topic_specification, callback
        );
    }
    MUTEX_WAIT
    topic_specification_free(topic_specification);
}


void utils_set_topic_value(
    SESSION_T *session,
    const char *topic_path,
    DIFFUSION_DATATYPE datatype,
    BUF_T *value)
{
     DIFFUSION_TOPIC_UPDATE_SET_PARAMS_T params = {
        .datatype = datatype,
        .on_topic_update = on_topic_update,
        .topic_path = topic_path,
        .update = value,
        .on_error = on_error
    };

    diffusion_topic_update_set(session, params);
    MUTEX_WAIT
}

void utils_create_json_topic_with_topic_removal_policy(
    SESSION_T *session,
    const char *topic_path,
    const char *topic_removal_policy)
{
    HASH_T *properties = hash_new(3);
    hash_add(properties, DIFFUSION_REMOVAL, topic_removal_policy);

    utils_create_json_topic_with_properties(session, topic_path, NULL, properties);
    hash_free(properties, NULL, NULL);
}


void utils_create_json_topic_with_properties(
    SESSION_T *session,
    const char *topic_path,
    const char *json_string,
    HASH_T *properties)
{
    BUF_T *value = NULL;
    if (json_string != NULL) {
        value = buf_create();
        write_diffusion_json_value(json_string, value);
    }

    utils_create_topic_with_properties(
        session, topic_path, TOPIC_TYPE_JSON, DATATYPE_JSON, value, properties
    );

    buf_free(value);
}


void utils_create_json_topic(
    SESSION_T *session,
    const char *topic_path,
    const char *json_string)
{
    utils_create_json_topic_with_properties(
        session, topic_path, json_string, NULL
    );
}


void utils_set_json_topic_value(
    SESSION_T *session,
    const char *topic_path,
    const char *json_string)
{
    BUF_T *value = buf_create();
    write_diffusion_json_value(json_string, value);

    utils_set_topic_value(session, topic_path, DATATYPE_JSON, value);

    buf_free(value);
}


void utils_create_int64_topic(
    SESSION_T *session,
    const char *topic_path,
    int64_t int64_value)
{
    BUF_T *value = buf_create();
    write_diffusion_int64_value(int64_value, value);

    utils_create_topic_with_properties(
        session, topic_path, TOPIC_TYPE_INT64, DATATYPE_INT64, value, NULL
    );

    buf_free(value);
}


void utils_set_int64_topic_value(
    SESSION_T *session,
    const char *topic_path,
    int64_t int64_value)
{
    BUF_T *value = buf_create();
    write_diffusion_int64_value(int64_value, value);

    utils_set_topic_value(session, topic_path, DATATYPE_INT64, value);

    buf_free(value);
}


void utils_create_string_topic_with_properties(
    SESSION_T *session,
    const char *topic_path,
    const char *value_string,
    HASH_T *properties)
{
    BUF_T *value = buf_create();
    write_diffusion_string_value(value_string, value);

    utils_create_topic_with_properties(
        session, topic_path, TOPIC_TYPE_STRING, DATATYPE_STRING, value, properties
    );

    buf_free(value);
}


void utils_create_string_topic(
    SESSION_T *session,
    const char *topic_path,
    const char *value_string)
{
    utils_create_string_topic_with_properties(
        session, topic_path, value_string, NULL
    );
}


void utils_create_topic_view(
    SESSION_T *session,
    const char *name,
    const char *specification)
{
    DIFFUSION_CREATE_TOPIC_VIEW_PARAMS_T create_topic_view_params = {
        .view = name,
        .specification = specification,
        .on_topic_view_created = on_topic_view_created,
        .on_error = on_error
    };
    diffusion_topic_views_create_topic_view(session, create_topic_view_params, NULL);
    MUTEX_WAIT
}


void utils_create_remote_server(
    SESSION_T *session,
    char *name,
    char *url,
    char *principal,
    char *password,
    HASH_NUM_T *connection_options)
{
    CREDENTIALS_T *credentials = (
        (password == NULL)
        ? credentials_create_none()
        : credentials_create_password(password)
    );

    DIFFUSION_REMOTE_SERVER_BUILDER_T *builder = diffusion_remote_server_builder_init();
    diffusion_remote_server_builder_principal(builder, principal);
    diffusion_remote_server_builder_credentials(builder, credentials);
    diffusion_remote_server_builder_connection_options(builder, connection_options);

    DIFFUSION_REMOTE_SERVER_T *remote_server =
        diffusion_remote_server_builder_create_secondary_initiator(
            builder, name, url, NULL
        );

    DIFFUSION_CREATE_REMOTE_SERVER_PARAMS_T params = {
        .remote_server = remote_server,
        .on_remote_server_created = on_remote_server_created,
        .on_error = on_error
    };
    diffusion_create_remote_server(session, params, NULL);
    MUTEX_WAIT
    credentials_free(credentials);
    diffusion_remote_server_free(remote_server);
    diffusion_remote_server_builder_free(builder);
}


VALUE_STREAM_T *utils_create_value_stream(
    SESSION_T *session,
    const char *topic_selector,
    DIFFUSION_DATATYPE datatype)
{
    VALUE_STREAM_T *value_stream = calloc(1, sizeof(VALUE_STREAM_T));
    value_stream->datatype = datatype;
    value_stream->on_subscription = on_subscription;
    value_stream->on_unsubscription = on_unsubscription;

    if (datatype == DATATYPE_JSON) {
        value_stream->on_value = on_json_value;
    }
    else if (datatype == DATATYPE_STRING) {
        value_stream->on_value = on_string_value;
    }
    else {
        printf("Missing on_value function for chosen datatype\n");
        free(value_stream);
        return NULL;
    }

    add_stream(session, topic_selector, value_stream);

    return value_stream;
}


VALUE_STREAM_T *utils_subscribe(
    SESSION_T *session,
    const char *topic_selector,
    DIFFUSION_DATATYPE datatype)
{
    VALUE_STREAM_T *value_stream = utils_create_value_stream(
        session, topic_selector, datatype
    );

    if (value_stream == NULL) {
        return  NULL;
    }

    SUBSCRIPTION_PARAMS_T params = {
        .topic_selector = topic_selector,
        .on_subscribe = on_subscribe
    };

    subscribe(session, params);
    MUTEX_WAIT
    return value_stream;
}


void utils_get_topic_properties(
    SESSION_T *session,
    const char *topic_selector)
{
    DIFFUSION_FETCH_REQUEST_T *fetch_request = diffusion_fetch_request_init(session);
    diffusion_fetch_request_with_properties(fetch_request, NULL);

    DIFFUSION_FETCH_REQUEST_PARAMS_T fetch_request_params = {
        .topic_selector = topic_selector,
        .fetch_request = fetch_request,
        .on_fetch_result = on_fetch_result_topic_properties
    };

    diffusion_fetch_request_fetch(session, fetch_request_params);
    MUTEX_WAIT
    diffusion_fetch_request_free(fetch_request);
}


void utils_time_series_range_query_int64(
    SESSION_T *session,
    const char *topic_path)
{
    DIFFUSION_TIME_SERIES_RANGE_QUERY_T *range_query =
        diffusion_time_series_range_query();
    diffusion_time_series_range_query_for_values(range_query, NULL);

    DIFFUSION_TIME_SERIES_RANGE_QUERY_PARAMS_T query_params = {
        .topic_path = topic_path,
        .range_query = range_query,
        .on_query_result = on_range_query_result_int64
    };

    diffusion_time_series_select_from(session, query_params, NULL);
    MUTEX_WAIT
    diffusion_time_series_range_query_free(range_query);
}


void utils_build_branch_mapping_table(
    SESSION_T * session,
    const char *topic_path,
    size_t mappings_length,
    const UTILS_SESSION_TREES_MAPPING_T mappings[])
{
    DIFFUSION_BRANCH_MAPPING_TABLE_BUILDER_T *builder =
        diffusion_branch_mapping_table_builder_init();

    if (mappings_length > 0) {
        for (int i = 0; i < mappings_length; i++) {
            UTILS_SESSION_TREES_MAPPING_T entry = mappings[i];
            diffusion_branch_mapping_table_builder_add_branch_mapping(
                builder, entry.rule, entry.topic_path
            );
        }
    }

    DIFFUSION_BRANCH_MAPPING_TABLE_T *table =
        diffusion_branch_mapping_table_builder_create_table(
            builder, (char *) topic_path
        );

    DIFFUSION_SESSION_TREES_PUT_BRANCH_MAPPING_TABLE_PARAMS_T params = {
        .on_table_set = on_branch_mapping_table_set,
        .on_error = on_error,
        .table = table
    };

    diffusion_session_trees_put_branch_mapping_table(session, params, NULL);
    MUTEX_WAIT

    diffusion_branch_mapping_table_free(table);
    diffusion_branch_mapping_table_builder_free(builder);
}


void utils_remove_branch_mapping_table(
    SESSION_T *session,
    const char *topic_path)
{
    utils_build_branch_mapping_table(session, topic_path, 0, NULL);
}


void utils_print_session_properties(
    SESSION_T *session,
    SESSION_ID_T *session_id,
    const char *properties_regex)
{
    SET_T *required_properties = set_new_string(1);
    set_add(required_properties, PROPERTIES_SELECTOR_ALL_FIXED_PROPERTIES);
    set_add(required_properties, PROPERTIES_SELECTOR_ALL_USER_PROPERTIES);

    char *regex_copy = (properties_regex == NULL) ? NULL : strdup(properties_regex);

    GET_SESSION_PROPERTIES_PARAMS_T get_session_properties_params = {
        .on_session_properties = on_session_properties,
        .on_error = on_error,
        .session_id = session_id,
        .required_properties = required_properties,
        .context = regex_copy
    };

    get_session_properties(session, get_session_properties_params);
    MUTEX_WAIT
    set_free(required_properties);
    free(regex_copy);
}


void utils_remove_session_metric_collector(
    SESSION_T *session,
    const char *name)
{
    char *name_copy = strdup(name);

    DIFFUSION_METRICS_REMOVE_SESSION_METRIC_COLLECTOR_PARAMS_T remove_params = {
        .collector_name = name_copy,
        .on_collector_removed = on_collector_removed,
        .on_error = on_error
    };

    diffusion_metrics_remove_session_metric_collector(session, remove_params, NULL);
    MUTEX_WAIT

    free(name_copy);
}


void utils_remove_topic_metric_collector(
    SESSION_T *session,
    const char *name)
{
    char *name_copy = strdup(name);

    DIFFUSION_METRICS_REMOVE_TOPIC_METRIC_COLLECTOR_PARAMS_T remove_params = {
        .collector_name = name_copy,
        .on_collector_removed = on_collector_removed,
        .on_error = on_error
    };

    diffusion_metrics_remove_topic_metric_collector(session, remove_params, NULL);
    MUTEX_WAIT

    free(name_copy);
}


SECURITY_STORE_T *utils_get_security_store(
    SESSION_T *session)
{
    SECURITY_STORE_T *store;

    const GET_SECURITY_STORE_PARAMS_T params = {
        .on_get = on_get_security_store,
        .on_error = on_error,
        .context = &store
    };

    get_security_store(session, params);
    MUTEX_WAIT

    return store;
}

void utils_remove_role(
    SESSION_T *session,
    const char *role_name)
{
    if (session == NULL) {
        return;
    }

    SCRIPT_T *script_remove_role = script_create();

    SET_T *set_no_permissions = set_new_int(2);
    update_security_store_default_path_permissions(
        script_remove_role, role_name, set_no_permissions
    );
    update_security_store_global_role_permissions(
        script_remove_role, role_name, set_no_permissions
    );
    set_free(set_no_permissions);

    LIST_T *list_no_roles = list_create();
    update_security_store_include_roles(
        script_remove_role, role_name, list_no_roles
    );
    list_free(list_no_roles, NULL);

    utils_update_security_store(session, script_remove_role);
    script_free(script_remove_role);
}


void utils_add_role_from_security_store(
    SESSION_T *session,
    SECURITY_STORE_T *store,
    const char *role_name)
{
    if (session == NULL || store == NULL || store->roles == NULL) {
        return;
    }

    SECURITY_STORE_ROLE_T *role =
        (SECURITY_STORE_ROLE_T *) hash_get(store->roles, role_name);

    if (role == NULL) {
        return;
    }

    SCRIPT_T *script_add_role = script_create();

    LIST_T *list_included_roles = utils_set_to_list(
        role->included_roles
    );
    update_security_store_include_roles(
        script_add_role, role_name, list_included_roles
    );
    list_free(list_included_roles, NULL);

    update_security_store_global_role_permissions(
        script_add_role, role_name, role->global_permissions
    );

    update_security_store_default_path_permissions(
        script_add_role, role_name, role->default_path_permissions
    );

    if (role->path_permissions != NULL) {
        char **keys = hash_keys(role->path_permissions);
        for(char **ptr = keys; *ptr != NULL; ptr++) {
            PATH_PERMISSIONS_T **arr_permissions =
                (PATH_PERMISSIONS_T **) hash_get(role->path_permissions, *ptr);

            SET_T *set_path_permissions = set_new_int(3);
            for(PATH_PERMISSIONS_T **arr_ptr = arr_permissions;
                *arr_ptr != NULL;
                arr_ptr++) {
                    set_add(set_path_permissions, *arr_ptr);
            }
            update_security_store_path_permissions(
                script_add_role, role_name, *ptr, set_path_permissions
            );
            set_free(set_path_permissions);
        }
        free(keys);
    }

    if (role->locking_principal != NULL) {
        update_security_store_role_locked_by_principal(
            script_add_role, role_name, role->locking_principal
        );
    }

    utils_update_security_store(session, script_add_role);
    script_free(script_add_role);
}


void utils_print_security_store(
    SESSION_T *session,
    const char *selected_role)
{
    SECURITY_STORE_T *store = utils_get_security_store(session);

    // Show all available information
    printf("Roles:\n");
    utils_print_security_roles(store->roles, selected_role);

    char *anon_roles = utils_set_to_string(
        store->anon_roles_default,
        utils_print_string
    );
    printf("Anonymous session roles: %s\n", anon_roles);
    free(anon_roles);

    char *named_roles = utils_set_to_string(
        store->named_roles_default,
        utils_print_string
    );
    printf("Named session roles: %s\n", named_roles);
    free(named_roles);

    char *isolated_paths = utils_set_to_string(
        store->isolated_paths,
        utils_print_string
    );
    printf("Isolated paths: %s\n", isolated_paths);
    free(isolated_paths);

    security_store_free(store);
}


void utils_remove_topic_view(
    SESSION_T *session,
    const char *topic_view_name)
{
    DIFFUSION_REMOVE_TOPIC_VIEW_PARAMS_T remove_params = {
        .view = topic_view_name,
        .on_topic_view_removed = on_topic_view_removed,
        .on_error = on_error
    };

    diffusion_topic_views_remove_topic_view(session, remove_params, NULL);
    MUTEX_WAIT
}


void utils_remove_remote_server(
    SESSION_T *session,
    const char *remote_server_name)
{
    DIFFUSION_REMOVE_REMOTE_SERVER_PARAMS_T params = {
        .name = (char *) remote_server_name,
        .on_remote_server_removed = on_remote_server_removed,
        .on_error = on_error
    };

    diffusion_remove_remote_server(session, params, NULL);
    MUTEX_WAIT
}

void utils_release_lock(
    SESSION_T *session,
    DIFFUSION_SESSION_LOCK_T *lock)
{
    DIFFUSION_SESSION_LOCK_UNLOCK_PARAMS_T unlock_params = {
        .on_unlock = on_lock_released,
        .on_error = on_error
    };

    diffusion_session_lock_unlock(session, lock, unlock_params);
    MUTEX_WAIT
}

void utils_update_security_store(
    SESSION_T *session,
    SCRIPT_T *script)
{
    const UPDATE_SECURITY_STORE_PARAMS_T params = {
        .on_update = on_security_store_updated,
        .on_error = on_error,
        .update_script = script
    };

    update_security_store(session, params);
    MUTEX_WAIT
}


void utils_update_system_authentication_store(
    SESSION_T *session,
    SCRIPT_T *script)
{
    const UPDATE_SYSTEM_AUTHENTICATION_STORE_PARAMS_T params = {
        .on_update = on_system_authentication_store_updated,
        .on_error = on_error,
        .update_script = script
    };

    update_system_authentication_store(session, params);
    MUTEX_WAIT
}


void utils_apply_json_patch(
    SESSION_T *session,
    char *topic_path,
    char *patch)
{
    DIFFUSION_APPLY_JSON_PATCH_PARAMS_T patch_params = {
        .topic_path = topic_path,
        .patch = patch,
        .on_json_patch_result = on_json_patch_result
    };

    diffusion_apply_json_patch(session, patch_params, NULL);
    MUTEX_WAIT
}


double utils_random_double(void)
{
    SEED_RANDOM(RANDOM());
    long random_long = RANDOM() % 1000;
    double random_double = (double) (random_long / (double) 1000.0);
    return random_double;
}


const char *utils_print_global_permission(void *value)
{
    uint32_t *ptr = (uint32_t *) value;
    GLOBAL_PERMISSIONS_T permission = *ptr;
    return SECURITY_GLOBAL_PERMISSIONS_NAMES[permission];
}

const char *utils_print_path_permission(void *value)
{
    uint32_t *ptr = (uint32_t *) value;
    PATH_PERMISSIONS_T permission = *ptr;
    return SECURITY_PATH_PERMISSIONS_NAMES[permission];
}

const char *utils_print_string(void *value)
{
    char *ptr = (char *) value;
    return ptr;
}

const char *utils_print_path_permission_set(void *value)
{
    SET_T *set = (SET_T *) value;
    return utils_set_to_string(set, utils_print_path_permission);
}


LIST_T *utils_set_to_list(
    SET_T *set)
{
    LIST_T *result = list_create();
    for (void **entry = set_values(set); *entry != NULL; entry++ ) {
        list_append_last(result, *entry);
    }
    return result;
}


char *utils_set_to_string(
    SET_T *set,
    print_fn print_function)
{
    if (set == NULL) {
        return NULL;
    }
    size_t current_size = set->size * 10;
    size_t used = 0;
    char *result = calloc(current_size, sizeof(char));
    for (void **entry = set_values(set); *entry != NULL; entry++ ) {
        const char *entry_string = print_function(*entry);
        size_t remaining = current_size - used - 1;
        size_t required = strlen(entry_string) + ((used > 0) ? 1 : 0);
        if (remaining < required ) {
            // not enough space, realloc string
            current_size += required;
            result = realloc(result, current_size);
        }
        char *prefix = ((used == 0) ? "" : " ");
        sprintf(result + used, "%s%s", prefix, entry_string);
        used += required;
    }
    result[used] = 0;
    return result;
}

void utils_print_security_roles(
    HASH_T *hash,
    const char *selected_role)
{
    char **keys = hash_keys(hash);
    for(char **ptr = keys; *ptr != NULL; ptr++) {
        SECURITY_STORE_ROLE_T *role =
            (SECURITY_STORE_ROLE_T *)hash_get(hash, *ptr);

        if (selected_role != NULL && strcmp(role->name, selected_role) != 0) {
            continue;
        }

        printf("\t%s\n", role->name);

        char *included_roles = utils_set_to_string(
            role->included_roles,
            utils_print_string
        );
        printf("\t\tIncluded Roles: %s\n", included_roles);
        free(included_roles);

        char *global_permissions = utils_set_to_string(
            role->global_permissions,
            utils_print_global_permission
        );
        printf("\t\tGlobal Permissions: %s\n", global_permissions);
        free(global_permissions);

        char *default_path_permissions = utils_set_to_string(
            role->default_path_permissions,
            utils_print_path_permission
        );
        printf("\t\tDefault Path Permissions: %s\n", default_path_permissions);
        free(default_path_permissions);

        printf("\t\tPath Permissions:\n");
        utils_print_hash(
            role->path_permissions, NULL, utils_print_path_permission_set
        );

        printf("\t\tLocking Principal: %s\n", role->locking_principal);
    }
    free(keys);
}

void utils_print_hash(
    HASH_T *hash,
    const char *pattern,
    print_fn print_function)
{
    char **keys = hash_keys(hash);
    for(char **ptr = keys; *ptr != NULL; ptr++) {
        if (pattern == NULL || utils_string_matches_regex(pattern, *ptr)) {
            void *value = hash_get(hash, *ptr);
            const char *value_string = print_function(value);
            printf("\t%s: %s\n", *ptr, value_string);
        }
    }
    free(keys);
}

bool utils_string_starts_with(
    const char *prefix,
    const char *string)
{
    size_t prefix_length = strlen(prefix);
    size_t string_length = strlen(string);
    return
        string_length < prefix_length ? false : memcmp(prefix, string, prefix_length) == 0;
}


bool utils_string_matches_regex(
    const char *pattern,
    const char *string)
{
    Resub rs[1];
    Reprog *p = regcomp9((char *) pattern);
    memset(rs, 0, sizeof(Resub));
    return regexec9(p, (char *) string, rs, 1);
}

char *utils_list_to_string(LIST_T *list)
{
    if (list == NULL) {
        return NULL;
    }
    int list_size = list_get_size(list);
    size_t current_size = list_size * 10;
    size_t used = 0;
    char *result = calloc(current_size, sizeof(char));
    for (int i = 0; i < list_size; i++) {
        char *entry = list_get_data_indexed(list, i);
        size_t remaining = current_size - used - 1;
        size_t required = strlen(entry) + ((used > 0) ? 1 : 0);
        if (remaining < required ) {
            // not enough space, realloc string
            current_size += required;
            result = realloc(result, current_size);
        }
        char *prefix = ((used == 0) ? "" : " ");
        sprintf(result + used, "%s%s", prefix, entry);
        used += required;
    }
    result[used] = 0;
    return result;
}

void utils_print_script(SCRIPT_T *script)
{
    char *content = buf_as_string((BUF_T *)script);
    printf("Script: %s\n", content);
    free(content);
}

char *utils_path_to_folder(
    const char *path,
    const char *folder)
{
    LIST_T *components = list_create();
    char *token = strtok((char *) path, OS_PATH_SEPARATOR);
    size_t components_total_len = 0;

    while (token != NULL) {
        list_append_last(components, OS_STRDUP(token));
        components_total_len += strlen(token);
        if (strcmp(token, folder) == 0) {
            // we have the needed path, stop here
            break;
        }
        token = strtok(NULL, OS_PATH_SEPARATOR);
    }

    int size = list_get_size(components);
    size_t string_len = components_total_len + (size + 1) * strlen(OS_PATH_SEPARATOR) + 1;
    char *result = calloc(string_len + 1, sizeof(char));

    for (int i = 0; i < size; i++) {
        char *component = (char *) list_get_data_indexed(components, i);
        #ifdef WIN32
            if (i != 0) {
                OS_STRCAT(result, string_len, OS_PATH_SEPARATOR);
            }
        #else
            OS_STRCAT(result, string_len, OS_PATH_SEPARATOR);
        #endif
        OS_STRCAT(result, string_len, component);
    }
    return result;
}