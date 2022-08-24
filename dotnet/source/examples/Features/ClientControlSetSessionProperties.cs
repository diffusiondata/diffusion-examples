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
    /// Client implementation that sets the property values of a session.
    /// </summary>
    public sealed class ClientControlSetSessionProperties : IExample
    {
        /// <summary>
        /// Runs the client control example that sets the property values of a session.
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

            Dictionary<string, string> properties = new Dictionary<string, string> { { "$Latitude", "51.509865" } };

            try
            {
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
    }
}
