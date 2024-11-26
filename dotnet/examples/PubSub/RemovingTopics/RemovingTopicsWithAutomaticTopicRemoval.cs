/**
 * Copyright © 2024 Diffusion Data Ltd.
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
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.PubSub.RemovingTopics
{
    public sealed class RemovingTopicsWithAutomaticTopicRemoval : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            await AddTopic(session, "my/topic/path/to/be/removed/time/after", "when time after 'Tue, 4 May 2077 11:05:30 GMT'", cancellationToken);
            await AddTopic(session, "my/topic/path/to/be/removed/subscriptions", "when subscriptions < 1 for 10m", cancellationToken);
            await AddTopic(session, "my/topic/path/to/be/removed/local/subscriptions", "when local subscriptions < 1 for 10m", cancellationToken);

            await AddTopic(session, "my/topic/path/to/be/removed/no/updates", "when no updates for 10m", cancellationToken);
            await AddTopic(session, "my/topic/path/to/be/removed/no/session", "when no session has '$Principal is \"client\"' for 1h", cancellationToken);
            await AddTopic(session, "my/topic/path/to/be/removed/no/local/session", "when no local session has 'Department is \"Accounts\"' for 1h after 1d", cancellationToken);

            await AddTopic(session, "my/topic/path/to/be/removed/subcriptions/or/updates", "when subscriptions < 1 for 10m or no updates for 20m", cancellationToken);
            await AddTopic(session, "my/topic/path/to/be/removed/subcriptions/and/updates", "when subscriptions < 1 for 10m and no updates for 20m", cancellationToken);

            session.Close();
        }

        private async Task AddTopic(ISession session, string topic, string removalValue, CancellationToken cancellationToken)
        {
            var topicSpecification = session.TopicControl.NewSpecification(TopicType.JSON).WithProperty(TopicSpecificationProperty.Removal, removalValue);

            var result = await session.TopicControl.AddTopicAsync(topic, topicSpecification, cancellationToken);

            if (result == AddTopicResult.CREATED)
            {
                WriteLine("Topic has been created.");
            }
            else
            {
                WriteLine("Topic already exists.");
            }
        }
    }
}
