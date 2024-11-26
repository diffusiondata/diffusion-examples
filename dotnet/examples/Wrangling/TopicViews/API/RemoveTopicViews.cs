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
using System.Threading.Tasks;
using System.Threading;
using System.Linq;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.Wrangling.TopicViews.API
{
    public sealed class RemoveTopicViews : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string topic = "my/topic/path";
            string topic2 = "my/topic/path/array";

            string json = "{\"diffusion\":\"data\"}";
            var topicSpecification = session.TopicControl.NewSpecification(TopicType.JSON);
            await session.TopicUpdate.AddAndSetAsync(topic, topicSpecification, Diffusion.DataTypes.JSON.FromJSONString(json), cancellationToken);
            await session.TopicUpdate.AddAndSetAsync(topic2, topicSpecification, Diffusion.DataTypes.JSON.FromJSONString(json), cancellationToken);

            var view1 = await session.TopicViews.CreateTopicViewAsync("topic_view_1", "map my/topic/path to views/<path(0)>", cancellationToken);
            WriteLine($"Topic View '{view1.Name}' has been created.");

            var view2 = await session.TopicViews.CreateTopicViewAsync("topic_view_2", "map my/topic/path/array to views/<path(0)>", cancellationToken);
            WriteLine($"Topic View '{view2.Name}' has been created.");

            var topicViews = await session.TopicViews.ListTopicViewsAsync(cancellationToken);
            
            foreach (var topicView in topicViews)
            {
                WriteLine($"Topic View {topicView.Name}: {topicView.Specification} ({string.Join(",", topicView.Roles.ToList())})");
            }

            await session.TopicViews.RemoveTopicViewAsync("topic_view_1", cancellationToken);

            topicViews = await session.TopicViews.ListTopicViewsAsync(cancellationToken);

            foreach (var topicView in topicViews)
            {
                WriteLine($"Topic View {topicView.Name}: {topicView.Specification} ({string.Join(",", topicView.Roles.ToList())})");
            }
            session.Close();
        }
    }
}
