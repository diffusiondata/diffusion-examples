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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Features;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.PubSub.PublishingTopicsWithConstraint
{
     public sealed class AddAndSetTopicOr : Example
     {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var topicSpecification = Diffusion.NewSpecification(TopicType.JSON);

            string topic = "my/topic/path";

            var value = Diffusion.DataTypes.JSON.FromJSONString("{\"diffusion\":\"data\"}");
            var differentValue = Diffusion.DataTypes.JSON.FromJSONString("{\"diffusion\":\"data2\"}");

            var constraint = Diffusion.UpdateConstraints.Value(value).Or(Diffusion.UpdateConstraints.NoTopic);

            // Update the topic, this works as we satisfy the no topic constraint
            var result = await session.TopicUpdate.AddAndSetAsync(topic, topicSpecification, value, constraint, cancellationToken);

            if (result == TopicCreationResult.CREATED)
            {
                WriteLine("Topic has been created.");
            }
            else
            {
                throw new Exception("Topic failed to be created.");
            }

            // Update the topic again, this works as we satisfy the value constraint
            result = await session.TopicUpdate.AddAndSetAsync(topic, topicSpecification, differentValue, constraint, cancellationToken);

            if (result == TopicCreationResult.CREATED)
            {
                WriteLine("Topic has been created.");
            }
            else
            {
                WriteLine("Topic already exists.");
            }
                hasUpdateFailed = true;
            Assert.IsTrue(hasUpdateFailed);
            var removeResult = await session.TopicControl.RemoveTopicsAsync(topic, cancellationToken);
            WriteLine($"Or: {removeResult.ToString()}");
