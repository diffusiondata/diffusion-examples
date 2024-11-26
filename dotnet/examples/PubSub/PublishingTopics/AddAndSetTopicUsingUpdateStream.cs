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
using PushTechnology.ClientInterface.Data.JSON;

namespace PushTechnology.ClientInterface.Examples.PubSub.PublishingTopics
{
     public sealed class AddAndSetTopicUsingUpdateStream : Example
     {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var topic = "my/topic/path/with/update/stream";

            var topicSpecification = session.TopicControl.NewSpecification(TopicType.JSON);

            var updateStream = session.TopicUpdate.NewUpdateStreamBuilder()
                .Specification(topicSpecification)
                .Build<IJSON>(topic);

            var result = await updateStream.SetAsync(Diffusion.DataTypes.JSON.FromJSONString("{\"diffusion\":\"data\"}"), cancellationToken);

            if (result == TopicCreationResult.CREATED)
            {
                WriteLine("Topic has been created.");
            }
            else
            {
                WriteLine("Topic already exists.");
            }

            session.Close();
        }
    }
}
