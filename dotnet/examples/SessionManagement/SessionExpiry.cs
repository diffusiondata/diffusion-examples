/**
 * Copyright © 2025 - 2026 Diffusion Data Ltd.
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
    public sealed class SessionExpiry : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            ISession controlSession = null;
            IRegistration registration = null;

            var authenticator = new Authenticator(3000);

            try
            {
                controlSession = Diffusion.Sessions
                    .Principal("control")
                    .Password("password")
                    .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                    .Open(serverUrl);

                registration = await controlSession.AuthenticationControl.SetAuthenticationHandlerAsync("before-system-handler", authenticator, cancellationToken);

                await Task.Delay(5000);
            }
            catch (Exception ex)
            {
                WriteLine($"An error occurred when running the example : {ex}.");
            }

            var session = Diffusion.Sessions.Principal("control")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var properties = await session.GetSessionPropertiesAsync();
            WriteLine($"Expiry time in milliseconds is: {properties[SessionProperty.EXPIRY_TIME]}");
            
            // Wait until the session expires
            await Task.Delay(5000);

            WriteLine($"Session state is: {session.State}");
            
            var closeRegistrationTask = registration.CloseAsync();
            if (await Task.WhenAny(closeRegistrationTask, Task.Delay(30_000)) != closeRegistrationTask)
            {
                throw new TimeoutException("registration.CloseAsync() did not complete within 30s");
            }
            await closeRegistrationTask;
            await Task.Delay(2000);

            session.Close();
            controlSession.Close();
        }

        private sealed class Authenticator : IControlAuthenticator
        {
            private long expireAfterMs;

            public Authenticator(long expireAfterMs) => this.expireAfterMs = expireAfterMs;

            public void Authenticate(
                string principal,
                ICredentials credentials,
                IReadOnlyDictionary<string, string> sessionProperties,
                IReadOnlyDictionary<string, string> proposedProperties,
                IAuthenticatorCallback callback)
            {
                if ("denyme".Equals(principal))
                {
                    callback.Deny();
                }
                else
                {
                    WriteLine("Session establishment accepted.");

                    // Passes through all user defined properties
                    var newProperties = new Dictionary<string, string>(proposedProperties);

                    long currentTimeMillis = DateTimeOffset.Now.ToUnixTimeMilliseconds();
                    long expiryTime = currentTimeMillis + expireAfterMs;
                    
                    newProperties.Add(SessionProperty.EXPIRY_TIME, expiryTime.ToString());

                    callback.Allow(newProperties);
                }
            }

            public void OnClose() { }

            public void OnError(ErrorReason errorReason) { }
        }
    }
}