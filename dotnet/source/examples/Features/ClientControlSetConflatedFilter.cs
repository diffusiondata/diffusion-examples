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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that sets the queue conflation of a session using a filter.
    /// </summary>
    public sealed class ClientControlSetConflatedFilter : IExample
    {
        /// <summary>
        /// Runs the client control example that sets the queue conflation of a session using a filter.
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

            ISession session2 = Diffusion.Sessions.Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .NoReconnection()
                .Open(serverUrl);

            WriteLine($"Session with id '{session2.SessionId}' created.");

            try
            {
                await clientControl.SetSessionPropertiesAsync(session.SessionId, new Dictionary<string, string> { { "$Country", "UK" } });

                await clientControl.SetSessionPropertiesAsync(session2.SessionId, new Dictionary<string, string> { { "$Country", "US" } });
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to set the properties of a session : {ex}.");
                session.Close();
                session2.Close();
                controlSession.Close();
                return;
            }

            try
            {
                var matchedSessions = await clientControl.SetConflatedAsync("$Country is 'UK'", true);

                WriteLine($"Total clients with queue conflation enabled by session filter: {matchedSessions}.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to set queue conflation by filter : {ex}.");
            }

            // Close the sessions
            session.Close();
            session2.Close();
            controlSession.Close();
        }
    }
}
