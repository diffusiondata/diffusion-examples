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
using System.Threading.Tasks;
using System.Threading;
using System.Collections.Generic;
using static System.Console;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Topics;
using static PushTechnology.ClientInterface.Examples.Program;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;

namespace PushTechnology.ClientInterface.Examples.Wrangling.TopicViews.DSL
{
    public sealed class OptionsThrottle : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string topic = "my/topic/path";
            string topicSelector = "?.*//";

            var topicSpecification = session.TopicControl.NewSpecification(TopicType.INT64);

            await session.TopicUpdate.AddAndSetAsync<long?>(topic, topicSpecification, 0, cancellationToken);

            var valueStream = new ValueStream();
            session.Topics.AddStream(topicSelector, valueStream);

            await session.Topics.SubscribeAsync(topicSelector, cancellationToken);

            var view1 = await session.TopicViews.CreateTopicViewAsync("topic_view_1", "map my/topic/path to views/<path(0)> throttle to 1 update every 3 seconds", cancellationToken);
            WriteLine($"Topic View {view1.Name} has been created.");

            for (int i = 0; i < 15; i++)
            {
                await session.TopicUpdate.SetAsync<long?>(topic, DateTimeOffset.Now.ToUnixTimeMilliseconds(), cancellationToken);
                await Task.Delay(1000);
            }

            session.Close();
        }

        private sealed class ValueStream : IValueStream<long?>
        {
            public void OnClose() {}

            public void OnError(ErrorReason errorReason) {}

            public void OnSubscription(string topicPath, ITopicSpecification specification)
            {
                WriteLine($"Subscribed to {topicPath}.");
            }

            public void OnUnsubscription(string topicPath, ITopicSpecification specification, TopicUnsubscribeReason reason)
            {
                WriteLine($"Unsubscribed from {topicPath}: {reason}.");
            }

            public void OnValue(string topicPath, ITopicSpecification specification, long? oldValue, long? newValue)
            {
                WriteLine($"{topicPath} changed from {(oldValue == null ? "NULL" : oldValue.ToString())} to {(newValue == null ? "NULL" : newValue.ToString())}.");
            }
        }
    }
}
