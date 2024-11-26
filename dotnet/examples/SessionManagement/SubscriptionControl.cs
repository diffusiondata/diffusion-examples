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
using System.Linq;
using static System.Console;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.SessionManagement
{
    public sealed class SubscriptionControl : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string topic = "my/topic/path/hello";
            string topicSelector = "?my/topic/path//";

            var specification = session.TopicControl.NewSpecification(TopicType.STRING);

            await session.TopicUpdate.AddAndSetAsync(topic, specification, "Hello World!", cancellationToken);

            var parameters = Diffusion.NewSessionEventParametersBuilder()
                .Properties(SessionProperty.ALL_FIXED_PROPERTIES)
                .Build();

            var myEventStream = new MyEventStream(session);
            var registration = await session.ClientControl.AddSessionEventListenerAsync(myEventStream, parameters);

            var session2 = Diffusion.Sessions
                .Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var valueStream = new ValueStream();
            session2.Topics.AddStream(topicSelector, valueStream);

            session2.Close();
            session.Close();
        }

        private class MyEventStream : ISessionEventStream
        {
            private readonly ISession session;

            public MyEventStream(ISession session) => this.session = session;

            public void OnClose() { }

            public void OnError(ErrorReason errorReason) { }

            public void OnSessionEvent(ISessionEventStreamEvent sessionEventStreamEvent)
            {
                if (sessionEventStreamEvent.IsOpenEvent)
                {
                    if (session.SessionId.ToString() != sessionEventStreamEvent.SessionId.ToString())
                    {
                        Thread.Sleep(2000);

                        session.SubscriptionControl.SubscribeAsync(sessionEventStreamEvent.SessionId, "?my/topic/path//");

                        Thread.Sleep(2000);

                        session.SubscriptionControl.UnsubscribeAsync(sessionEventStreamEvent.SessionId, "?my/topic/path//");
                    }
                }
            }
        }

        private sealed class ValueStream : IValueStream<string>
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
                WriteLine($"{topicPath}: {newValue}.");
            }
        }
    }
}
