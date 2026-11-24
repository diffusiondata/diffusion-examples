#include "coordinator.h"
#include "utils.h"


COORDINATOR_T *coordinator_init(void)
{
    COORDINATOR_T *result = calloc(1, sizeof(COORDINATOR_T));
    apr_initialize();
    apr_pool_create(&result->pool, NULL);
    apr_thread_mutex_create(&result->mutex, APR_THREAD_MUTEX_NESTED, result->pool);
    apr_thread_cond_create(&result->cond, result->pool);
    apr_atomic_set32(&result->flag, 0);
    result->timeout = 5000;
    result->value = NULL;
    result->response = NULL;
    return result;
}


void coordinator_free(COORDINATOR_T *coordinator)
{
    if (coordinator == NULL) {
        return;
    }
    apr_thread_mutex_destroy(coordinator->mutex);
    apr_thread_cond_destroy(coordinator->cond);
    apr_pool_destroy(coordinator->pool);
    apr_terminate();

    // value and response allocation resposibility is of who made them
    free(coordinator);
}


void coordinator_wait(COORDINATOR_T *coordinator)
{
    if (coordinator == NULL) {
        return;
    }
    if (apr_atomic_read32(&coordinator->flag) == 0) {
        // needs to wait
        apr_thread_mutex_lock(coordinator->mutex);
        if(apr_thread_cond_timedwait(coordinator->cond, coordinator->mutex, coordinator->timeout * 1000) != APR_SUCCESS) {
            LOG("Waiting expired.");
            exit(1);
        }
        apr_thread_mutex_unlock(coordinator->mutex);
    }
    // reset flag
    apr_atomic_set32(&coordinator->flag, 0);
}


void coordinator_broadcast(COORDINATOR_T *coordinator)
{
    if (coordinator == NULL) {
        return;
    }
    apr_thread_mutex_lock(coordinator->mutex);
    apr_atomic_set32(&coordinator->flag, 1);
    apr_thread_cond_broadcast(coordinator->cond);
    apr_thread_mutex_unlock(coordinator->mutex);
}

