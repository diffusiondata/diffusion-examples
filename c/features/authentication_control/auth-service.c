/*
 * Copyright © 2014 - 2023 DiffusionData Ltd.
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
 * @author DiffusionData Limited
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

// Username/password pairs that this handler accepts.
static const struct user_credentials_s USERS[] = {
        { "fish", "chips" },
        { "ham", "eggs" },
        { NULL, NULL }
};


DIFFUSION_REGISTRATION_T *g_registration = NULL;


ARG_OPTS_T arg_opts[] = {
        ARG_OPTS_HELP,
        {'u', "url", "Diffusion server URL", ARG_OPTIONAL, ARG_HAS_VALUE, "ws://localhost:8080"},
        {'n', "name", "Name under which to register the authorisation handler", ARG_OPTIONAL, ARG_HAS_VALUE, "before-system-handler"},
        {'p', "principal", "Principal (username) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "control"},
        {'c', "credentials", "Credentials (password) for the connection", ARG_OPTIONAL, ARG_HAS_VALUE, "password"},
        END_OF_ARG_OPTS
};

// When the authenticator handler is active, this function will be called.
static int on_authenticator_active(
        SESSION_T *session,
        const DIFFUSION_REGISTRATION_T *registration)
{
        g_registration = diffusion_registration_dup(registration);

        printf("Registered authentication handler\n");
        return HANDLER_SUCCESS;
}


// When the authenticator handler is closed, this function will be called.
static void on_authenticator_close()
{
        printf("Closed authentication handler\n");
}

/*
 * This is the function that is called when authentication has been delegated
 * from Diffusion.
 *
 * The available methods for an authentication response are:
 * diffusion_authenticator_allow: The user is authenticated without user-defined
 * properties
 * diffusion_authenticator_allow_with_properties: The user is authenticated with
 * modifications to the session properties.
 * diffusion_authenticator_abstain: Allow another handler to make the decision
 * diffusion_authenticator_deny: The user is NOT authenticated
 * The response may return one of three values via the response parameter:
 *
 * The handler should return HANDLER_SUCCESS in all cases, unless an actual
 * error occurs during the authentication process (in which case,
 * HANDLER_FAILURE is appropriate).
 */
static int on_authenticator_authenticate(
        SESSION_T *session,
        const char *principal,
        const CREDENTIALS_T *credentials,
        const HASH_T *session_properties,
        const HASH_T *proposed_session_properties,
        const DIFFUSION_AUTHENTICATOR_T *authenticator)
{
        // No credentials, or not password type. We're not an authority for
        // this type of authentication so abstain in case some other registered
        // authentication handler can deal with the request.
        if(credentials == NULL) {
                printf("No credentials specified, abstaining\n");
                diffusion_authenticator_abstain(session, authenticator, NULL);
                return HANDLER_SUCCESS;
        }
        if(credentials->type != PLAIN_PASSWORD) {
                printf("Credentials are not PLAIN_PASSWORD, abstaining\n");
                diffusion_authenticator_abstain(session, authenticator, NULL);
                return HANDLER_SUCCESS;
        }

        printf("principal = %s\n", principal);
        printf("credentials = %*s\n",
               (int)credentials->data->len,
               credentials->data->data);

        if(principal == NULL || strlen(principal) == 0) {
                printf("Denying anonymous connection (no principal)\n");
                // Deny anonymous connections
                diffusion_authenticator_deny(session, authenticator, NULL);
                return HANDLER_SUCCESS;
        }

        char *password = malloc(credentials->data->len + 1);
        memmove(password, credentials->data->data, credentials->data->len);
        password[credentials->data->len] = '\0';

        int auth_decided = 0;
        int i = 0;
        while(USERS[i].username != NULL) {

                printf("Checking username %s vs %s\n", principal, USERS[i].username);
                printf("     and password %s vs %s\n", password, USERS[i].password);

                if(strcmp(USERS[i].username, principal) == 0 &&
                   strcmp(USERS[i].password, password) == 0) {

                        puts("Allowed");
                        diffusion_authenticator_allow(session, authenticator, NULL);
                        auth_decided = 1;
                        break;
                }
                i++;
        }

        free(password);

        if(auth_decided == 0) {
                puts("Abstained");
                diffusion_authenticator_abstain(session, authenticator, NULL);
        }
        return HANDLER_SUCCESS;
}


int main(int argc, char** argv)
{
        HASH_T *options = parse_cmdline(argc, argv, arg_opts);
        if (options == NULL || hash_get(options, "help") != NULL) {
                show_usage(argc, argv, arg_opts);
                return EXIT_FAILURE;
        }

        const char *url = hash_get(options, "url");
        const char *name = hash_get(options, "name");
        const char *principal = hash_get(options, "principal");
        const char *password = hash_get(options, "credentials");

        CREDENTIALS_T *credentials = NULL;
        if (password != NULL) {
            credentials = credentials_create_password(password);
        }

        /*
         * Create a session with Diffusion.
         */
        puts("Creating session");
        DIFFUSION_ERROR_T error = { 0 };
        SESSION_T *session = session_create(url, principal, credentials, NULL, NULL, &error);
        if (session == NULL) {
                fprintf(stderr, "TEST: Failed to create session\n");
                fprintf(stderr, "ERR : %s\n", error.message);
                return EXIT_FAILURE;
        }

        // Register the authentication handler.
        DIFFUSION_AUTHENTICATION_HANDLER_T handler = {
                .handler_name = (char *) name,
                .on_active = on_authenticator_active,
                .on_authenticate = on_authenticator_authenticate,
                .on_close = on_authenticator_close
         };

        DIFFUSION_AUTHENTICATION_HANDLER_PARAMS_T params = {
                .handler = &handler
         };

        puts("Setting authentication handler");
        diffusion_set_authentication_handler(session, params);

        // Wait a while before moving on to deregistration.
        sleep(30);

        // Deregister the authentication handler.
        printf("Closing authentication handler\n");
        diffusion_registration_close(session, g_registration);

        session_close(session, NULL);
        session_free(session);
        hash_free(options, NULL, free);
        credentials_free(credentials);
        diffusion_registration_free(g_registration);
        g_registration = NULL;

        return EXIT_SUCCESS;
}
