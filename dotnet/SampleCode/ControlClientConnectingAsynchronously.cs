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

using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;

namespace Examples {
    /// <summary>
    /// This is a simple example of a client that uses asynchronous connection to connect, create a topic and then
    /// disconnect.
    /// </summary>
    public class ControlClientConnectingAsynchronously {
        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="topicPath">The path of the topic to create.</param>
        public ControlClientConnectingAsynchronously( string topicPath ) {
            Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80", new TopicAdder( topicPath ) );
        }

        private class TopicAdder : ISessionOpenCallback {
            private readonly string topicToAdd;

            /// <summary>
            /// Cosntructor.
            /// </summary>
            /// <param name="topicPath">The path of the topic to add.</param>
            public TopicAdder( string topicPath ) {
                topicToAdd = topicPath;
            }

            /// <summary>
            /// Notification of a contextual error related to this callback. This is analogous to an exception being
            /// raised. Situations in which <code>OnError</code> is called include the session being closed, a
            /// communication timeout, or a problem with the provided parameters. No further calls will be made to this
            /// callback.
            /// </summary>
            /// <param name="errorReason">errorReason a value representing the error; this can be one of constants
            /// defined in <see cref="ErrorReason" />, or a feature-specific reason.</param>
            public void OnError( ErrorReason errorReason ) {
            }

            /// <summary>
            /// Called when a session has been successfully opened.
            /// </summary>
            /// <param name="session"></param>
            public void OnOpened( ISession session ) {
                session.TopicControl.AddTopic(
                    topicToAdd, TopicType.SINGLE_VALUE, new TopicAddCallback( session ) );
            }
        }

        private class TopicAddCallback : ITopicControlAddCallback {
            private readonly ISession theSession;

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="session">The client session.</param>
            public TopicAddCallback( ISession session ) {
                theSession = session;
            }

            /// <summary>
            /// This is called to notify that a call context was closed prematurely, typically due to a timeout or the
            /// session being closed. No further calls will be made for the context.
            /// </summary>
            public void OnDiscard() {
                theSession.Close();
            }

            /// <summary>
            /// Topic has been added.
            /// </summary>
            /// <param name="topicPath">The full path of the topic that was added.</param>
            public void OnTopicAdded( string topicPath ) {
                theSession.Close();
            }

            /// <summary>
            /// An attempt to add a topic has failed.
            /// </summary>
            /// <param name="topicPath">The topic path as supplied to the add request.</param>
            /// <param name="reason">The reason for failure.</param>
            public void OnTopicAddFailed( string topicPath, TopicAddFailReason reason ) {
                theSession.Close();
            }
        }
    }
}