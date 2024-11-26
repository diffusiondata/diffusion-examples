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
using System.Linq;
using static System.Console;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Topics;
using static PushTechnology.ClientInterface.Examples.Program;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using PushTechnology.ClientInterface.Data.JSON;

namespace PushTechnology.ClientInterface.Examples.Wrangling.TopicViews.DSL
{
    public sealed class SourcePathDirective : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string topic = "a/b/c/d/e/f/g";
            string topicSelector = "?views//";

            string json = "{\"account\":\"1234\",\"balance\":{\"amount\":12.57,\"currency\":\"USD\"}}";
            var topicSpecification = session.TopicControl.NewSpecification(TopicType.JSON);
            await session.TopicUpdate.AddAndSetAsync(topic, topicSpecification, Diffusion.DataTypes.JSON.FromJSONString(json), cancellationToken);

            var jsonStream = new JSONStream();
            session.Topics.AddStream(topicSelector, jsonStream);

            await session.Topics.SubscribeAsync(topicSelector, cancellationToken);

            var view1 = await session.TopicViews.CreateTopicViewAsync("topic_view_1", "map a/b/c/d/e/f/g to views/<path(0)>", cancellationToken);
            WriteLine($"Topic View {view1.Name} has been created.");

            var view2 = await session.TopicViews.CreateTopicViewAsync("topic_view_2", "map a/b/c/d/e/f/g to views/<path(2)>", cancellationToken);
            WriteLine($"Topic View {view2.Name} has been created.");

            var view3 = await session.TopicViews.CreateTopicViewAsync("topic_view_3", "map a/b/c/d/e/f/g to views/<path(3,5)>", cancellationToken);
            WriteLine($"Topic View {view3.Name} has been created.");
            
            session.Close();
        }

        private sealed class JSONStream : IValueStream<IJSON>
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

            public void OnValue(string topicPath, ITopicSpecification specification, IJSON oldValue, IJSON newValue)
            {
                WriteLine($"{topicPath} changed from {(oldValue == null ? "NULL" : oldValue.ToJSONString())} to {newValue.ToJSONString()}.");
            }
        }
    }
}
