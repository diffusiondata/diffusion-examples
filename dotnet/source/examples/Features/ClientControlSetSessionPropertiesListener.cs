/**
 * Copyright © 2021 - 2023 DiffusionData Ltd.
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
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Features.Impl;
using PushTechnology.ClientInterface.Client.Security.Authentication;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.DiffusionCore.Client.Types;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that sets the property values of a session using a listener for notifications.
    /// </summary>
    public sealed class ClientControlSetSessionPropertiesListener : IExample
    {
        /// <summary>
        /// Runs the client control example that sets the property values of a session using a listener for notifications.
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

            try
            {
                List<string> requiredProperties = new List<string> { SessionProperty.ALL_FIXED_PROPERTIES, SessionProperty.ALL_USER_PROPERTIES };

                clientControl.SetSessionPropertiesListener(new SessionPropertiesListener(), requiredProperties.ToArray());
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to register listener : {ex}.");
                controlSession.Close();
                return;
            }

            ISession session = Diffusion.Sessions.Principal("client")
                                    .Credentials(Diffusion.Credentials.Password("password"))
                                    .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                                    .NoReconnection()
                                    .Open(serverUrl);

            try
            {
                Dictionary<string, string> properties = new Dictionary<string, string> { { "$Latitude", "51.509865" } };

                var changedProperties = await clientControl.SetSessionPropertiesAsync(session.SessionId, properties);

                foreach (var changedProperty in changedProperties)
                {
                    var value = string.IsNullOrEmpty(changedProperty.Value) ? "[not set]" : $"'{changedProperty.Value}'";
                    WriteLine($"Session property {changedProperty.Key} changed from {value} to '{properties[changedProperty.Key]}'");
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to set properties of session with id '{session.SessionId}' : {ex}.");
            }

            // Close the sessions
            session.Close();
            controlSession.Close();
        }

        /// <summary>
        /// The handler for session properties listener notifications.
        /// </summary>
        private class SessionPropertiesListener : ISessionPropertiesListener
        {
            /// <summary>
            /// Called if the handler is closed.
            /// </summary>
            public void OnClose()
            {
                WriteLine($"The listener was closed.");
            }

            /// <summary>
            /// Notification of a contextual error related to this handler.
            /// </summary>
            /// <param name="errorReason"></param>
            public void OnError(ErrorReason errorReason)
            {
                WriteLine($"An error has occured : {errorReason}.");
            }

            /// <summary>
            /// Called when the handler has been successfully registered with the server.
            /// </summary>
            /// <param name="registration"></param>
            public void OnRegistered(IRegistration registration)
            {
                WriteLine($"The listener has been registered.");
            }

            /// <summary>
            /// Notification that a client session has closed.
            /// </summary>
            /// <param name="sessionId"></param>
            /// <param name="properties"></param>
            /// <param name="closeReason"></param>
            public void OnSessionClose(ISessionId sessionId, IDictionary<string, string> properties, CloseReason closeReason)
            {
                WriteLine($"Session with id '{sessionId}' has been closed.");
            }

            /// <summary>
            /// Notification of a session event that can result in a change of properties.
            /// </summary>
            /// <param name="sessionId"></param>
            /// <param name="eventType"></param>
            /// <param name="properties"></param>
            /// <param name="previousValues"></param>
            public void OnSessionEvent(ISessionId sessionId, SessionPropertiesListenerEventType? eventType, IDictionary<string, string> properties, IDictionary<string, string> previousValues)
            {
                if (eventType.HasValue)
                {
                    WriteLine($"Session with id '{sessionId}' was {eventType.Value}.");
                }
            }

            /// <summary>
            /// Notification that a new client session has been opened.
            /// </summary>
            /// <param name="sessionId"></param>
            /// <param name="properties"></param>
            public void OnSessionOpen(ISessionId sessionId, IDictionary<string, string> properties)
            {
                WriteLine($"Session with id '{sessionId}' has been opened.");
            }
        }
    }
}
