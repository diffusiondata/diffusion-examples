#ifndef _diffusion_utils_functions_
#define _diffusion_utils_functions_ 1

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

#include "diffusion.h"

#include "apr.h"
#include "apr_getopt.h"
#include "apr_thread_mutex.h"
#include "apr_thread_cond.h"
#include <time.h>

extern apr_pool_t *g_pool;
extern apr_thread_cond_t *g_cond;
extern apr_thread_mutex_t *g_mutex;
extern volatile apr_uint32_t g_flag;
extern unsigned long g_timeout;

// Required global variables for synchronisation of threads

// Macro to define
#define GET_TID ((unsigned int)(((unsigned long long)apr_os_thread_current()) % 1000))

#define LOG(format, ...) fprintf(stdout, "[%03d] %30s():%s:%d " format "\n", GET_TID, __FUNCTION__, __FILE__, __LINE__, ##__VA_ARGS__); fflush(stdout)

#define MUTEX_DEF                           \
    apr_pool_t *g_pool;                     \
    apr_thread_cond_t *g_cond;              \
    apr_thread_mutex_t *g_mutex;            \
    volatile apr_uint32_t g_flag;           \
    unsigned long g_timeout = 5000;

// Macro to initialise
#define MUTEX_INIT                                                          \
    apr_initialize();                                                       \
    apr_pool_create(&g_pool, NULL);                                         \
    apr_thread_mutex_create(&g_mutex, APR_THREAD_MUTEX_NESTED, g_pool);     \
    apr_thread_cond_create(&g_cond, g_pool);                                \
    apr_atomic_set32(&g_flag, 0);

// Macro to terminate
#define MUTEX_TERMINATE                     \
    apr_thread_mutex_destroy(g_mutex);      \
    apr_thread_cond_destroy(g_cond);        \
    apr_pool_destroy(g_pool);               \
    apr_terminate();

// Macro to wait
#define MUTEX_WAIT                                                                          \
    if (apr_atomic_read32(&g_flag) == 0) {                                                  \
        apr_thread_mutex_lock(g_mutex);                                                     \
        if(apr_thread_cond_timedwait(g_cond, g_mutex, g_timeout * 1000) != APR_SUCCESS) {   \
            LOG("Waiting expired.");                                                        \
            exit(1);                                                                        \
        }                                                                                   \
        apr_thread_mutex_unlock(g_mutex);                                                   \
        apr_atomic_set32(&g_flag, 0);                                                       \
    }

// Macro to broadcast
#define MUTEX_BROADCAST                     \
    apr_thread_mutex_lock(g_mutex);         \
    apr_thread_cond_broadcast(g_cond);      \
    apr_atomic_set32(&g_flag, 1);           \
    apr_thread_mutex_unlock(g_mutex);


#ifndef WIN32
        #include <unistd.h>
        #include <sys/socket.h>
        #include <string.h>

        #define OS_STRTOK(STRING, SEPARATOR, REMAINDER_PTR) strtok_r(STRING, SEPARATOR, REMAINDER_PTR)
        #define OS_STRDUP(x) strdup(x)
        #define OS_STRDUP_FN (void *(*) (void *)) strdup
        #define OS_STRCAT(DEST, DEST_LEN, SOURCE) strcat(DEST, SOURCE)
        #define OS_STRNCAT(DEST, DEST_LEN, SOURCE, COUNT) strncat(DEST, SOURCE, COUNT)
        #define OS_PATH_SEPARATOR "/"

        #define SEED_RANDOM(SEED) srandom(SEED)
        #define RANDOM() random()
        #define OS_SETENV(X, Y) setenv(X, Y, 1)
#else
        #define OS_STRTOK(STRING, SEPARATOR, REMAINDER_PTR) strtok_s(STRING, SEPARATOR, REMAINDER_PTR)
        #define OS_STRDUP(x) _strdup(x)
        #define OS_STRDUP_FN (void *(*) (void *)) _strdup
        #define OS_STRCAT(DEST, DEST_LEN, SOURCE) strcat_s(DEST, DEST_LEN, SOURCE)
        #define OS_STRNCAT(DEST, DEST_LEN, SOURCE, COUNT) strncat_s(DEST, DEST_LEN, SOURCE, COUNT)
        #define OS_PATH_SEPARATOR "\\"

        #define WIN32_LEAN_AND_MEAN
        #define getpid _getpid

        #define SEED_RANDOM(SEED) srand(SEED)
        #define RANDOM() rand()
        #define OS_SETENV(X, Y) _putenv_s(X, Y)

        #define sleep(x) Sleep(1000 * x)
#endif


typedef struct {
    char *rule;
    char *topic_path;
} UTILS_SESSION_TREES_MAPPING_T;

typedef const char *(*print_fn)(void *value);

SESSION_T *utils_open_session(
    const char *url,
    const char *principal,
    const char *password
);

void utils_remove_topic(
    SESSION_T *session,
    const char *topic_selector
);

void utils_create_topic_with_properties(
    SESSION_T *session,
    const char *topic_path,
    TOPIC_TYPE_T topic_type,
    DIFFUSION_DATATYPE datatype,
    BUF_T *value,
    HASH_T *properties
);

void utils_set_topic_value(
    SESSION_T *session,
    const char *topic_path,
    DIFFUSION_DATATYPE datatype,
    BUF_T *value
);

void utils_create_int64_topic(
    SESSION_T *session,
    const char *topic_path,
    int64_t int64_value
);

void utils_set_int64_topic_value(
SESSION_T *session,
    const char *topic_path,
    int64_t int64_value
);

void utils_create_json_topic(
    SESSION_T *session,
    const char *topic_path,
    const char *json_string
);

void utils_create_json_topic_with_topic_removal_policy(
    SESSION_T *session,
    const char *topic_path,
    const char *topic_removal_policy
);

void utils_create_json_topic_with_properties(
    SESSION_T *session,
    const char *topic_path,
    const char *json_string,
    HASH_T *properties
);

void utils_set_json_topic_value(
    SESSION_T *session,
    const char *topic_path,
    const char *json_string
);

void utils_create_string_topic_with_properties(
    SESSION_T *session,
    const char *topic_path,
    const char *value_string,
    HASH_T *properties
);

void utils_create_string_topic(
    SESSION_T *session,
    const char *topic_path,
    const char *value_string
);

void utils_create_topic_view(
    SESSION_T *session,
    const char *name,
    const char *specification
);

void utils_create_remote_server(
    SESSION_T *session,
    char *name,
    char *url,
    char *principal,
    char *password,
    HASH_NUM_T *connection_options
);

VALUE_STREAM_T *utils_create_value_stream(
    SESSION_T *session,
    const char *topic_selector,
    DIFFUSION_DATATYPE datatype
);

VALUE_STREAM_T *utils_subscribe(
    SESSION_T *session,
    const char *topic_selector,
    DIFFUSION_DATATYPE datatype
);

void utils_get_topic_properties(
    SESSION_T *session,
    const char *topic_selector
);

void utils_time_series_range_query_int64(
    SESSION_T *session,
    const char *topic_path
);

void utils_build_branch_mapping_table(
    SESSION_T * session,
    const char *topic_path,
    size_t mappings_length,
    const UTILS_SESSION_TREES_MAPPING_T mappings[]
);

void utils_remove_branch_mapping_table(
    SESSION_T *session,
    const char *topic_path
);

void utils_print_session_properties(
    SESSION_T *session,
    SESSION_ID_T *session_id,
    const char *properties_regex
);

void utils_remove_session_metric_collector(
    SESSION_T *session,
    const char *name
);

void utils_remove_topic_metric_collector(
    SESSION_T *session,
    const char *name
);

SECURITY_STORE_T *utils_get_security_store(
    SESSION_T *session
);

void utils_remove_role(
    SESSION_T *session,
    const char *role_name
);

void utils_add_role_from_security_store(
    SESSION_T *session,
    SECURITY_STORE_T *store,
    const char *role_name
);

void utils_print_security_store(
    SESSION_T *session,
    const char *selected_role
);

void utils_remove_topic_view(
    SESSION_T *session,
    const char *topic_view_name
);

void utils_remove_remote_server(
    SESSION_T *session,
    const char *remote_server_name
);

void utils_release_lock(
    SESSION_T *session,
    DIFFUSION_SESSION_LOCK_T *lock
);

void utils_update_security_store(
    SESSION_T *session,
    SCRIPT_T *script
);

void utils_update_system_authentication_store(
    SESSION_T *session,
    SCRIPT_T *script
);

void utils_apply_json_patch(
    SESSION_T *session,
    char *topic_path,
    char *patch
);

double utils_random_double(void);

LIST_T *utils_set_to_list(
    SET_T *set
);

char *utils_set_to_string(
    SET_T *set,
    print_fn print_function
);

void utils_print_security_roles(
    HASH_T *hash,
    const char *selected_role
);

void utils_print_hash(
    HASH_T *hash,
    const char *pattern,
    print_fn print_function
);

bool utils_string_starts_with(
    const char *prefix,
    const char *string
);

bool utils_string_matches_regex(
    const char *pattern,
    const char *string
);

char *utils_list_to_string(LIST_T *list);

const char *utils_print_global_permission(void *value);

const char *utils_print_path_permission(void *value);

const char *utils_print_string(void *value);

void utils_print_script(SCRIPT_T *script);

char *utils_path_to_folder(
    const char *path,
    const char *folder
);

#endif