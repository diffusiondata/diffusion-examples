/**
 * Copyright © 2016 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Content.Metadata;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Types;
using PushTechnology.ClientInterface.Examples.Runner;

namespace PushTechnology.ClientInterface.Examples.Client {

    /// <summary>
    /// Implementation of a client which subscribes to a Record topic and consumes the data it receives.
    /// </summary>
    public sealed class ConsumingRecordTopics : IExample {

        /// <summary>
        /// Runs the Record topic Client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public void Run( CancellationToken cancellationToken, string[] args ) {
            var serverUrl = args[ 0 ];

            // Connect anonymously
            var session = Diffusion.Sessions.Open( serverUrl );

            // Get the Topics feature to subscribe to topics
            var topics = session.GetTopicsFeature();

            var topicPath = ">random/record";

            // Add a topic stream for 'foo/counter' and request subscription
            var recordStream = new RecordStream();
            topics.AddTopicStream( topicPath, recordStream );

            topics.Subscribe( topicPath, new TopicsCompletionCallbackDefault() );

            // Run until user requests ending of example
            try {
                Task.Delay( Timeout.Infinite, cancellationToken ).Wait();
            } catch ( AggregateException ) {
                //Task was canceled; close stream, unsubscribe and close session
                topics.RemoveStream( recordStream );

                // Note that closing the session, will automatically unsubscribe from all topics the client is
                // subscribed to.
                session.Close();
            }
        }
    }

    /// <summary>
    /// Basic implementation of the IValueStream<TValue> for record topics.
    /// </summary>
    internal sealed class RecordStream : ITopicStream {

        private readonly string[] data = new string[ 10 ];

        /// <summary>
        /// Notification of stream being closed normally.
        /// </summary>
        public void OnClose() {
            Console.WriteLine( "The subscription stream is now closed." );
        }

        /// <summary>
        /// Notification of a contextual error related to this callback.
        /// </summary>
        /// <remarks>
        /// Situations in which <code>OnError</code> is called include the session being closed, a communication
        /// timeout, or a problem with the provided parameters. No further calls will be made to this callback.
        /// </remarks>
        /// <param name="errorReason">Error reason.</param>
        public void OnError( ErrorReason errorReason ) {
            Console.WriteLine( "An error has occured  : {0}", errorReason );
        }

        /// <summary>
        /// Notification of a succesfull subscription.
        /// </summary>
        /// <param name="topicPath">Topic path.</param>
        /// <param name="details">Topic details.</param>
        public void OnSubscription( string topicPath, ITopicDetails details ) {
            Console.WriteLine( "Client subscribed to {0} ", topicPath );
        }

        /// <summary>
        /// Topic update received.
        /// </summary>
        /// <param name="topicPath">Topic path.</param>
        /// <param name="content">The new changes for the data.</param>
        /// <param name="context">Topic context.</param>
        public void OnTopicUpdate( string topicPath, IContent content, IUpdateContext context ) {
            if ( context.UpdateType == TopicUpdateType.SNAPSHOT ) {
                var record = Diffusion.Content.NewReader<IRecordContentReader>( content ).NextRecord();
                record.Fields.CopyTo( data, 0 );
            } else {
                var record = Diffusion.Content.NewReader<IRecordContentReader>( content ).NextRecord();
                // Go through fields and if there is a change update the array which will be output to user.
                for ( int i = 0; i < data.Length ; i++ ) {
                    if ( record.Get( i ) != "" ) {
                        data[ i ] = record.Get( i );
                    }
                }
            }

            Console.WriteLine( "New value of {0} is {1}", topicPath, string.Join( ", ", data ) );
        }

        /// <summary>
        /// Notification of a succesfull unsubscription.
        /// </summary>
        /// <param name="topicPath">Topic path.</param>
        /// <param name="reason">Unsubscription reason.</param>
        public void OnUnsubscription( string topicPath, TopicUnsubscribeReason reason ) {
            Console.WriteLine( "Client unsubscribed from {0} : {1}", topicPath, reason );
        }
    }
}