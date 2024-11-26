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
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.Wrangling.SessionTrees
{
    public sealed class UseCase : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var topicSpecification = session.TopicControl.NewSpecification(TopicType.STRING);
            await session.TopicUpdate.AddAndSetAsync("my/topic/path/for/admin", topicSpecification, "Good morning Administrator", cancellationToken);
            await session.TopicUpdate.AddAndSetAsync("my/topic/path/for/control", topicSpecification, "Good afternoon Control Client", cancellationToken);
            await session.TopicUpdate.AddAndSetAsync("my/topic/path/for/anonymous", topicSpecification, "Good night Anonymous", cancellationToken);

            var table = Diffusion.NewBranchMappingTableBuilder()
                .AddBranchMapping("$Principal is 'admin'", "my/topic/path/for/admin")
                .AddBranchMapping("$Principal is 'control'", "my/topic/path/for/control")
                .AddBranchMapping("$Principal is ''", "my/topic/path/for/anonymous")
                .Create("my/personal/path");

            await session.SessionTrees.PutBranchMappingTableAsync(table, cancellationToken);

            string topicSelector = ">my/personal/path";
            var stringStream = new StringStream();
            session.Topics.AddStream(topicSelector, stringStream);

            await session.Topics.SubscribeAsync(topicSelector, cancellationToken);

            var session2 = Diffusion.Sessions.Open(serverUrl);

            var anotherStringStream = new AnotherStringStream();
            session2.Topics.AddStream(topicSelector, anotherStringStream);

            await session2.Topics.SubscribeAsync(topicSelector, cancellationToken);

            session.Close();
            session2.Close();
        }

        private sealed class StringStream : IValueStream<string>
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

            public void OnValue(string topicPath, ITopicSpecification specification, string oldValue, string newValue)
            {
                WriteLine($"{topicPath} changed from {oldValue ?? "NULL"} to {newValue}.");
            }
        }

        private sealed class AnotherStringStream : IValueStream<string>
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

            public void OnValue(string topicPath, ITopicSpecification specification, string oldValue, string newValue)
            {
                WriteLine($"{topicPath} changed from {oldValue ?? "NULL"} to {newValue}.");
            }
        }
    }
}
