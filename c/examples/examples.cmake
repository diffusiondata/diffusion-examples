function(add_example FOLDER_PATH FILE_NAME COPY_RESOURCES)
    string(REPLACE "/" "_" EXAMPLE_PREFIX ${FOLDER_PATH})
    set(EXAMPLE_NAME "${EXAMPLE_PREFIX}_${FILE_NAME}")
    add_executable(
        ${EXAMPLE_NAME}
        "${EXAMPLES_SOURCE_FOLDER}/main.c"
        "${EXAMPLES_SOURCE_FOLDER}/${FOLDER_PATH}/${FILE_NAME}.c"
        "${EXAMPLES_SOURCE_FOLDER}/utils/utils.c"
        "${EXAMPLES_SOURCE_FOLDER}/utils/regexp9.c"
    )
    target_link_libraries(${EXAMPLE_NAME} PRIVATE ${DEPENDENCIES} Threads::Threads ${ADDITIONAL_LD_FLAGS})
    target_include_directories(${EXAMPLE_NAME} PUBLIC ${INCLUDE_DIRECTORIES})

    if (${COPY_RESOURCES} STREQUAL "true")
        cmake_path(SET RESOURCES_PATH ${TARGET}/resources)

        add_custom_command(
            TARGET ${EXAMPLE_NAME} POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E make_directory "${RESOURCES_PATH}"
        )

        file(GLOB RESOURCES_FILES_LIST ${RESOURCES_SOURCE_FOLDER}/[^.]*)

        foreach(FILE IN ITEMS ${RESOURCES_FILES_LIST})
            add_custom_command(
                TARGET ${EXAMPLE_NAME} POST_BUILD
                COMMAND ${CMAKE_COMMAND} -E copy_if_different "${FILE}" "${RESOURCES_PATH}/"
            )
        endforeach()

        if (WIN32)
            cmake_path(SET LIB_PATH ${TARGET}/lib/${ARCHITECTURE})

            add_custom_command(
                TARGET ${EXAMPLE_NAME} POST_BUILD
                COMMAND ${CMAKE_COMMAND} -E make_directory "${LIB_PATH}"
            )

            foreach(FILE IN ITEMS ${WINDOWS_PATH})
                message("Copying ${FILE} to ${LIB_PATH}")
                add_custom_command(
                    TARGET ${EXAMPLE_NAME} POST_BUILD
                    COMMAND ${CMAKE_COMMAND} -E copy_if_different "${FILE}" "${LIB_PATH}/"
                )
            endforeach()
        endif()
    endif()
endfunction()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror -O1 -fsanitize=address -g -fno-omit-frame-pointer -Wdeprecated-declarations")

# Examples
file(GLOB_RECURSE SRCS ${EXAMPLES_SOURCE_FOLDER}/*.[hc])

add_example("connection/establishment" "connect_async" "false")
add_example("connection/establishment" "connect_sync_with_context" "false")
add_example("connection/establishment" "accept_all_certificates" "false")
add_example("connection/establishment" "accept_specific_certificate" "true")
add_example("connection/establishment" "connect_sync" "false")
add_example("connection/establishment" "connect_via_factory" "false")

add_example("connection" "session_state_listener" "false")

add_example("connection/resilience" "session_establishment_retry_mechanism" "false")
add_example("connection/resilience" "reconnection_strategy" "false")

add_example("ping" "ping_server" "false")

add_example("security" "change_principal" "false")
add_example("security" "get_global_permissions" "false")
add_example("security" "get_path_permissions" "false")

add_example("pub_sub/publish" "add_topic" "false")
add_example("pub_sub/publish" "add_topic_custom_properties" "false")
add_example("pub_sub/publish" "add_and_set_topic" "false")
add_example("pub_sub/publish" "set_topic" "false")
add_example("pub_sub/publish" "add_and_set_topic_via_update_stream" "false")
add_example("pub_sub/publish" "set_topic_via_update_stream" "false")
add_example("pub_sub/publish/with_constraint" "add_and_set_topic_no_topic" "false")
add_example("pub_sub/publish/with_constraint" "add_and_set_topic_value" "false")
add_example("pub_sub/publish/with_constraint" "add_and_set_topic_session_lock" "false")
add_example("pub_sub/publish/with_constraint" "add_and_set_topic_json_value_with" "false")
add_example("pub_sub/publish/with_constraint" "add_and_set_topic_json_value_without" "false")
add_example("pub_sub/publish/with_constraint" "add_and_set_topic_and" "false")
add_example("pub_sub/publish/with_constraint" "set_topic_no_value" "false")
add_example("pub_sub/publish/with_constraint" "set_topic_value" "false")
add_example("pub_sub/publish/with_constraint" "set_topic_session_lock" "false")
add_example("pub_sub/publish/with_constraint" "set_topic_json_value_with" "false")
add_example("pub_sub/publish/with_constraint" "set_topic_json_value_without" "false")

add_example("pub_sub/subscribe" "single_topic_via_path" "false")
add_example("pub_sub/subscribe" "multiple_topics_via_selector" "false")
add_example("pub_sub/subscribe" "fallback_stream" "false")
add_example("pub_sub/subscribe" "cross_compatible" "false")

add_example("pub_sub/remove" "multiple_topics_via_selector" "false")
add_example("pub_sub/remove" "single_topic_via_path" "false")
add_example("pub_sub/remove" "automatic_topic_removal" "false")

add_example("pub_sub/fetch" "topic_properties" "false")
add_example("pub_sub/fetch" "topic_via_paging" "false")

add_example("pub_sub/json_patch" "add" "false")
add_example("pub_sub/json_patch" "copy" "false")
add_example("pub_sub/json_patch" "move" "false")
add_example("pub_sub/json_patch" "remove" "false")
add_example("pub_sub/json_patch" "replace" "false")
add_example("pub_sub/json_patch" "test" "false")

add_example("time_series" "append_user_supplied_timestamp" "false")
add_example("time_series" "append_value_via_update_stream" "false")
add_example("time_series" "append_value" "false")
add_example("time_series" "create_topic" "false")
add_example("time_series" "edit_value" "false")
add_example("time_series" "range_query" "false")
add_example("time_series" "subscribe_cross_compatible" "false")
add_example("time_series" "subscribe" "false")

add_example("messaging" "send_to_path" "false")
add_example("messaging" "send_to_session_id" "false")
add_example("messaging" "send_to_session_filter" "false")

add_example("wrangling/topic_views/api" "add" "false")
add_example("wrangling/topic_views/api" "list" "false")
add_example("wrangling/topic_views/api" "remove" "false")

add_example("wrangling/topic_views/dsl" "source_path_directive" "false")
add_example("wrangling/topic_views/dsl" "remote_topic_view" "false")
add_example("wrangling/topic_views/dsl" "scalar_directive" "false")
add_example("wrangling/topic_views/dsl" "expand_value" "false")
add_example("wrangling/topic_views/dsl" "insert_transformations" "false")

add_example("wrangling/topic_views/dsl/process_transformations" "set" "false")
add_example("wrangling/topic_views/dsl/process_transformations" "remove" "false")
add_example("wrangling/topic_views/dsl/process_transformations" "continue" "false")

add_example("wrangling/topic_views/dsl/patch_transformations" "add" "false")
add_example("wrangling/topic_views/dsl/patch_transformations" "remove" "false")
add_example("wrangling/topic_views/dsl/patch_transformations" "replace" "false")
add_example("wrangling/topic_views/dsl/patch_transformations" "move" "false")
add_example("wrangling/topic_views/dsl/patch_transformations" "copy" "false")
add_example("wrangling/topic_views/dsl/patch_transformations" "test" "false")

add_example("wrangling/topic_views/dsl/options" "topic_property_mapping" "false")
add_example("wrangling/topic_views/dsl/options" "topic_value" "false")
add_example("wrangling/topic_views/dsl/options" "throttle" "false")
add_example("wrangling/topic_views/dsl/options" "delay" "false")
add_example("wrangling/topic_views/dsl/options" "separator" "false")
add_example("wrangling/topic_views/dsl/options" "preserve_topics" "false")
add_example("wrangling/topic_views/dsl/options" "topic_type" "false")

add_example("wrangling/session_trees" "put_branch_mapping_table" "false")
add_example("wrangling/session_trees" "list_session_tree_branches_with_mappings" "false")
add_example("wrangling/session_trees" "get_branch_mapping_table" "false")
add_example("wrangling/session_trees" "use_case" "false")
add_example("wrangling/session_trees" "put_and_remove_branch_mapping_table" "false")

add_example("monitoring" "missing_topic_notifications" "false")
add_example("monitoring" "topic_notifications" "false")
add_example("monitoring" "session_properties_listener" "false")

add_example("session_management" "subscription_control" "false")
add_example("session_management" "authentication_control" "false")

add_example("client_control" "get_session_properties_via_session_id" "false")
add_example("client_control" "set_session_properties_via_session_id" "false")
add_example("client_control" "set_session_properties_via_session_filter" "false")
add_example("client_control" "close_client_via_session_id" "false")
add_example("client_control" "close_client_via_session_filter" "false")
add_example("client_control" "change_roles_via_session_id" "false")
add_example("client_control" "change_roles_via_session_filter" "false")
add_example("client_control" "queue_conflation_via_session_filter" "false")

add_example("server_configuration/metrics" "session_metric_collector_put" "false")
add_example("server_configuration/metrics" "session_metric_collector_list" "false")
add_example("server_configuration/metrics" "session_metric_collector_remove" "false")
add_example("server_configuration/metrics" "topic_metric_collector_put" "false")
add_example("server_configuration/metrics" "topic_metric_collector_list" "false")
add_example("server_configuration/metrics" "topic_metric_collector_remove" "false")

add_example("server_configuration/security_control" "isolate_path" "false")
add_example("server_configuration/security_control" "deisolate_path" "false")
add_example("server_configuration/security_control" "set_path_permissions" "false")
add_example("server_configuration/security_control" "remove_path_permissions" "false")
add_example("server_configuration/security_control" "set_default_path_permissions" "false")
add_example("server_configuration/security_control" "set_global_permissions" "false")
add_example("server_configuration/security_control" "define_roles_hierarchy" "false")
add_example("server_configuration/security_control" "restrict_role_edit_permissions" "false")
add_example("server_configuration/security_control" "set_default_roles_for_anonymous_sessions" "false")
add_example("server_configuration/security_control" "set_default_roles_for_named_sessions" "false")

add_example("server_configuration/system_authentication_control" "deny_anonymous_connections" "false")
add_example("server_configuration/system_authentication_control" "abstain_anonymous_connections" "false")
add_example("server_configuration/system_authentication_control" "allow_anonymous_connections" "false")
add_example("server_configuration/system_authentication_control" "add_principal" "false")
add_example("server_configuration/system_authentication_control" "add_locked_principal" "false")
add_example("server_configuration/system_authentication_control" "remove_principal" "false")
add_example("server_configuration/system_authentication_control" "assign_roles" "false")
add_example("server_configuration/system_authentication_control" "change_password" "false")
add_example("server_configuration/system_authentication_control" "verify_password" "false")
add_example("server_configuration/system_authentication_control" "trust_client_proposed_property_in" "false")
add_example("server_configuration/system_authentication_control" "trust_client_proposed_property_matches" "false")
add_example("server_configuration/system_authentication_control" "ignore_client_proposed_property" "false")

add_example("remote_servers" "create_primary_initiator" "false")
add_example("remote_servers" "create_secondary_initiator" "false")
add_example("remote_servers" "create_secondary_acceptor" "false")
add_example("remote_servers" "list" "false")
add_example("remote_servers" "check" "false")
add_example("remote_servers" "remove" "false")