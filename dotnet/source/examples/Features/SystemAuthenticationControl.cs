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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;
using PushTechnology.ClientInterface.Client.Session;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// An example of altering the system authentication configuration.
    /// </summary>
    public sealed class SystemAuthenticationControl : IExample
    {
        private ISystemAuthenticationControl sysAuthCtrl;
        private IScriptBuilder emptyScript;

        public async Task Run(CancellationToken cancel, string[] args) {
            string serverUrl = args[0];

            // Connect as an admin session
            var session = Diffusion.Sessions.Principal("admin").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            sysAuthCtrl = session.SystemAuthenticationControl;
            emptyScript = sysAuthCtrl.Script;

            IEnumerable<ISystemPrincipal> filteredPrincipals = null;

            try
            {
                var sysAuth = await sysAuthCtrl.GetSystemAuthenticationAsync();

                var principals = sysAuth.Principals.AsEnumerable();

                // For each principal that has the CLIENT assigned role
                filteredPrincipals = principals.Where(p => p.AssignedRoles.Contains("CLIENT"));

                WriteLine($"The following principals have the CLIENT role:");

                foreach (var principal in filteredPrincipals)
                {
                    WriteLine($"'{principal.Name}'.");
                }
            }
            catch(Exception ex)
            {
                WriteLine($"Failed to get principals : {ex}.");
            }

            try
            {
                string newPrincipalsScript = filteredPrincipals.Select(p =>
                {
                    var newRoles = new HashSet<string>();

                    foreach (string r in p.AssignedRoles)
                    {
                        newRoles.Add(r);
                    }

                    newRoles.Add("OPERATOR");

                    return emptyScript.AssignRoles(p.Name, newRoles);
                }).Aggregate(emptyScript, (sb1, sb2) => sb2.Append(sb1)).ToScript();

                await sysAuthCtrl.UpdateStoreAsync(newPrincipalsScript);

                WriteLine($"Adding the OPERATOR role to these principals...");

                var sysAuth = await sysAuthCtrl.GetSystemAuthenticationAsync();

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
            catch(Exception ex)
            {
                WriteLine($"Failed to add roles : {ex}.");
            }
            finally
            {
                session.Close();
            }
        }
    }
}
