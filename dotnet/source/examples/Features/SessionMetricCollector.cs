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
using System.Collections.Generic;
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
    /// Client implementation that demonstrates the session metric collector.
    /// </summary>
    public sealed class SessionMetricCollector : IExample
    {
        /// <summary>
        /// Runs the session metric collector example.
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
            ISessionMetricCollector collector = null;
            string sessionFilter = "x is 'y'";

            try
            {
                WriteLine($"Adding the session metric collector 'Test' with session filter '{sessionFilter}'.");

                collector = Diffusion.NewSessionMetricCollectorBuilder()
                    .GroupByProperties(new List<string> { "$Location" })
                    .RemoveMetricsWithNoMatches(true)
                    .Create("Test", sessionFilter);

                await metrics.PutSessionMetricCollectorAsync(collector);

                WriteLine($"Session metric collector '{collector.Name}' added.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add session metric collector : {ex}.");
                session.Close();
                return;
            }

            try
            {
                WriteLine($"The following session metric collectors exist:");

                var listSessionMetricCollectors = await metrics.ListSessionMetricCollectorsAsync();

                foreach (var sessionMetricCollector in listSessionMetricCollectors)
                {
                    WriteLine($"Name: '{sessionMetricCollector.Name}', Session filter: '{sessionMetricCollector.SessionFilter}', Exports to Prometheus: '{GetAnswer(sessionMetricCollector.ExportsToPrometheus)}', Removes metrics with no matches: '{GetAnswer(sessionMetricCollector.RemovesMetricsWithNoMatches)}'");

                    foreach (string property in sessionMetricCollector.GroupByProperties)
                    {
                        WriteLine($"Group by: '{property}' property");
                    }
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to list session metric collectors : {ex}.");
                session.Close();
                return;
            }

            try
            {
                await metrics.RemoveSessionMetricCollectorAsync(collector.Name);

                WriteLine($"Collector '{collector.Name}' removed.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to remove session metric collector : {ex}.");
            }

            // Close the session
            session.Close();
        }

        private string GetAnswer(bool result) => result ? "Yes" : "No";
    }
}