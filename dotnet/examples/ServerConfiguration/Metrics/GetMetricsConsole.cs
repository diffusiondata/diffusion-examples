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
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;
using PushTechnology.ClientInterface.Client.Factories;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.Metrics
{
    public sealed class GetMetricsConsole : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var requiredMetrics = new HashSet<string>
            {
                "diffusion_server_time_zone",
                "diffusion_server_user_directory",
                "diffusion_server_license_expiry_date",
                "diffusion_server_uptime_millis",
                "diffusion_license",
                "diffusion_server_free_memory_bytes",
                "diffusion_server_max_memory_bytes",
                "diffusion_server_total_memory_bytes",
                "diffusion_release",
                "diffusion_server_start_date",
                "diffusion_server_start_date_millis",
                "diffusion_server_used_swap_space_size_bytes",
                "diffusion_server_used_physical_memory_size_bytes",
                "diffusion_server_number_of_topics",
                "diffusion_server_session_locks",
                "diffusion_server_user_name",
                "diffusion_server_uptime",
                "diffusion_multiplexer_manager_number_of_multiplexers"
            };

            var result = await session.Metrics.MetricsRequest().Filter(requiredMetrics).FetchAsync();

            string serverName = result.ServerNames.First();

            var collections = result.GetMetrics(serverName);

            foreach (var collection in collections)
            {
                foreach (var sample in collection.Samples)
                {
                    WriteLine($"{collection.Name}: {sample.Name} {collection.Unit} ({collection.Type})");
                }
            }

            session.Close();
        }
    }
}
