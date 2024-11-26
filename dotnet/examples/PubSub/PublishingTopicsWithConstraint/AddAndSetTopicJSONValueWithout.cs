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
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;
using PushTechnology.ClientInterface.Client.Features;

namespace PushTechnology.ClientInterface.Examples.PubSub.PublishingTopicsWithConstraint
{
     public sealed class AddAndSetTopicJSONValueWithout : Example
     {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string topic = "my/topic/path";

            string json = "{\"diffusion\":\"data\"}";
            var topicSpecification = session.TopicControl.NewSpecification(TopicType.JSON);
            var constraint = Diffusion.UpdateConstraints.JSONValue.Without("/bar");
            var result = await session.TopicUpdate.AddAndSetAsync(topic, topicSpecification, Diffusion.DataTypes.JSON.FromJSONString(json), cancellationToken);

            if (result == TopicCreationResult.CREATED)
            {
                WriteLine("Topic has been created.");
            }
            else
            {
                throw new Exception("Topic failed to be created.");
            }

            string json2 = "{\"diffusion\":\"baz\"}";
            result = await session.TopicUpdate.AddAndSetAsync(topic, topicSpecification, Diffusion.DataTypes.JSON.FromJSONString(json2), constraint, cancellationToken);

            if (result == TopicCreationResult.EXISTS)
            {
                WriteLine("Topic already exists.");
            }
            else
            {
                throw new Exception("Topic should exist already.");
            }

            session.Close();
        }
    }
}
