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
using System.Linq;
using static System.Console;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static PushTechnology.ClientInterface.Examples.Program;
using NUnit.Framework;

namespace PushTechnology.ClientInterface.Examples.Monitoring
{
    public sealed class TopicNotifications : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var topicNotificationListener = new TopicNotificationListener();
            var registration = await session.TopicNotifications.AddListenerAsync(topicNotificationListener, cancellationToken);
            await registration.SelectAsync(">my/topic/path", cancellationToken);

            var specification = session.TopicControl.NewSpecification(TopicType.STRING);

            await session.TopicUpdate.AddAndSetAsync("my/topic/path", specification, "Good morning", cancellationToken);
            await session.TopicUpdate.AddAndSetAsync("my/topic/path/descendant", specification, "Good afternoon", cancellationToken);
            await session.TopicUpdate.AddAndSetAsync("other/path/of/the/topic/tree", specification, "This will not generate a notification", cancellationToken);
            await session.TopicControl.RemoveTopicsAsync("my/topic/path/descendant", cancellationToken);

            session.Close();
        }

        private sealed class TopicNotificationListener : ITopicNotificationListener
        {
            public void OnClose() {}

            public void OnError(ErrorReason errorReason) {}

            public void OnDescendantNotification(string topicPath, NotificationType type)
            {
                if (type == NotificationType.ADDED)
                {
                    WriteLine($"Descendant Topic {topicPath} has been added.");
                }
                if (type == NotificationType.SELECTED)
                {
                    WriteLine($"Descendant Topic {topicPath} has been selected.");
                }
                if (type == NotificationType.DESELECTED)
                {
                    WriteLine($"Descendant Topic {topicPath} has been deselected.");
                }
                if (type == NotificationType.REMOVED)
                {
                    WriteLine($"Descendant Topic {topicPath} has been removed.");
                }
            }

            public void OnTopicNotification(string topicPath, ITopicSpecification specification, NotificationType type)
            {
                if (type == NotificationType.ADDED) 
                {
                    WriteLine($"Topic {topicPath} has been added.");
                }
                if (type == NotificationType.SELECTED) 
                {
                    WriteLine($"Topic {topicPath} has been selected.");
                }
                if (type == NotificationType.DESELECTED) 
                {
                    WriteLine($"Topic {topicPath} has been deselected.");
                }
                if (type == NotificationType.REMOVED) 
                {
                    WriteLine($"Topic {topicPath} has been removed.");
                }
            }
        }
    }
}
