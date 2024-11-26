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
using System.Collections.Generic;
using static System.Console;
using System.Linq;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static PushTechnology.ClientInterface.Examples.Program;
using NUnit.Framework;

namespace PushTechnology.ClientInterface.Examples.Monitoring
{
    public sealed class MissingTopicNotifications : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            IRegistration registration = null;
            string topicPath = "my/topic/path";
            string topicSelector = "?my/topic/path//";

            var missingTopicNotificationStream = new MissingTopicNotificationStream(session);
            registration = await session.TopicControl.AddMissingTopicHandlerAsync(topicPath, missingTopicNotificationStream, cancellationToken);

            var session2 = Diffusion.Sessions
                .Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var stringStream = new StringStream();
            session2.Topics.AddStream(topicSelector, stringStream);

            await session2.Topics.SubscribeAsync("my/topic/path/does/not/exist/yet", cancellationToken);

            session.Close();
            session2.Close();
        }

        private sealed class MissingTopicNotificationStream : IMissingTopicNotificationStream
        {
            private ISession session;
            public MissingTopicNotificationStream(ISession session) => this.session = session;

            public void OnClose() {}

            public void OnError(ErrorReason errorReason) {}

            public void OnMissingTopic(IMissingTopicNotification notification)
            {
                WriteLine($"Topic '{notification.TopicPath}' does not exist.");

                session.TopicControl.AddTopicAsync(notification.TopicPath, session.TopicControl.NewSpecification(TopicType.STRING));
            }
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
    }
}
