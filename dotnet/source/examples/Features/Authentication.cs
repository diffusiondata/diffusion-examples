/**
 * Copyright © 2019 Push Technology Ltd.
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

using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Security.Authentication;
using PushTechnology.DiffusionCore.Client.Types;
using static PushTechnology.ClientInterface.Examples.Runner.Program;
using static System.Console;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Implementation of a client which authenticates other sessions.
    /// </summary>
    public sealed class Authentication : IExample {
        /// <summary>
        /// Runs the authenticator client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];

            // Connect as a control session
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" ).Open( serverUrl );

            try {
                await session.AuthenticationControl.SetAuthenticationHandlerAsync(
                    "before-system-handler", new Authenticator(), cancellationToken );

                // Run until user requests ending of example
                await Task.Delay( Timeout.Infinite, cancellationToken );
            } catch ( TaskCanceledException ) {
                //Task was canceled; 
            } finally {
                session.Close();
            }
        }

        /// <summary>
        /// Basic implementation of <see cref="IControlAuthenticator"/>.
        /// </summary>
        private sealed class Authenticator : IControlAuthenticator {
            /// <summary>
            /// Method which decides whether a connection attempt should be allowed, denied or
            /// if another authenticator should evaluate this request.
            /// </summary>
            /// <param name="principal">The session principal.</param>
            /// <param name="credentials">The credentials.</param>
            /// <param name="sessionProperties">The session properties.</param>
            /// <param name="proposedProperties">The client proposed properties.</param>
            /// <param name="callback">The callback.</param>
            public void Authenticate(
                string principal,
                ICredentials credentials,
                IReadOnlyDictionary<string, string> sessionProperties,
                IReadOnlyDictionary<string, string> proposedProperties,
                IAuthenticatorCallback callback ) {

                switch ( principal ) {
                case "admin": {
                        WriteLine( "Authenticator allowing connection with proposed properties properties." );
                        callback.Allow( proposedProperties );
                        break;
                    }
                case "client": {
                        WriteLine( "Authenticator allowing connection with no properties." );
                        callback.Allow();
                        break;
                    }
                case "block": {
                        WriteLine( "Authenticator denying connection." );
                        callback.Deny();
                        break;
                    }
                default: {
                        WriteLine( "Authenticator abstaining." );
                        callback.Abstain();
                        break;
                    }
                }
            }
            /// <summary>
            /// Notification of authenticator closure.
            /// </summary>
            public void OnClose() => WriteLine( "Authenticator closed." );

            /// <summary>
            /// Notification of error.
            /// </summary>
            /// <param name="errorReason">Error reason.</param>
            public void OnError( ErrorReason errorReason ) => WriteLine( $"Received and error: {errorReason}" );
        }
    }
}
