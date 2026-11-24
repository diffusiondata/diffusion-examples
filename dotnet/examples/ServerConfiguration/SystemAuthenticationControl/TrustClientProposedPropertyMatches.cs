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
    public sealed class TrustClientProposedPropertyMatches : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session1 = Diffusion.Sessions.Principal("admin").Password("password")
                .Open(serverUrl);

            WriteLine($"Trust client proposed property matches.");

            string updateScript = session1.SystemAuthenticationControl.Script
                .TrustClientProposedPropertyMatches("Flintstone", ".*_Flintstone")
                .ToScript();

            await session1.SystemAuthenticationControl.UpdateStoreAsync(updateScript, cancellationToken);

            var session2 = Diffusion.Sessions.Principal("control").Password("password")
                .Property("Flintstone", "Barney_Rubble")
                .Open(serverUrl);

            var requiredProperties = new List<string> { SessionProperty.ALL_FIXED_PROPERTIES };

            var properties = await session1.ClientControl.GetSessionPropertiesAsync(session2.SessionId, requiredProperties);

            foreach (var property in properties)
            {
                WriteLine($"{property.Key}: {property.Value}");
            }

            var session3 = Diffusion.Sessions.Principal("control").Password("password")
                .Property("Flintstone", "Fred_Flintstone")
                .Open(serverUrl);

            properties = await session1.ClientControl.GetSessionPropertiesAsync(session3.SessionId, requiredProperties);

            foreach (var property in properties)
            {
                WriteLine($"{property.Key}: {property.Value}");
            }

            session3.Close();
            session2.Close();
            session1.Close();
        }
    }
}
