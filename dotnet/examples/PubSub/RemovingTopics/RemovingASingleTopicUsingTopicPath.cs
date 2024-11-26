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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using PushTechnology.ClientInterface.Data.JSON;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.PubSub.RemovingTopics
{
     public sealed class RemovingASingleTopicUsingTopicPath: Example
     {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string topic = "my/topic/path/to/be/removed";

            var topicSpecification = session.TopicControl.NewSpecification(TopicType.JSON);

            await AddAndSetTopic(session, topic, topicSpecification, "{\"diffusion\":[\"data\", \"more data\"]}", cancellationToken);
            await AddAndSetTopic(session, "my/topic/path/will/not/be/removed", topicSpecification, "{\"diffusion\":[\"no data\"]}", cancellationToken);
            await AddAndSetTopic(session, "my/topic/path/will/not/be/removed/either", topicSpecification, "{\"diffusion\":[\"no data either\"]}", cancellationToken);

            await session.TopicControl.RemoveTopicsAsync(topic, cancellationToken);

            WriteLine("Topic has been removed.");

            session.Close();
        }

        private async Task AddAndSetTopic(ISession session, string topic, ITopicSpecification topicSpecification, string json, CancellationToken cancellationToken)
        {
            var result = await session.TopicControl.AddTopicAsync(topic, topicSpecification, cancellationToken);

            if (result == AddTopicResult.CREATED)
            {
                WriteLine("Topic has been created.");
            }
            else
            {
                WriteLine("Topic already exists.");
            }

            await session.TopicUpdate.SetAsync<IJSON>(topic, Diffusion.DataTypes.JSON.FromJSONString(json), cancellationToken);
        }
    }
}
