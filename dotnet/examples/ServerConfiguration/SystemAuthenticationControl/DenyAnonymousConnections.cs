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
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.SystemAuthenticationControl
{
    public sealed class DenyAnonymousConnections : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions.Principal("admin").Password("password")
                .Open(serverUrl);

            WriteLine($"Deny anonymous connections.");

            string updateScript = session.SystemAuthenticationControl.Script
                .DenyAnonymousConnections()
                .ToScript();

            await session.SystemAuthenticationControl.UpdateStoreAsync(updateScript, cancellationToken);

            try
            {
                var session2 = Diffusion.Sessions.Open(serverUrl);
            }
            catch (Exception ex)
            {
                WriteLine($"{ex.Message}");
            }

            session.Close();
        }
    }
}
