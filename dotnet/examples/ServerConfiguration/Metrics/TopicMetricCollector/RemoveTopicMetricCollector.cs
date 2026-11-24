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
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Metrics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.ServerConfiguration.Metrics.TopicMetricCollector
{
    public sealed class RemoveTopicMetricCollector : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            ITopicMetricCollector collector = null;
            string topicSelector = "?my/topic//";

            var builder = Diffusion.NewTopicMetricCollectorBuilder();
            builder = (ITopicMetricCollectorBuilder)builder.ExportsToPrometheus(false);
            builder = builder.GroupByTopicType(true);
            builder = builder.GroupByTopicView(true);
            builder = builder.GroupByPathPrefixParts(15);
            builder = (ITopicMetricCollectorBuilder)builder.MaximumGroups(10);
            collector = builder.Create("Topic Metric Collector 1", topicSelector);

            await session.Metrics.PutTopicMetricCollectorAsync(collector, cancellationToken);

            await session.Metrics.RemoveTopicMetricCollectorAsync(collector.Name, cancellationToken);

            WriteLine($"{collector.Name} has been removed.");

            session.Close();
        }
    }
}
