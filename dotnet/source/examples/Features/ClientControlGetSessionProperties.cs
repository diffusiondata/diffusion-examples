/**
 * Copyright © 2021 Push Technology Ltd.
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

using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that gets the property values of a session.
    /// </summary>
    public sealed class ClientControlGetSessionProperties : IExample
    {
        /// <summary>
        /// Runs the client control example that gets the property values of a session.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var serverUrl = args[0];
            var controlSession = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var clientControl = controlSession.ClientControl;

            ISession session = Diffusion.Sessions.Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .NoReconnection()
                .Open(serverUrl);

            WriteLine($"Session with id '{session.SessionId}' created.");

            List<string> requiredProperties = new List<string> { SessionProperty.ALL_FIXED_PROPERTIES, SessionProperty.ALL_USER_PROPERTIES };

            try
            {
                clientControl.GetSessionProperties(session.SessionId, requiredProperties, new SessionPropertiesCallback());
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to get properties of session with id '{session.SessionId}' : {ex}.");
            }

            // Close the sessions
            session.Close();
            controlSession.Close();
        }

        /// <summary>
        /// The callback for retrieving session properties.
        /// </summary>
        private class SessionPropertiesCallback : ISessionPropertiesCallback
        {
            /// <summary>
            /// Notification of a contextual error related to this callback.
            /// </summary>
            /// <param name="errorReason"></param>
            public void OnError(ErrorReason errorReason)
            {
                WriteLine($"An error has occured : {errorReason}.");
            }

            /// <summary>
            /// Indicates that the session is not known by the server.
            /// </summary>
            public void OnUnknownSession(ISessionId sessionId)
            {
                WriteLine($"Session with id {sessionId} is not known.");
            }

            /// <summary>
            /// Called to return requested session properties.
            /// </summary>
            public void OnReply(ISessionId sessionId, Dictionary<string, string> properties)
            {
                WriteLine($"Session properties are:");

                foreach (var property in properties)
                {
                    WriteLine($"{property.Key}: {property.Value}");
                }
            }
        }
    }
}
