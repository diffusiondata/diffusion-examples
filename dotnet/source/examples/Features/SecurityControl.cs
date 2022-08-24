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
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients.SecurityControl;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Types;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that uses the security control.
    /// </summary>
    public sealed class SecurityControl : IExample
    {
        /// <summary>
        /// Runs the security control example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancel, string[] args)
        {
            string serverUrl = args[0];

            // Connect as a control session
            var session = Diffusion.Sessions.Principal("admin").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            string role = "ADMINISTRATOR";

            IReadOnlyCollection<GlobalPermission> defaultPermissions = null;
            ISecurityConfiguration securityConfig = null;

            try
            {
                //Get the default global permissions for the Admin role
                securityConfig = await session.SecurityControl.GetSecurityAsync();

                var adminRole = securityConfig.Roles.Where(x => x.Name == role).FirstOrDefault();
                defaultPermissions = adminRole.GlobalPermissions;

                WriteLine($"The Administrator role has the following global permissions by default:");

                foreach (var permission in defaultPermissions)
                {
                    WriteLine($"'{permission}'");
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to get global permissions : {ex}.");
            }

            try
            {
                //Add the following global permissions for the Admin role
                var permissions = new List<GlobalPermission>(defaultPermissions);
                permissions.AddRange(new[] { GlobalPermission.REGISTER_HANDLER, GlobalPermission.VIEW_SESSION });

                WriteLine($"Adding further permissions...");

                string script = session.SecurityControl.Script.SetGlobalPermissions(role, permissions).ToScript();

                await session.SecurityControl.UpdateStoreAsync(script);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to set global permissions : {ex}.");
            }

            try
            {
                //Get the current global permissions for the Admin role
                securityConfig = await session.SecurityControl.GetSecurityAsync();

                var adminRole = securityConfig.Roles.Where(x => x.Name == role).FirstOrDefault();

                WriteLine($"The Administrator role now has the following global permissions:");

                foreach (var permission in adminRole.GlobalPermissions)
                {
                    WriteLine($"'{permission}'");
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to get global permissions : {ex}.");
            }

            try
            {
                WriteLine($"Reseting to the original state.");

                string script = session.SecurityControl.Script.SetGlobalPermissions(role, defaultPermissions).ToScript();

                await session.SecurityControl.UpdateStoreAsync(script);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to set global permissions : {ex}.");
            }

            // Create a new role

            string testRole = "TestRole";

            try
            {
                var defaultPermission = new[] { GlobalPermission.REGISTER_HANDLER };

                WriteLine($"Creating role '{testRole}'.");

                string storeScript = session.SecurityControl.Script
                                    .SetGlobalPermissions(testRole, defaultPermission)
                                    .ToScript();

                await session.SecurityControl.UpdateStoreAsync(storeScript);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to create role : {ex}.");
            }

            //Get the current global permissions for the new role

            try
            {
                securityConfig = await session.SecurityControl.GetSecurityAsync();

                var newRole = securityConfig.Roles.Where(x => x.Name == testRole).FirstOrDefault();

                WriteLine($"'{testRole}' has been created with the following global permissions:");

                foreach (var permission in newRole.GlobalPermissions)
                {
                    WriteLine($"'{permission}'");
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to get global permissions : {ex}.");
            }

            // Create a new principal

            string testPrincipal = "TestPrincipal";

            try
            {
                WriteLine($"Creating principal '{testPrincipal}'.");

                string storeScript = session.SystemAuthenticationControl.Script
                    .AddPrincipal(testPrincipal, "password", new List<string>())
                    .TrustClientProposedPropertyIn("Foo", new List<string> { "value1", "value2" })
                    .TrustClientProposedPropertyMatches("Bar", "regex1")
                    .ToScript();

                await session.SystemAuthenticationControl.UpdateStoreAsync(storeScript);

                WriteLine($"'{testPrincipal}' has been created.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to create principal : {ex}.");
            }

            //Assign roles to the principal

            try
            {
                WriteLine($"Adding the roles of Administrator, Modify Session and TestRole to '{testPrincipal}'.");

                string script1 = session.SystemAuthenticationControl.Script
                    .AssignRoles(testPrincipal, new[] { "ADMINISTRATOR", "MODIFY_SESSION", testRole })
                    .ToScript();

                await session.SystemAuthenticationControl.UpdateStoreAsync(script1);

                var sysAuth = await session.SystemAuthenticationControl.GetSystemAuthenticationAsync();

                var principals = sysAuth.Principals.AsEnumerable();

                WriteLine($"The following principals now exist:");

                foreach (var principal in principals)
                {
                    WriteLine($"'{principal.Name}' has the following roles:");

                    foreach (string assignedRole in principal.AssignedRoles)
                    {
                        WriteLine($"'{assignedRole}'");
                    }
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to assign roles : {ex}.");
            }

            // Remove the new principal

            try
            {
                WriteLine($"Removing '{testPrincipal}'.");

                string storeScript = session.SystemAuthenticationControl.Script
                    .RemovePrincipal(testPrincipal)
                    .ToScript();

                await session.SystemAuthenticationControl.UpdateStoreAsync(storeScript);

                WriteLine($"'{testPrincipal}' has been removed.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to remove principal : {ex}.");
            }
            finally
            {
                session.Close();
                session = null;
            }
        }
    }
}