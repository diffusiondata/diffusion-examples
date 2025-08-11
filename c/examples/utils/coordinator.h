#ifndef _diffusion_coordinator_
#define _diffusion_coordinator_ 1


#include "apr.h"
#include "apr_getopt.h"
#include "apr_thread_mutex.h"
#include "apr_thread_cond.h"
#include <time.h>

typedef struct coordinator_s {
    apr_pool_t *pool;
    apr_thread_cond_t *cond;
    apr_thread_mutex_t *mutex;
    unsigned long timeout;
    volatile apr_uint32_t flag;

    void *response;
    void *value;
} COORDINATOR_T;


COORDINATOR_T *coordinator_init(void);

void coordinator_free(COORDINATOR_T *coordinator);

void coordinator_wait(COORDINATOR_T *coordinator);

void coordinator_broadcast(COORDINATOR_T *coordinator);


#endif