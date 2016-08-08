/**
 * Copyright © 2014, 2016 Push Technology Ltd.
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
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Types;

namespace GettingStarted {
    /// <summary>
    /// A client that subscribes to the topic 'foo/counter'.
    /// </summary>
    internal class SubscribingClient {
        private static void Main2( string[] args ) {
            // Connect anonymously
            var session = Diffusion.Sessions.Open( "ws://host:80" );

            // Get the Topics feature to subscribe to topics
            var topics = session.GetTopicsFeature();

            // Add a new topic stream for 'foo/counter'
            topics.AddTopicStream( ">foo/counter", new TopicStreamPrintLine() );

            // Wait for a minute while the stream prints updates
            Thread.Sleep( 60000 );
        }

        private class TopicStreamPrintLine : ITopicStream {
            /// <summary>
            /// Notification of a contextual error related to this callback. This is analogous to an exception being
            /// raised. Situations in which <code>OnError</code> is called include the session being closed, a
            /// communication timeout, or a problem with the provided parameters. No further calls will be made to this
            /// callback.
            /// </summary>
            /// <param name="errorReason">errorReason a value representing the error; this can be one of
            /// constants defined in <see cref="T:PushTechnology.ClientInterface.Client.Callbacks.ErrorReason"/>, or a
            /// feature-specific reason.</param>
            public void OnError( ErrorReason errorReason ) {
            }

            /// <summary>
            /// Called to notify that a stream context was closed normally.
            /// No further calls will be made for the stream context.
            /// </summary>
            public void OnClose() {
            }

            /// <summary>
            /// This notifies when a topic is subscribed to.
            /// This provides only <see cref="F:PushTechnology.ClientInterface.Client.Topics.TopicDetailsLevel.BASIC"/>
            /// details of the topic.
            /// </summary>
            /// <param name="topicPath">the full topic path</param><param name="details">the basic details</param>
            public void OnSubscription( string topicPath, ITopicDetails details ) {
            }

            /// <summary>
            /// This notifies when a topic is unsubscribed.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param><param name="reason">the reason for
            /// unsubscription.</param>
            public void OnUnsubscription( string topicPath, TopicUnsubscribeReason reason ) {
            }

            /// <summary>
            /// Topic update received. This indicates an update to the state of a topic that is subscribed to.
            /// </summary>
            /// <param name="topicPath">the full topic path.</param><param name="content">the topic content. The context
            /// may contain more information about the nature of the content </param><param name="context"> the update
            /// context which may indicate whether the content represents the total state or a change to the state</param>
            public void OnTopicUpdate( string topicPath, IContent content, IUpdateContext context ) {
                Console.WriteLine( "{0}: {1}", topicPath, content.AsString() );
            }
        }
    }
}