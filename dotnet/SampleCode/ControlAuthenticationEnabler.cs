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
 */

using PushTechnology.ClientInterface.Client.Details;
using PushTechnology.ClientInterface.Client.Security.Authentication;
using PushTechnology.DiffusionCore.Client.Types;

namespace Examples {
    /// <summary>
    /// This is a local authentication handler that allows control clients to install their own authentication handlers.
    /// </summary>
    public class ControlAuthenticationEnabler : IAuthenticationHandler {
        private const string AuthUser = "auth";
        private const string AuthPassword = "auth_secret";

        /// <summary>
        /// Request authentication.
        ///
        /// The server calls this to authenticate new sessions, and when a client requests the session principal is
        /// changed.
        ///
        /// For each call to Authenticate, the authentication handler should respond by calling one of the methods of
        /// the provided callback. The handler may return immediately and process the authentication request
        /// asynchronously. The client session will be blocked until a callback method is called.
        /// </summary>
        /// <param name="principal"></param>
        /// <param name="credentials"></param>
        /// <param name="sessionDetails"></param>
        /// <param name="callback"></param>
        public void Authenticate( string principal, ICredentials credentials, ISessionDetails sessionDetails,
            IAuthenticationHandlerCallback callback ) {
            if ( credentials.Type == CredentialsType.PLAIN_PASSWORD ) {
                if ( AuthUser.Equals( principal ) ) {
                    if ( AuthPassword.Equals( System.Text.Encoding.UTF8.GetString( credentials.ToBytes() ) ) ) {
                        callback.Allow();

                        return;
                    }

                    callback.Deny();

                    return;
                }
            }

            callback.Abstain();
        }
    }
}