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

using System.Threading;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;

namespace Examples {
    /// <summary>
    /// An example of using a control client as an event feed to a topic.
    ///
    /// This uses the <see cref="ITopicControl"/> feature to create a topic and the <see cref="ITopicUpdateControl"/>
    /// feature to send updates to it.
    ///
    /// To send updates to a topic, the client session requires the <see cref="TopicPermission.UPDATE_TOPIC"/>
    /// permission for that branch of the topic tree.
    /// </summary>
    public class ControlClientAsUpdateSource {
        private const string TopicName = "Feeder";
        private readonly ISession session;
        private readonly ITopicControl topicControl;
        private readonly ITopicUpdateControl updateControl;
        private readonly ITopicUpdaterUpdateCallback updateCallback;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="callback">The callback for updates.</param>
        public ControlClientAsUpdateSource( ITopicUpdaterUpdateCallback callback ) {
            updateCallback = callback;

            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com;80" );

            topicControl = session.GetTopicControlFeature();
            updateControl = session.GetTopicUpdateControlFeature();
        }

        public void Start( IPriceProvider provider ) {
            // Set up topic details
            var builder = topicControl.CreateDetailsBuilder<ISingleValueTopicDetailsBuilder>();
            var details = builder.Metadata( Diffusion.Metadata.Decimal( "Price" ) ).Build();

            // Declare a custom update source implementation. When the source is set as active, start a periodic task
            // to poll the provider every second and update the topic. When the source is closed, stop the scheduled
            // task.
            var source = new UpdateSource( provider, updateCallback );

            // Create the topic. When the callback indicates that the topic has been created, register the topic
            // source for the topic.
            topicControl.AddTopicFromValue( TopicName, details, new AddCallback( updateControl, source ) );
        }

        public void Close() {
            // Remove our topic and close the session when done.
            topicControl.RemoveTopics( ">" + TopicName, new RemoveCallback( session ) );
        }

        private class RemoveCallback : TopicControlRemoveCallbackDefault {
            private readonly ISession theSession;

            public RemoveCallback( ISession session ) {
                theSession = session;
            }

            /// <summary>
            /// Notification that a call context was closed prematurely, typically due to a timeout or the session being
            /// closed. No further calls will be made for the context.
            /// </summary>
            public override void OnDiscard() {
                theSession.Close();
            }

            /// <summary>
            /// Topic(s) have been removed.
            /// </summary>
            public override void OnTopicsRemoved() {
                theSession.Close();
            }
        }

        private class AddCallback : TopicControlAddCallbackDefault {
            private readonly ITopicUpdateControl updateControl;
            private readonly UpdateSource updateSource;

            public AddCallback( ITopicUpdateControl updater, UpdateSource source ) {
                updateControl = updater;
                updateSource = source;
            }

            /// <summary>
            /// Topic has been added.
            /// </summary>
            /// <param name="topicPath">The full path of the topic that was added.</param>
            public override void OnTopicAdded( string topicPath ) {
                updateControl.RegisterUpdateSource( topicPath, updateSource );
            }
        }

        private class UpdateSource : TopicUpdateSourceDefault {
            private readonly IPriceProvider thePriceProvider;
            private readonly ITopicUpdaterUpdateCallback theUpdateCallback;
            private readonly CancellationTokenSource cancellationToken = new CancellationTokenSource();

            public UpdateSource( IPriceProvider provider, ITopicUpdaterUpdateCallback callback ) {
                thePriceProvider = provider;
                theUpdateCallback = callback;
            }

            /// <summary>
            /// State notification that this source is now active for the specified topic path, and is therefore in a
            /// valid state to send updates on topics at or below the registered topic path.
            /// </summary>
            /// <param name="topicPath">The registration path.</param>
            /// <param name="updater">An updater that may be used to update topics at or below the registered path.</param>
            public override void OnActive( string topicPath, ITopicUpdater updater ) {
                PeriodicTaskFactory.Start( () => {
                    updater.Update(
                        TopicName, Diffusion.Content.NewContent( thePriceProvider.Price ), theUpdateCallback );
                }, 1000, cancelToken: cancellationToken.Token );
            }

            /// <summary>
            /// Called if the handler is closed. The handler will be closed if the
            /// session is closed after the handler has been registered, or if the
            /// handler is unregistered using <see cref="IRegistration.Close">close</see>.
            ///
            /// No further calls will be made for the handler.
            /// </summary>
            /// <param name="topicPath">the branch of the topic tree for which the handler was registered</param>
            public override void OnClose( string topicPath ) {
                cancellationToken.Cancel();
            }
        }

        public interface IPriceProvider {
            /// <summary>
            /// Get the current price as a decimal string.
            /// </summary>
            string Price {
                get;
            }
        }
    }
}