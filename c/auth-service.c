/*
 * Copyright © 2014, 2017 Push Technology Ltd.
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
 * Diffusion can be configured to delegate authentication requests to
 * an external handler. This program provides an authentication
 * handler to demonstrate this feature. A detailed description of
 * security and authentication handlers can be found in the Diffusion
 * user manual.
 *
 * Authentication handlers are registered with a name, which is typically specified in
 * Server.xml
 *
 * Two handler names are provided by Diffusion and Diffusion Cloud by default;
 * before-system-handler and after-system-handler, and additional
 * handlers may be specified for Diffusion through the Server.xml file
 * and an accompanying Java class that implements the
 * AuthenticationHandler interface.
 *
 * This control authentication handler connects to Diffusion and attempts
 * to register itself with a user-supplied name, which should match the name
 * configured in Server.xml.
 *
 * The default behavior is to install as the "before-system-handler",
 * which means that it will intercept authentication requests before
 * Diffusion has a chance to act on them.
 *
 * It will:
 * <ul>
 * <li>Deny all anonymous connections</li>
 * <li>Allow connections where the principal and credentials (i.e., username and password) match some hardcoded values</li>
 * <li>Abstain from all other decisions, thereby letting Diffusion and other authentication handlers decide what to do.</li>
 * </ul>
 */

#include <stdio.h>
#include <stdlib.h>
#ifndef WIN32
#include <unistd.h>
#else
#define sleep(x) Sleep(1000 * x)
#endif

#include "diffusion.h"
#include "args.h"
#include "conversation.h"

struct user_credentials_s {
        const char *username;
        const char *password;
};

/*
 * Username/password pairs that this handler accepts.
 */
static const struct user_credentials_s USERS[] = {
        { "fish", "chips" },
        { "ham", "eggs" },
        { NULL, NULL }
};

ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'n', "name", "Name under which to register the authorisation handler", ARG_OPTIONAL, ARG_HAS_VALUE, "before-system-handler"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, NULL},
        END_OF_ARG_OPTS
};

/*
 * When the authentication service has been registered, this function will be
 * called.
 */
static int
on_registration(SESSION_T *session, void *context)
{
        printf("Registered authentication handler\n");
        return HANDLER_SUCCESS;
}

/*
 * When the authentication service has be deregistered, this function will be
 * called.
 */
static int
on_deregistration(SESSION_T *session, void *context)
{
        printf("Deregistered authentication handler\n");
        return HANDLER_SUCCESS;
}

/*
 * This is the function that is called when authentication has been delegated
 * from Diffusion.
 *
 * The response may return one of three values via the response parameter:
 * ALLOW:   The user is authenticated.
 * ALLOW_WITH_RESULT: The user is authenticated, and additional roles are
 *                    to be applied to the user.
 * DENY:    The user is NOT authenticated.
 * ABSTAIN: Allow another handler to make the decision.
 *
 * The handler should return HANDLER_SUCCESS in all cases, unless an actual
 * error occurs during the authentication process (in which case,
 * HANDLER_FAILURE is appropriate).
 */
static int
on_authentication(SESSION_T *session,
                  const SVC_AUTHENTICATION_REQUEST_T *request,
                  SVC_AUTHENTICATION_RESPONSE_T *response,
                  void *context)
{
        // No credentials, or not password type. We're not an authority for
        // this type of authentication so abstain in case some other registered
        // authentication handler can deal with the request.
        if(request->credentials == NULL) {
                printf("No credentials specified, abstaining\n");
                response->value = AUTHENTICATION_ABSTAIN;
                return HANDLER_SUCCESS;
        }
        if(request->credentials->type != PLAIN_PASSWORD) {
                printf("Credentials are not PLAIN_PASSWORD, abstaining\n");
                response->value = AUTHENTICATION_ABSTAIN;
                return HANDLER_SUCCESS;
        }

        printf("principal = %s\n", request->principal);
        printf("credentials = %*s\n",
               (int)request->credentials->data->len,
               request->credentials->data->data);

        if(request->principal == NULL || strlen(request->principal) == 0) {
                printf("Denying anonymous connection (no principal)\n");
                response->value = AUTHENTICATION_DENY; // Deny anon connections
                return HANDLER_SUCCESS;
        }

        char *password = malloc(request->credentials->data->len + 1);
        memmove(password, request->credentials->data->data, request->credentials->data->len);
        password[request->credentials->data->len] = '\0';

        int auth_decided = 0;
        int i = 0;
        while(USERS[i].username != NULL) {

                printf("Checking username %s vs %s\n", request->principal, USERS[i].username);
                printf("     and password %s vs %s\n", password, USERS[i].password);

                if(strcmp(USERS[i].username, request->principal) == 0 &&
                   strcmp(USERS[i].password, password) == 0) {

                        puts("Allowed");
                        response->value = AUTHENTICATION_ALLOW;
                        auth_decided = 1;
                        break;
                }
                i++;

        }

        free(password);

        if(auth_decided == 0) {
                puts("Abstained");
                response->value = AUTHENTICATION_ABSTAIN;
        }

        return HANDLER_SUCCESS;
}

int
main(int argc, char** argv)
{
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if (options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return EXIT_FAILURE;
        }

        char *url = hash_get(options, "url");
        char *name = hash_get(options, "name");
        char *principal = hash_get(options, "principal");
        char *credentials = hash_get(options, "credentials");

        /*
         * Create a session with Diffusion.
         */
        puts("Creating session");
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url,
                                            principal,
                                            credentials != NULL ? credentials_create_password(credentials) : NULL,
                                            NULL, NULL,
                                            &error);
        if (session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        /*
         * Provide a set (via a hash map containing keys and NULL
         * values) to indicate what information about the connecting
         * client that we'd like Diffusion to send us.
         */
        HASH_T *detail_set = hash_new(5);
        char buf[2];
        sprintf(buf, "%d", SESSION_DETAIL_SUMMARY);
        hash_add(detail_set, strdup(buf), NULL);
        sprintf(buf, "%d", SESSION_DETAIL_LOCATION);
        hash_add(detail_set, strdup(buf), NULL);
        sprintf(buf, "%d", SESSION_DETAIL_CONNECTOR_NAME);
        hash_add(detail_set, strdup(buf), NULL);

        /*
         * Register the authentication handler.
         */
        AUTHENTICATION_REGISTRATION_PARAMS_T auth_registration_params = {
                .name = name,
                .detail_set = detail_set,
                .on_registration = on_registration,
                .authentication_handlers.on_authentication = on_authentication
        };

        puts("Sending registration request");
        SVC_AUTHENTICATION_REGISTER_REQUEST_T *reg_request =
                authentication_register(session, auth_registration_params);

        /*
         *  Wait a while before moving on to deregistration.
         */
        sleep(30);

        AUTHENTICATION_DEREGISTRATION_PARAMS_T auth_deregistration_params = {
                .on_deregistration = on_deregistration,
                .original_request = reg_request
        };

        /*
         * Deregister the authentication handler.
         */
        printf("Deregistering authentication handler\n");
        authentication_deregister(session, auth_deregistration_params);

        session_close(session, NULL);
        session_free(session);
        hash_free(options, NULL, free);

        return EXIT_SUCCESS;
}
