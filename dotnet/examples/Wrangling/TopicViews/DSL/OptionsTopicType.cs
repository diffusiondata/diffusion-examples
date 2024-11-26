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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;
using System.Linq;

namespace PushTechnology.ClientInterface.Examples.Wrangling.TopicViews.DSL
{
    public sealed class OptionsTopicType : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            string topic = "my/topic/path";

            var topicSpecification = session.TopicControl.NewSpecification(TopicType.INT64);

            await session.TopicUpdate.AddAndSetAsync<long?>(topic, topicSpecification, 0, cancellationToken);

            var view1 = await session.TopicViews.CreateTopicViewAsync("topic_view_1", "map my/topic/path to views/archive/<path(0)> type TIME_SERIES", cancellationToken);
            WriteLine($"Topic View {view1.Name} has been created.");

            for (int i = 0; i < 15; i++)
            {
                await session.TopicUpdate.SetAsync<long?>(topic, DateTimeOffset.Now.ToUnixTimeMilliseconds(), cancellationToken);
                await Task.Delay(1000);
            }

            var rangeQuery = session.TimeSeries.RangeQuery.As<long?>();

            var queryResult = await rangeQuery
                .From(0)
                .SelectFromAsync("views/archive/my/topic/path", cancellationToken);

            var results = queryResult.Events.ToList();

            foreach (var result in results)
            {
                WriteLine($"{result.Metadata.Sequence} ({result.Metadata.Timestamp}): {result.Value}");
            }
            
            session.Close();
        }
    }
}
