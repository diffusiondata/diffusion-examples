
/*******************************************************************************
 * Copyright (C) 2019 - 2022 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Data.JSON;
using static PushTechnology.ClientInterface.Examples.Runner.Program;
using System;
using PushTechnology.ClientInterface.Data;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Session;
using System.Linq;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that fetches topic values and/or specifications.
    /// </summary>
    public sealed class TopicFetch : IExample
    {
        /// <summary>
        /// Runs the fetch example that retrieves values and/or topic specifications for a set of topics without subscribing.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var serverUrl = args[ 0 ];

            ISession session = Diffusion.Sessions
                    .Principal( "control" )
                    .Password( "password" )
                    .CertificateValidation((cert, chain, errors)
                        => CertificateValidationResult.ACCEPT)
                    .Open( serverUrl );

            ITopics topics = session.Topics;

            for (char a = 'a'; a <= 'z'; ++a)
            {
                try
                {
                    if (a % 2 == 0)
                    {
                        await session.TopicControl.AddTopicAsync("a/" + a, TopicType.JSON);
                    }
                    else if (a % 3 == 0)
                    {
                        await session.TopicControl.AddTopicAsync("a/" + a, TopicType.STRING);
                    }
                }
                catch(Exception ex)
                {
                    Console.WriteLine("Failed to add topic {0} : {1}.", "a/" + a, ex);
                }
            }

            //Fetch all topics - only topic path and type are returned in each result.
            var res = topics.FetchRequest.FetchAsync("*.*").Result;
            Console.WriteLine( "All results:" );
            foreach (var topic in res.Results) {
                Console.WriteLine(topic.Path);
            }

            // Fetch all string topics that satisfy a specified topic selector with values.
            res = topics.FetchRequest.WithValues<string>().FetchAsync("*.*").Result;
            Console.WriteLine( "All string results:" );
            foreach ( var topic in res.Results ) {
                Console.WriteLine( topic.Path );
            }

            // Fetch a single string topic with value and properties.
            var resSingle = topics.FetchRequest.WithValues<string>().WithProperties().FetchAsync("?a/c").Result.Results.ElementAt(0);
            Console.WriteLine($"Topic 'a/c' has path: {resSingle.Path}, type: {resSingle.Type}, value: '{resSingle.Value}', specification: '{resSingle.Specification}'");

            // Fetch all JSON topics that match a specified selector, with values.
            var resJSON = topics.FetchRequest.WithValues<IJSON>().TopicTypes(new[] { TopicType.JSON }).FetchAsync("*.*").Result;
            Console.WriteLine("All JSON results:");
            foreach (var topic in resJSON.Results)
            {
                Console.WriteLine(topic.Path);
            }

            // Obtain an inclusive range of topics, using topic paths for the start and end points.
            string from = "a/b";
            string to = "a/d";
            var resBytes = topics.FetchRequest.From(from).To(to).FetchAsync("*.*").Result;
            Console.WriteLine("All topics between 'a/b' and 'a/d' inclusive are:");
            foreach (var topic in resBytes.Results)
            {
                Console.WriteLine(topic.Path);
            }

            // Obtain the next group of topics, with values, from a specified start point 'after' limiting the number of topics to fetch with 'limit'.
            // This demonstrates paging and could be used repeatedly specifying the after value as the path of the last topic retrieved
            // from the previous call of the next method. The FetchResult.HasMore() method on the result can be used to
            // determine whether there may be more results.
            string after = "a/b";
            int limit = 2;
            var resNext = topics.FetchRequest.After(after).WithValues<IBytes>().First(limit).FetchAsync("*.*").Result;
            Console.WriteLine("The 2 next lexographic topics after 'a/b' are:");
            foreach (var topic in resNext.Results)
            {
                Console.WriteLine(topic.Path);
            }

            // Obtain the prior group of topics, with values, from a specified end point 'before' limiting the number of topics to fetch with 'limit'.
            // This demonstrates paging and could be used to retrieve the set of topics prior to the first topic from a previous call of prior or next.
            string before = "a/e";
            limit = 3;
            var resPrior = topics.FetchRequest.Before(before).WithValues<IBytes>().Last(limit).FetchAsync("*.*").Result;
            Console.WriteLine("The 3 prior lexographic topics before 'a/e' are:");
            foreach (var topic in resPrior.Results)
            {
                Console.WriteLine(topic.Path);
            }

            // Limit on the number of results returned for each deep branch with 'deepBranchDepth' the number of parts in the root path of a branch
            // for it to be considered deep, and 'deepBranchLimit' the maximum number of results to return for each deep branch.
            // This demonstrates a method that could be particularly useful for incrementally exploring a topic tree from the root,
            // allowing a breadth-first search strategy.
            int deepBranchDepth = 1;
            int deepBranchLimit = 1;
            var resLimit = topics.FetchRequest.LimitDeepBranches(deepBranchDepth, deepBranchLimit).WithValues<IBytes>().FetchAsync("*.*").Result;
            Console.WriteLine("The results that are at most 1 part deep, with a maximum of 1 result per deep branch are:");
            foreach (var topic in resLimit.Results)
            {
                Console.WriteLine(topic.Path);
            }
        }

    }

}

