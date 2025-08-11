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

using System;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;
using static System.Console;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics.Details;
using PushTechnology.ClientInterface.Client.Topics;
using static PushTechnology.ClientInterface.Examples.Program;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;

namespace PushTechnology.ClientInterface.Examples.PubSub.SubscribingToTopics
{
     public sealed class SubscribeUsingSelectionScopes : Example
     {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
            .Open(serverUrl);

            var topicPath = "my/topic/path";
            var myOtherPath = "my/other/path";

            var topicControl = session.TopicControl;
            var subscriptionControl = session.SubscriptionControl;

            await AddTopic(session, topicPath, TopicType.STRING, cancellationToken);
            await AddTopic(session, myOtherPath, TopicType.STRING, cancellationToken);

            var componentA = session.Topics;
            var componentB = session.Topics;

            var streamA = new MyStream("componentA");
            var streamB = new MyStream("componentB");

            // Each component registers a stream for the topics they are interested in
            // and subscribe to the topics with their own scope
            componentA.AddStream(topicPath, streamA);
            componentB.AddStream("?my//", streamB);

            await componentA.SubscribeAsync(topicPath, "scopeA", cancellationToken);
            await componentB.SubscribeAsync(topicPath, "scopeB", cancellationToken);
            await componentB.SubscribeAsync(myOtherPath, "scopeB", cancellationToken);
            
            var topicSelections = await session.SubscriptionControl.GetTopicSelectionsAsync(session.SessionId);

            foreach (var topicSelection in topicSelections)
            {
                WriteLine($"{topicSelection.Key} scopes");
            }

            // ComponentB unsubscribes from topicPath, scopeA is unaffected
            await componentB.UnsubscribeAsync(topicPath, "scopeB", cancellationToken);

            topicSelections = await session.SubscriptionControl.GetTopicSelectionsAsync(session.SessionId);

            foreach (var topicSelection in topicSelections)
            {
                WriteLine($"{topicSelection.Key} scopes");
            }

            // ComponentA unsubscribes from all topics, scopeB is unaffected
            // topicPath is no longer in any scopes and is unsubscribed for the session
            await componentA.UnsubscribeAsync("?.*//", "scopeA", cancellationToken);

            // ComponentA removes its stream and will no longer receive updates
            componentA.RemoveStream(streamA);

            // A control client unsubscribes all remaining scopes for myOtherPath which
            // is now unsubscribed for the session
            await session.SubscriptionControl.UnsubscribeAllScopesAsync(session.SessionId, myOtherPath);

            componentB.RemoveStream(streamB);
        }

        private async Task AddTopic(ISession session, string topic, TopicType type, CancellationToken cancellationToken)
        {
            var result = await session.TopicControl.AddTopicAsync(topic, type, cancellationToken);

            if (result == AddTopicResult.CREATED)
            {
                WriteLine("Topic has been created.");
            }
            else
            {
                WriteLine("Topic already exists.");
            }
        }

        private sealed class MyStream : IValueStream<string>
        {
            private string scopeName;

            public MyStream(string scopeName) => this.scopeName = scopeName;

            public void OnClose() {
                WriteLine($"{scopeName}: stream closed.");
            }

            public void OnError(ErrorReason errorReason) {}

            public void OnSubscription(string topicPath, ITopicSpecification specification)
            {
                WriteLine($"{scopeName}: Subscribed to {topicPath}.");
            }

            public void OnUnsubscription(string topicPath, ITopicSpecification specification, TopicUnsubscribeReason reason)
            {
                WriteLine($"{scopeName}: Unsubscribed from {topicPath}: {reason}.");
            }

            public void OnValue(string topicPath, ITopicSpecification specification, string oldValue, string newValue)
            {
                WriteLine($"{scopeName}: {topicPath} changed from {(oldValue ?? "NULL")} to {newValue}.");
            }
        }
    }
}
