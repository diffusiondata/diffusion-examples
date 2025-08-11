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
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;


namespace PushTechnology.ClientInterface.Examples.SessionManagement.ClientControl
{
    public sealed class SetSessionPropertiesViaSessionID : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var session2 = Diffusion.Sessions
                .Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var requiredProperties = new List<string> { SessionProperty.ALL_FIXED_PROPERTIES };

            var properties = await session.ClientControl.GetSessionPropertiesAsync(session2.SessionId, requiredProperties, cancellationToken);

            WriteLine("Original session properties:");

            foreach (var property in properties)
            {
                WriteLine($"{property.Key}: {property.Value}");
            }

            await session.ClientControl.SetSessionPropertiesAsync(session2.SessionId, new Dictionary<string, string> { { "$Language", "en-gb" } }, cancellationToken);

            properties = await session.ClientControl.GetSessionPropertiesAsync(session2.SessionId, requiredProperties, cancellationToken);

            WriteLine("Changed session properties:");

            foreach (var property in properties)
            {
                WriteLine($"{property.Key}: {property.Value}");
            }

            session2.Close();
            session.Close();
        }
    }
}
