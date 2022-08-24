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
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Metrics;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that demonstrates the topic metric collector.
    /// </summary>
    public sealed class TopicMetricCollector : IExample
    {
        /// <summary>
        /// Runs the topic metric collector example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];
            var session = Diffusion.Sessions.Principal("admin").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var metrics = session.Metrics;
            ITopicMetricCollector collector = null;
            string topicSelector = "selector";

            try
            {
                WriteLine($"Adding the topic metric collector 'Test' with topic selector '{topicSelector}'.");

                var builder = Diffusion.NewTopicMetricCollectorBuilder();
                builder = (ITopicMetricCollectorBuilder)builder.ExportsToPrometheus(true);
                builder = (ITopicMetricCollectorBuilder)builder.GroupByTopicType(true);
                builder = (ITopicMetricCollectorBuilder)builder.MaximumGroups(10);
                builder = (ITopicMetricCollectorBuilder)builder.GroupByPathPrefixParts(1);
                collector = builder.Create("Test", topicSelector);

                await metrics.PutTopicMetricCollectorAsync(collector);

                WriteLine($"Topic metric collector '{collector.Name}' added.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic metric collector : {ex}.");
                session.Close();
                return;
            }

            try
            {
                WriteLine($"The following topic metric collectors exist:");

                var listTopicMetricCollectors = await metrics.ListTopicMetricCollectorsAsync();

                foreach (var topicMetricCollector in listTopicMetricCollectors)
                {
                    WriteLine($"Name: '{topicMetricCollector.Name}', Topic selector: '{topicMetricCollector.TopicSelector}', " +
                              $"Maximum Groups: {topicMetricCollector.MaximumGroups}, " +
                              $"Exports to Prometheus: '{GetAnswer(topicMetricCollector.ExportsToPrometheus)}', " +
                              $"Group By Path Prefix Parts: {topicMetricCollector.GroupByPathPrefixParts}, " +
                              $"Groups by topic type: '{GetAnswer(topicMetricCollector.GroupsByTopicType)}'");
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to list topic metric collectors : {ex}.");
                session.Close();
                return;
            }

            try
            {
                await metrics.RemoveTopicMetricCollectorAsync(collector.Name);

                WriteLine($"Collector '{collector.Name}' removed.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to remove topic metric collector : {ex}.");
            }

            // Close the session
            session.Close();
        }

        private string GetAnswer(bool result) => result ? "Yes" : "No";
    }
}