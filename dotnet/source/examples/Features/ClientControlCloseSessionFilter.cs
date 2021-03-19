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
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that closes a session using a filter.
    /// </summary>
    public sealed class ClientControlCloseSessionFilter : IExample {
        /// <summary>
        /// Runs the client control example that closes a session with a filter.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal("control").Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open( serverUrl );

            var clientControl = session.ClientControl;

            ISession clientSession = Diffusion.Sessions.Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .NoReconnection()
                .Open(serverUrl);

            WriteLine($"Session with id '{clientSession.SessionId}' created.");

            try
            {
                WriteLine($"Closing session with id '{clientSession.SessionId}'");

                var result = await clientControl.CloseAsync("$SessionId is \"" + clientSession.SessionId + "\"");

                WriteLine($"Number of sessions closed: {result}");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to close session with id '{clientSession.SessionId}' : {ex}.");
            }

            // Close the session
            session.Close();
        }
    }
}
