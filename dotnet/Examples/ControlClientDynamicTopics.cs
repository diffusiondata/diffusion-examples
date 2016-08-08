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

using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;

namespace Examples {
    /// <summary>
    /// An example of using control client to create topics dynamically, i.e. when topics that do not exist are
    /// requested.
    /// </summary>
    public class ControlClientDynamicTopics {
        private readonly ISession session;

        /// <summary>
        /// Constructor.
        /// </summary>
        public ControlClientDynamicTopics() {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            var tc = session.GetTopicControlFeature();
            var proceedCallback = new AddTopicAndProceed();

            // Add a handler that, upon receiving subscriptions or fetches for any topic under 'topicroot', creates a
            // topic. If the topic name starts with 'SV', it creates a single value topic, otherwise a delegated topic.
            tc.AddMissingTopicHandler( "topicroot", new MissingTopicHandler( tc, proceedCallback ) );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close() {
            session.Close();
        }

        private class MissingTopicHandler : MissingTopicHandlerDefault {
            private readonly ITopicControl theTopicControl;
            private readonly AddTopicAndProceed theProceedCallback;

            public MissingTopicHandler( ITopicControl topicControl, AddTopicAndProceed proceedCallback ) {
                theTopicControl = topicControl;
                theProceedCallback = proceedCallback;
            }

            /// <summary>
            /// Called when a client session requests a topic that does not exist, and the topic path belongs to part of
            /// the topic tree for which this handler was registered.
            ///
            /// The handler implementation should take the appropriate action (for example, create the topic), and then
            /// call IMissingTopicNotification.Proceed on the supplied notification. This allows the client request to
            /// continue and successfully resolve against the topic if it was created.
            ///
            /// A handler should always call Proceed() otherwise resources will continue to be reserved on the server
            /// and the client's request will not complete.
            /// </summary>
            /// <param name="notification"></param>
            public override void OnMissingTopic( IMissingTopicNotification notification ) {
                var topicPath = notification.TopicPath;

                //TODO: TopicType.DELEGATED is deprecated.
                theTopicControl.AddTopic( topicPath,
                    topicPath.StartsWith( "topicroot/SV" ) ? TopicType.SINGLE_VALUE : TopicType.DELEGATED,
                    notification, theProceedCallback );
            }
        }

        private class AddTopicAndProceed : ITopicControlAddContextCallback<IMissingTopicNotification> {
            /// <summary>
            /// Notification that a context was closed prematurely, typically due to a timeout or the session being
            /// closed. No further calls will be made for a context.
            /// </summary>
            /// <param name="context"></param>
            public void OnDiscard( IMissingTopicNotification context ) {
                context.Proceed();
            }

            /// <summary>
            /// Topic has been added.
            /// </summary>
            /// <param name="context">The context object the application supplied when making the call; may be null.</param>
            /// <param name="topicPath">The full path of the topic that was added.</param>
            public void OnTopicAdded( IMissingTopicNotification context, string topicPath ) {
                context.Proceed();
            }

            /// <summary>
            /// An attempt to add a topic has failed.
            /// </summary>
            /// <param name="context">The context object the application supplied when making the call; may be null.</param>
            /// <param name="topicPath">The topic path as supplied to the add request.</param>
            /// <param name="reason">The reason for failure.</param>
            public void OnTopicAddFailed( IMissingTopicNotification context, string topicPath, TopicAddFailReason reason ) {
                context.Proceed();
            }
        }
    }
}