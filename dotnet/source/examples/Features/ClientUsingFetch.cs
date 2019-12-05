
/*******************************************************************************
 * Copyright (C) 2019 Push Technology Ltd.
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

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Implementation of a client which utlises the Fetch API.
    /// </summary>


    public class IFetch : IExample {
        ITopics topics;
        ISession session;


        /// <summary>
        /// Runs the authenticator client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {

            var serverUrl = args[ 0 ];

            session = Diffusion.Sessions
                    .Principal( "client" )
                    .Password( "password" )
                    .Open( serverUrl );

            topics = session.Topics;

            var topicCtl = session.TopicControl;

            for(char a = 'a'; a <= 'z'; ++a ) {

                if (a % 2 == 0) {
                    await topicCtl.AddTopicAsync( "?a//" + a, TopicType.JSON );
                } else if (a % 3 == 0) {
                    await topicCtl.AddTopicAsync( "?a//" + a, TopicType.STRING );
                }
            }


            var res = fetchAll();
            Console.WriteLine( "All results" );
            foreach (var topic in res.Results) {
                Console.WriteLine(topic.Path);
            }


            res = fetchAllStringTopics("*.*");
            Console.WriteLine( "All string results" );
            foreach ( var topic in res.Results ) {
                Console.WriteLine( topic.Path );
            }


            var resStringTopic = fetchStringTopic( "a/b" );
            Console.WriteLine( "\n" + resStringTopic.Path );
            Console.WriteLine( resStringTopic.Type );
            Console.WriteLine( resStringTopic.Value );
            Console.WriteLine( resStringTopic.Specification + "\n");


            Console.WriteLine( getStringTopicValue( "a/b" ) + "\n" );


            var resJSON = fetchJSONTopics( "*.*" );
            Console.WriteLine( "\nAll JSON results" );
            foreach ( var topic in resJSON.Results ) {
                Console.WriteLine( topic.Path );
            }


            var resBytes = fetchRange( "?a//a", "?a//c" );
            Console.WriteLine("\nAll topics between a/a and a/c inclusive" );
            foreach ( var topic in resBytes.Results ) {
                Console.WriteLine( topic.Path );
            }


            var resNext = next( "?a//a", 2 );
            Console.WriteLine( "\nThe 2 next lexographic topics after a/a" );
            foreach ( var topic in resNext.Results ) {
                Console.WriteLine( topic.Path );
            }


            var resPrior = prior( "?a//e", 3 );
            Console.WriteLine( "\nThe 3 prior lexographic topics before a/e" );
            foreach ( var topic in resPrior.Results ) {
                Console.WriteLine( topic.Path );
            }
        }


    /// <summary>
        /// This shows an example of retrieving all topics - only topic path and type
        /// are returned in each result.
        /// </summary>
    public IFetchResult fetchAll() => topics.FetchRequest.FetchAsync("*.*").Result;


    /// <summary>
    /// This shows an example of retrieving all string topics that satisfy a
    /// specified topic selector with values.
    /// </summary>
    /// <param name="topicSelector"> The topic selector to be used </param>
     public IFetchResult<string> fetchAllStringTopics(string topicSelector)  => topics.FetchRequest.WithValues<string>().FetchAsync(topicSelector).Result;


    /// <summary>
    ///This shows an example of retrieving a single string topic with value and
    ///properties.
    /// </summary>
    /// <param name="topicPath"> The topic path to be used </param>
    public ITopicResult<string> fetchStringTopic(string topicPath) => topics.FetchRequest.WithValues<string>().WithProperties().FetchAsync(topicPath).Result.Results.GetEnumerator().Current;


    ///<summary>
    ///This shows how to obtain the value of a specified string topic.
    ///<p>
    ///</summary>
    /// <param name="topicPath"> The topic path to be used </param>
    public string getStringTopicValue(string topicPath) => fetchStringTopic(topicPath)?.Value;

    ///<summary>
    ///This shows an example of retrieving all JSON topics that match a
    ///specified selector with values.
    ///
    ///This would return results only for JSON topics and not type compatible
    ///subtypes.
    ///</summary>
    /// <param name="topicSelector"> The topic selector to be used </param>
    public IFetchResult<IJSONDataType> fetchJSONTopics(string topicSelector) => topics.FetchRequest.WithValues<IJSONDataType>().TopicTypes(new [] {TopicType.JSON}).FetchAsync(topicSelector).Result;


    ///<summary>
    ///Shows how to obtain an inclusive range of topics, with values.
    ///</summary>
    /// <param name="from"> The topic selector to be used as a starting point </param>
    /// <param name="to"> The topic selector to be used as an ending point </param>
    public IFetchResult<IBytes> fetchRange( string from, string to ) => topics.FetchRequest.From(from).To(to).WithValues<IBytes>().FetchAsync("*.*").Result;


    ///<summary>
    ///Shows how to obtain the next group of topics, with values, from a
    ///specified start point.
    ///
    ///This demonstrates paging and could be used repeatedly specifying the
    ///after value as the path of the last topic retrieved from the previous
    ///call of the next method. The {@link FetchResult#hasMore() hasMore} method
    ///on the result can be used to determine whether there may be more results.
    ///
    ///Bytes is used as the value type so that all topic types are selected.
    ///</summary>
    /// <param name="after"> The topic selector to be used as a starting point </param>
    /// <param name="limit"> The number of topics to fetch after the starting point </param>
    public IFetchResult<IBytes> next( string after, int limit) => topics.FetchRequest.After(after).WithValues<IBytes>().First(limit).FetchAsync("*.*").Result;


    ///<summary>
    ///Shows how to obtain the prior group of topics, with values, from a
    ///specified end point.
    ///This demonstrates paging and could be used to retrieve the set of topics
    ///prior to the first topic from a previous call of prior or next.
    ///</summary>
    /// <param name="before"> The topic selector to be used as a starting point </param>
    /// <param name="limit"> The number of topics to fetch before the starting point </param>
    public IFetchResult<IBytes> prior( string before, int limit) => topics.FetchRequest.Before(before).WithValues<IBytes>().Last(limit).FetchAsync("*.*").Result;

    }

}

