/**
 * Copyright © 2025 - 2026 Diffusion Data Ltd.
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
using static System.Console;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
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

            // The server evaluates alerts periodically, so the alert topic appears some time after
            // SetMetricAlertAsync completes. Subscribe first and wait for its first value.
            var alertStream = new AlertStream();
            session.Topics.AddStream("my/topic/path", alertStream);
            await session.Topics.SubscribeAsync("my/topic/path", cancellationToken);

            await session.Metrics.SetMetricAlertAsync("myAlert", "select os_system_cpu_load into topic my/topic/path");

            WriteLine("Alert created");

            var alertValue = await alertStream.FirstValue.WaitAsync(TimeSpan.FromSeconds(30), cancellationToken);

            string topicValue = alertValue.ToJSONString();

            WriteLine($"Topic value: {topicValue}");

            session.Close();
        }

        private sealed class AlertStream : IValueStream<IJSON>
        {
            private readonly TaskCompletionSource<IJSON> firstValue =
                new TaskCompletionSource<IJSON>(TaskCreationOptions.RunContinuationsAsynchronously);

            public Task<IJSON> FirstValue => firstValue.Task;

            public void OnClose() {}

            public void OnError(ErrorReason errorReason) {}

            public void OnSubscription(string topicPath, ITopicSpecification specification) {}

            public void OnUnsubscription(string topicPath, ITopicSpecification specification, TopicUnsubscribeReason reason) {}

            public void OnValue(string topicPath, ITopicSpecification specification, IJSON oldValue, IJSON newValue)
            {
                firstValue.TrySetResult(newValue);
            }
        }
    }
}
