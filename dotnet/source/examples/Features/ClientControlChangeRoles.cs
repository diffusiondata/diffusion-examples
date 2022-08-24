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
    /// Client implementation that changes the assigned roles of a session using the session id.
    /// </summary>
    public sealed class ClientControlChangeRoles : IExample {
        /// <summary>
        /// Runs the client control example that changes the assigned roles of a session using the session id.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal("control").Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open( serverUrl );

            ISession adminSession = Diffusion.Sessions.Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var adminSecurity = adminSession.Security;

            var clientControl = session.ClientControl;

            try
            {
                //Get the permissions assigned to the session
                var result = await adminSecurity.GetGlobalPermissionsAsync();

                WriteLine("Permissions for the Administrator session are:");

                foreach (var permission in result)
                {
                    WriteLine($"{permission}");
                }
            }
            catch(Exception ex)
            {
                WriteLine($"Failed to get global permissions : {ex}.");
                adminSession.Close();
                session.Close();
                return;
            }

            try
            {
                WriteLine("Removing the Administrator role for the session.");

                await clientControl.ChangeRolesAsync(adminSession.SessionId, new Collection<string>() { "ADMINISTRATOR" }, new Collection<string>());
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to change roles : {ex}.");
                adminSession.Close();
                session.Close();
                return;
            }

            try
            {
                var result = await adminSecurity.GetGlobalPermissionsAsync();

                WriteLine($"Session now has {result.Count} permissions.");
            }
            catch(Exception ex)
            {
                WriteLine($"Failed to get global permissions : {ex}.");
            }

            // Close the sessions
            adminSession.Close();
            session.Close();
        }
    }
}
