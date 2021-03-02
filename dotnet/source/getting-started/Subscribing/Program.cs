/**
 * Copyright © 2016, 2017 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using PushTechnology.ClientInterface.Data.JSON;

namespace PushTechnology.ClientInterface.Example {
    class Program {
        static void Main( string[] args ) {
            // Connect anonymously
            var session = Diffusion.Sessions.Open( "ws://localhost:8080" );

            // Get the Topics feature to subscribe to topics
            var topics = session.Topics;
            var topic = ">foo/counter";

            // Add a topic stream for 'random/JSON' and request subscription
            var jsonStream = new JSONStream();
            topics.AddStream( topic, jsonStream );
            topics.SubscribeAsync( topic ).Wait();

            //Stay connected for 10 minutes
            Thread.Sleep( TimeSpan.FromMinutes( 10 ) );

            session.Close();
        }

        /// <summary>
        /// Basic implementation of the IValueStream<TValue> for JSON topics.
        /// </summary>
        internal sealed class JSONStream : IValueStream<IJSON> {
            /// <summary>
            /// Notification of stream being closed normally.
            /// </summary>
            public void OnClose() 
                => Console.WriteLine( "The subscription stream is now closed." );

            /// <summary>
            /// Notification of a contextual error related to this callback.
            /// </summary>
            /// <remarks>
            /// Situations in which <code>OnError</code> is called include the session being closed, a communication
            /// timeout, or a problem with the provided parameters. No further calls will be made to this callback.
            /// </remarks>
            /// <param name="errorReason">Error reason.</param>
            public void OnError( ErrorReason errorReason ) 
                => Console.WriteLine( $"An error has occured : {errorReason}" );

            /// <summary>
            /// Notification of a successful subscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            public void OnSubscription( string topicPath, ITopicSpecification specification ) 
                => Console.WriteLine( $"Client subscribed to {topicPath}" );

            /// <summary>
            /// Notification of a successful unsubscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="reason">Error reason.</param>
            public void OnUnsubscription( string topicPath, ITopicSpecification specification, TopicUnsubscribeReason reason ) 
                => Console.WriteLine( $"Client unsubscribed from {topicPath} : {reason}" );

            /// <summary>
            /// Topic update received.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="oldValue">Value prior to update.</param>
            /// <param name="newValue">Value after update.</param>
            public void OnValue( string topicPath, ITopicSpecification specification, IJSON oldValue, IJSON newValue ) 
                => Console.WriteLine( $"New value of {topicPath} is {newValue.ToJSONString()}" );
        }
    }
}
