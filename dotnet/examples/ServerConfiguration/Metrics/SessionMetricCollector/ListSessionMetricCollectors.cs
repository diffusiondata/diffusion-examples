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
using System.Threading;
using System.Threading.Tasks;
using static System.Console;
using System.Linq;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Metrics;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.Metrics.SessionMetricCollector
{
    public sealed class ListSessionMetricCollectors : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string sessionFilter = "$Principal is 'control'";

            var builder = Diffusion.NewSessionMetricCollectorBuilder();
            builder = (ISessionMetricCollectorBuilder)builder.ExportsToPrometheus(false);
            builder = builder.GroupByProperties(new List<string> { "$Location" });
            builder = builder.RemoveMetricsWithNoMatches(true);
            builder = (ISessionMetricCollectorBuilder)builder.MaximumGroups(10);
            var collector = builder.Create("Session Metric Collector 1", sessionFilter);

            await session.Metrics.PutSessionMetricCollectorAsync(collector, cancellationToken);

            builder = Diffusion.NewSessionMetricCollectorBuilder();
            builder = (ISessionMetricCollectorBuilder)builder.ExportsToPrometheus(true);
            builder = builder.GroupByProperties(new List<string> { "$Location" });
            builder = builder.RemoveMetricsWithNoMatches(true);
            builder = (ISessionMetricCollectorBuilder)builder.MaximumGroups(250);
            collector = builder.Create("Session Metric Collector 2", sessionFilter);

            await session.Metrics.PutSessionMetricCollectorAsync(collector, cancellationToken);

            var listSessionMetricCollectors = await session.Metrics.ListSessionMetricCollectorsAsync(cancellationToken);

            foreach (var sessionMetricCollector in listSessionMetricCollectors)
            {
                WriteLine($"{sessionMetricCollector.Name}: " +
                    $"{sessionMetricCollector.SessionFilter} " +
                    $"({sessionMetricCollector.MaximumGroups}, " +
                    $"{GetAnswer(sessionMetricCollector.ExportsToPrometheus)}, " +
                    $"{GetAnswer(sessionMetricCollector.RemovesMetricsWithNoMatches)}, " +
                    $"{string.Join(",", sessionMetricCollector.GroupByProperties)})");
            }

            session.Close();
        }

        private string GetAnswer(bool result) => result ? "Yes" : "No";
    }
}
