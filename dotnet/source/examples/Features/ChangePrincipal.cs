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
using System.Collections.ObjectModel;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that changes the principal of a session.
    /// </summary>
    public sealed class ChangePrincipal : IExample {
        /// <summary>
        /// Runs the client control example that changes the principal of a session.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string serverUrl = args[ 0 ];

            var session = Diffusion.Sessions.Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var security = session.Security;

            try
            {
                WriteLine($"Changing the security principal of the session to 'control'.");

                bool result = await security.ChangePrincipalAsync("control", Diffusion.Credentials.Password("password"));

                string stringResult = result ? "succeeded" : "failed";

                WriteLine($"Principal change {stringResult}.");
            }
            catch(Exception ex)
            {
                WriteLine($"An error occurred while trying to change principal : {ex}.");
            }
            finally
            {
                session.Close();
            }
        }
    }
}
