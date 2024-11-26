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
using static System.Console;
using System.Linq;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using static PushTechnology.ClientInterface.Examples.Program;
using PushTechnology.ClientInterface.Client.Topics;

namespace PushTechnology.ClientInterface.Examples.PubSub.FetchTopics
{
    public sealed class FetchMultipleTopicsByIteratingThroughPaging : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var topics = session.Topics;

            var topicSpecification = session.TopicControl.NewSpecification(TopicType.STRING);

            for (int i = 1; i <= 25; i++)
            {
                string stringValue = string.Format("diffusion data #{0}", i);
                await session.TopicUpdate.AddAndSetAsync("my/topic/path/" + i, topicSpecification, stringValue, cancellationToken);
            }

            IFetchResult<string> fetchResult = null;

            string topicSelector = "?my/topic/path//";

            fetchResult = await topics.FetchRequest.WithValues<string>().First(10).FetchAsync(topicSelector, cancellationToken);

            while (true)
            {
                foreach (var topic in fetchResult.Results)
                {
                    WriteLine($"{topic.Path}: {topic.Value}");
                }

                if (fetchResult.HasMore)
                {
                    // Fetch the next 10 values.
                    string path = fetchResult.Results.ElementAt(fetchResult.Results.Count - 1).Path;
                    fetchResult = await topics.FetchRequest.After(path).WithValues<string>().First(10).FetchAsync(topicSelector, cancellationToken);
                    WriteLine("Loading next page.");
                }
                else
                {
                    WriteLine("Done.");
                    break;
                }
            }
            
            session.Close();
        }
    }
}

