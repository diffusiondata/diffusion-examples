/**
 * Copyright © 2025 Diffusion Data Ltd.
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

using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using static System.Console;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Data.JSON;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.Metrics.MetricAlerts
{
    public sealed class SetMetricAlert : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            await session.Metrics.SetMetricAlertAsync("myAlert", "select os_system_cpu_load into topic my/topic/path");

            WriteLine("Alert created");

            await Task.Delay(5000);

            var fetchResult = await session.Topics.FetchRequest.WithValues<IJSON>().FetchAsync("my/topic/path", cancellationToken);
            
            string topicValue = fetchResult.Results.First().Value.ToJSONString();

            WriteLine($"Topic value: {topicValue}");

            session.Close();
        }
    }
}
