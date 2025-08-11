/**
 * Copyright © 2023 - 2024 Diffusion Data Ltd.
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
using System.Linq;
using static System.Console;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Security.Authentication;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.DiffusionCore.Client.Types;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.SessionManagement
{
    public sealed class AuthenticationControl : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            ISession session1 = null, session2 = null, session3 = null, session4 = null;
            IRegistration registration = null;

            var authenticator = new Authenticator();

            try
            {
                session1 = Diffusion.Sessions
                    .Principal("control")
                    .Password("password")
                    .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                    .Open(serverUrl);

                registration = await session1.AuthenticationControl.SetAuthenticationHandlerAsync("before-system-handler", authenticator, cancellationToken);

                await Task.Delay(5000);
            }
            catch (Exception ex)
            {
                WriteLine($"An error occurred when running the example : {ex}.");
            }

            try
            {
                session2 = Diffusion.Sessions.Open(serverUrl);

                await Task.Delay(5000);
            }
            catch(Exception ex)
            {
                WriteLine($"Session creation failed due to authentication denied : {ex}.");
            }

            try
            {
                session3 = Diffusion.Sessions.Principal("control")
                    .Credentials(Diffusion.Credentials.Password("password"))
                    .Open(serverUrl);

                await Task.Delay(5000);
            }
            catch (Exception ex)
            {
                WriteLine($"Session creation failed due to authentication denied : {ex}.");
            }

            try
            {
                session4 = Diffusion.Sessions.Principal("diffusion_control")
                    .Credentials(Diffusion.Credentials.Password("password"))
                    .Open(serverUrl);

                await Task.Delay(5000);
            }
            catch (Exception ex)
            {
                WriteLine($"An error occurred when running the example : {ex}.");
            }

            await registration.CloseAsync();

            session4?.Close();
            session3?.Close();
            session2?.Close();
            session1.Close();
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
                if (principal.StartsWith("diffusion_"))
                {
                    WriteLine("Principal begins with diffusion_ prefix. Session establishment Accepted.");

                    callback.Allow();
                }
                else
                {
                    if (string.IsNullOrEmpty(principal))
                    {
                        WriteLine("Anonymous connection attempt detected. Session establishment Rejected.");
                    }
                    else
                    {
                        WriteLine("Principal does not begins with diffusion_ prefix. Session establishment Rejected.");
                    }

                    callback.Deny();
                }
            }

            public void OnClose() { }

            public void OnError(ErrorReason errorReason) { }
        }
    }
}