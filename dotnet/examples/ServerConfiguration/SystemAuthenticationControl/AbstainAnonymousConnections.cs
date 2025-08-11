/**
 * Copyright © 2024 Diffusion Data Ltd.
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Security.Authentication;
using PushTechnology.DiffusionCore.Client.Types;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.SystemAuthenticationControl
{
    public sealed class AbstainAnonymousConnections : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions.Principal("admin").Password("password")
                .Open(serverUrl);

            WriteLine($"Abstain anonymous connections.");

            string updateScript = session.SystemAuthenticationControl.Script
                .AbstainAnonymousConnections()
                .ToScript();

            await session.SystemAuthenticationControl.UpdateStoreAsync(updateScript, cancellationToken);


            var session2 = Diffusion.Sessions.Principal("control").Password("password")
            .Open(serverUrl);

            var authenticator = new Authenticator();

            var registration = await session2.AuthenticationControl.SetAuthenticationHandlerAsync("after-system-handler", authenticator, cancellationToken);

            try
            {
                var session3 = Diffusion.Sessions.Open(serverUrl);
            }
            catch (Exception ex)
            {
                WriteLine($"{ex.Message}");
            }

            session2.Close();
            session.Close();
        }

        private sealed class Authenticator : IControlAuthenticator
        {

            public void Authenticate(
                string principal,
                ICredentials credentials,
                IReadOnlyDictionary<string, string> sessionProperties,
                IReadOnlyDictionary<string, string> proposedProperties,
                IAuthenticatorCallback callback)
            {
                if (string.IsNullOrEmpty(principal))
                {
                    WriteLine("Anonymous connection attempt detected. Session establishment Rejected.");

                    callback.Deny();
                }
            }

            public void OnClose() { }

            public void OnError(ErrorReason errorReason) { }
        }
    }
}
