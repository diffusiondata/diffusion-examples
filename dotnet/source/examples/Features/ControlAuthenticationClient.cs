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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// An example of using a control client to alter the system authentication
    /// configuration.
    /// </summary>
    public sealed class ControlClientChangingSystemAuthentication : IExample
    {
        private ISystemAuthenticationControl sysAuthCtrl;
        private IScriptBuilder emptyScript;

        public async Task Run(CancellationToken cancel, string[] args) {
            string serverUrl = args[0];

            // Connect as a control session
            var session = Diffusion.Sessions.Principal("control").Password("password").Open(serverUrl);

            sysAuthCtrl = session.SystemAuthenticationControl;
            emptyScript = sysAuthCtrl.Script;

            var sysAuth = await sysAuthCtrl.GetSystemAuthenticationAsync();

            var principals = sysAuth.Principals.AsEnumerable();

            // For each principal that has the SUPERUSER assigned role
            var filteredSUPrincipals = principals.Where(p => p.AssignedRoles.Contains("SUPERUSER"));

            string replacedPrincipalsScript = filteredSUPrincipals.Select(p =>
            {
                var newRoles = new HashSet<string>();

                foreach (string r in p.AssignedRoles)
                {
                    newRoles.Add(r);
                }

                newRoles.Remove("SUPERUSER");
                newRoles.Add("ADMIN");

                return emptyScript.AssignRoles(p.Name, newRoles);
            }).Aggregate(emptyScript,(sb1,sb2) => sb2.Append(sb1)).ToScript();

            await sysAuthCtrl.UpdateStoreAsync(replacedPrincipalsScript);
        }
    }
}
