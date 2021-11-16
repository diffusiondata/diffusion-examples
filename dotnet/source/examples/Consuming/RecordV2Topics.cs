/**
 * Copyright © 2018, 2021 Push Technology Ltd.
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

using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using PushTechnology.ClientInterface.Data.Record;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Consuming {
    /// <summary>
    /// Implementation of a client which subscribes to a RecordV2 topic and consumes the data it receives.
    /// </summary>
    public sealed class ConsumingRecordV2Topics : IExample {
        /// <summary>
        /// Runs the RecordV2 topic consuming client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server URL.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string serverUrl = args[0];

            // Connect anonymously
            var session = Diffusion.Sessions.Open( serverUrl );

            // Get the Topics feature to subscribe to topics
            var topics = session.Topics;
            string topic = "random/RecordV2";

            // Add a topic stream for 'random/RecordV2'
            var recordV2Stream = new RecordV2Stream();
            topics.AddStream( topic, recordV2Stream );

            try {
                // Subscribe to 'random/RecordV2' topic
                await topics.SubscribeAsync( topic, cancellationToken );

                // Run until user requests ending of example
                await Task.Delay( Timeout.Infinite, cancellationToken );
            } catch ( TaskCanceledException ) {
                //Task was canceled; close stream and unsubscribe
                topics.RemoveStream( recordV2Stream );
                await topics.UnsubscribeAsync( topic );
            } finally {
                // Note that closing the session, will automatically unsubscribe from all topics the client is
                // subscribed to.
                session.Close();
            }
        }

        /// <summary>
        /// Basic implementation of the IValueStream for RecordV2 topics.
        /// </summary>
        private sealed class RecordV2Stream : IValueStream<IRecordV2> {
            /// <summary>
            /// Notification of stream being closed normally.
            /// </summary>
            public void OnClose()
                => WriteLine( "The subscription stream is now closed." );

            /// <summary>
            /// Notification of a contextual error related to this callback.
            /// </summary>
            /// <remarks>
            /// Situations in which <code>OnError</code> is called include the session being closed, a communication
            /// timeout, or a problem with the provided parameters. No further calls will be made to this callback.
            /// </remarks>
            /// <param name="errorReason">Error reason.</param>
            public void OnError( ErrorReason errorReason )
                => WriteLine( $"An error has occurred : {errorReason}." );

            /// <summary>
            /// Notification of a successful subscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            public void OnSubscription( string topicPath, ITopicSpecification specification )
                => WriteLine( $"Client subscribed to topic '{topicPath}'." );

            /// <summary>
            /// Notification of a successful unsubscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="reason">Error reason.</param>
            public void OnUnsubscription( string topicPath, ITopicSpecification specification, TopicUnsubscribeReason reason )
                => WriteLine( $"Client unsubscribed from topic '{topicPath}' with reason '{reason}'." );

            /// <summary>
            /// Topic update received.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="oldValue">Value prior to update.</param>
            /// <param name="newValue">Value after update.</param>
            public void OnValue( string topicPath, ITopicSpecification specification, IRecordV2 oldValue, IRecordV2 newValue )
                => WriteLine( $"New value of topic '{topicPath}' is [{string.Join( ", ", newValue.AsFields() )}]." );
        }
    }
}
