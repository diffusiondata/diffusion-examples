/*******************************************************************************
 * Copyright (C) 2023 - 2024 Diffusion Data Ltd.
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
 *******************************************************************************/
using System.Threading;
using System.Threading.Tasks;
using System.Linq;
using System.Collections.Generic;
using static PushTechnology.ClientInterface.Examples.Program;
using static System.Console;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;

namespace PushTechnology.ClientInterface.Examples.PubSub.FetchTopics
{
    public sealed class FetchTopicProperties : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var topics = session.Topics;

            var topicProperties = new Dictionary<string, string> {
                        { TopicSpecificationProperty.DontRetainValue, "true" },
                        { TopicSpecificationProperty.Persistent, "false" },
                        { TopicSpecificationProperty.PublishValuesOnly, "true" }
                    };

            var topicSpecification = session.TopicControl.NewSpecification(TopicType.JSON)
                .WithProperties(topicProperties);

            for (int i = 1; i <= 5; i++)
            {
                string jsonValue = string.Format("\"diffusion\":\"data #{0}\"", i);
                await session.TopicUpdate.AddAndSetAsync("my/topic/path/with/properties/" + i, topicSpecification, Diffusion.DataTypes.JSON.FromJSONString("{" + jsonValue + "}"), cancellationToken);
            }

            topicSpecification = session.TopicControl.NewSpecification(TopicType.STRING)
                .WithProperties(topicProperties);

            for (int i = 1; i <= 5; i++)
            {
                string stringValue = string.Format("diffusion data #{0}", i);
                await session.TopicUpdate.AddAndSetAsync("my/topic/path/with/default/properties/" + i, topicSpecification, stringValue, cancellationToken);
            }

            var topicTypes = new[] { TopicType.JSON };
            var fetchResult = await topics.FetchRequest.TopicTypes(topicTypes).WithProperties().FetchAsync("?my/topic/path//", cancellationToken);

            foreach (var topic in fetchResult.Results)
            {
                WriteLine($"{topic.Path} properties:");

                foreach (var property in topic.Specification.Properties)
                {
                    WriteLine($"{property.Key}: {property.Value}");
                }
            }

            topicTypes = new[] { TopicType.STRING };

            // Fetch the topic properties for the topic path selector my/topic/path/.
            fetchResult = await topics.FetchRequest.TopicTypes(topicTypes).WithProperties().FetchAsync("?my/topic/path//", cancellationToken);

            foreach (var topic in fetchResult.Results)
            {
                WriteLine($"{topic.Path} properties:");

                foreach (var property in topic.Specification.Properties)
                {
                    WriteLine($"{property.Key}: {property.Value}");
                }
            }

            session.Close();
        }
    }
}

