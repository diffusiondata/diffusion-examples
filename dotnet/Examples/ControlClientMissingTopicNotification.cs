/**
 * Copyright © 2014, 2015 Push Technology Ltd.
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

using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;

namespace Examples
{
    public class ControlClientMissingTopicNotification
    {
        #region Fields

        private readonly ISession clientSession;
        private readonly ITopicControl topicControl;

        #endregion Fields

        #region Constructor

        public ControlClientMissingTopicNotification()
        {
            clientSession = Diffusion.Sessions.Principal( "client" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            topicControl = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" ).GetTopicControlFeature();

            Subscribe( "some/path10" );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Subscribes to a topic which may or may not be missing.
        /// </summary>
        /// <param name="topicPath">The path of the topic to subscribe to.</param>
        public async void Subscribe( string topicPath )
        {
            var missingTopicHandler = new MissingTopicHandler();

            // Add the 'missing topic handler' to the topic control object
            topicControl.AddMissingTopicHandler( topicPath, missingTopicHandler );

            // Wait for the successful registration of the handler
            var registeredHandler = await missingTopicHandler.OnActiveCalled;

            var topics = clientSession.GetTopicsFeature();

            var topicCompletion = new TaskCompletionSource<bool>();

            // Attempt to subscribe to the topic
            topics.Subscribe( topicPath, new TopicsCompletionCallback( topicCompletion ) );

            await topicCompletion.Task;

            // Wait and see if a missing topic notification is generated
            var request = await missingTopicHandler.OnMissingTopicCalled;

            // Cancel the client request on the server
            request.Cancel();

            // Close the registered handler
            registeredHandler.Close();

            // All events in Diffusion are asynchronous, so we must wait for the close to happen
            await missingTopicHandler.OnCloseCalled;
        }

        #endregion Public Methods

        #region Private Classes

        private class TopicsCompletionCallback : ITopicsCompletionCallback
        {
            #region Fields

            private readonly TaskCompletionSource<bool> theCompletionSource;

            #endregion Fields

            #region Constructor

            public TopicsCompletionCallback( TaskCompletionSource<bool> source )
            {
                theCompletionSource = source;
            }

            #endregion Constructor

            /// <summary>
            /// This is called to notify that a call context was closed prematurely, typically due to a timeout or the 
            /// session being closed.  No further calls will be made for the context.
            /// </summary>
            public void OnDiscard()
            {
                theCompletionSource.SetResult( false );
            }

            /// <summary>
            /// Called to indicate that the requested operation has been processed by the server.
            /// </summary>
            public void OnComplete()
            {
                theCompletionSource.SetResult( true );
            }
        }

        /// <summary>
        /// Asynchronous helper class for handling missing topic notifications.
        /// </summary>
        private class MissingTopicHandler : IMissingTopicHandler
        {
            private readonly TaskCompletionSource<IRegisteredHandler> onActive = 
                new TaskCompletionSource<IRegisteredHandler>();

            private readonly TaskCompletionSource<IMissingTopicNotification> onMissingTopic = 
                new TaskCompletionSource<IMissingTopicNotification>();

            private readonly TaskCompletionSource<bool> onClose = new TaskCompletionSource<bool>();

            /// <summary>
            /// Waits for the 'OnActive' event to be called.
            /// </summary>
            public Task<IRegisteredHandler> OnActiveCalled { get { return onActive.Task; } }

            /// <summary>
            /// Waits for the 'OnMissingTopic' event to be called.
            /// </summary>
            public Task<IMissingTopicNotification> OnMissingTopicCalled { get { return onMissingTopic.Task; } }

            public Task OnCloseCalled { get { return onClose.Task; } }

            /// <summary>
            /// Called when a client session requests a topic that does not exist, and the topic path belongs to part of
            /// the topic tree for which this handler was registered.
            /// 
            /// The handler implementation should take the appropriate action (for example, create the topic), and then call
            /// IMissingTopicNotification.Proceed on the supplied notification.  This allows the client request to continue
            /// and successfully resolve against the topic if it was created.
            /// 
            /// A handler should always call Proceed() otherwise resources will continue to be reserved on the server and the
            /// client's request will not complete.
            /// </summary>
            /// <param name="notification">The client notification object.</param>
            void IMissingTopicHandler.OnMissingTopic( IMissingTopicNotification notification )
            {
                onMissingTopic.SetResult( notification );
            }

            /// <summary>
            /// Called when the handler has been successfully registered with the server.
            /// 
            /// A session can register a single handler of each type for a given branch of the topic tree.  If there is
            /// already a handler registered for the topic path the operation will fail, <c>registeredHandler</c> will be closed,
            /// and the session error handler will be notified.  To change the handler, first close the previous handler.
            /// </summary>
            /// <param name="topicPath">The path that the handler is active for.</param>
            /// <param name="registeredHandler">Allows the handler to be closed.</param>
            void ITopicTreeHandler.OnActive( string topicPath, IRegisteredHandler registeredHandler )
            {
                onActive.SetResult( registeredHandler );
            }

            /// <summary>
            /// Called if the handler is closed.  This happens if the call to register the handler fails, or the handler
            /// is unregistered.
            /// </summary>
            /// <param name="topicPath">The branch of the topic tree for which the handler was registered.</param>
            void ITopicTreeHandler.OnClose( string topicPath )
            {
                onClose.TrySetResult( false );
            }
        }

        #endregion Private Classes
    }
}