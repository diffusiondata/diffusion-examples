/**
 * Copyright © 2015, 2016 Push Technology Ltd.
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

using System.Linq;
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Types;

namespace Examples {
    /// <summary>
    /// This is an example of a control client using the 'MessagingControl' feature to send messages to clients using
    /// message filters. It also demonstrates the ability to register a message handler with an interest in session
    /// property values.
    /// </summary>
    public class ControlClientUsingFiltersAndProperties {
        private readonly ISession theSession;
        private readonly IMessagingControl theMessagingControl;
        private readonly ISendToFilterCallback theSendToFilterCallback;

        public ControlClientUsingFiltersAndProperties( ISendToFilterCallback callback ) {
            theSendToFilterCallback = callback;

            theSession = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            theMessagingControl = theSession.MessagingControl;

            // Register and receive all messages sent by clients on the "foo" branch and include the "JobTitle" session
            // property value with each message. To do this, the client session must have the "register_handler"
            // permission.
            theMessagingControl.AddMessageHandler(
                "foo",
                new BroadcastHandler( theMessagingControl, theSendToFilterCallback ),
                "JobTitle" );
        }

        public void Close() {
            theSession.Close();
        }

        private class BroadcastHandler : IMessageHandler {
            private readonly IMessagingControl theMessagingControl;
            private readonly ISendToFilterCallback theSendToFilterCallback;

            /// <summary>
            /// Constructor.
            /// </summary>
            /// <param name="messagingControl">The messaging control object.</param>
            /// <param name="callback">The filter callback.</param>
            public BroadcastHandler( IMessagingControl messagingControl, ISendToFilterCallback callback ) {
                theMessagingControl = messagingControl;
                theSendToFilterCallback = callback;
            }

            /// <summary>
            /// Called when the handler has been successfully registered with the server.
            ///
            /// A session can register a single handler of each type for a given branch of the topic tree. If there is
            /// already a handler registered for the topic path the operation will fail, <c>registeredHandler</c> will
            /// be closed, and the session error handler will be notified. To change the handler, first close the
            /// previous handler.
            /// </summary>
            /// <param name="topicPath">The path that the handler is active for.</param>
            /// <param name="registeredHandler">Allows the handler to be closed.</param>
            public void OnActive( string topicPath, IRegisteredHandler registeredHandler ) {
            }

            /// <summary>
            /// Called if the handler is closed. This happens if the call to register the handler fails, or the handler
            /// is unregistered.
            /// </summary>
            /// <param name="topicPath">The branch of the topic tree for which the handler was registered.</param>
            public void OnClose( string topicPath ) {
            }

            /// <summary>
            /// Receives content sent from a session via a topic.
            /// </summary>
            /// <param name="sessionId">Identifies the client session that sent the content.</param>
            /// <param name="topicPath">The path of the topic that the content was sent on.</param>
            /// <param name="content">The content sent by the client.</param>
            /// <param name="context">The context associated with the content.</param>
            public void OnMessage( SessionId sessionId, string topicPath, IContent content, IReceiveContext context ) {
                if ( !"Manager".Equals( context.SessionProperties[ "JobTitle" ] ) )
                    return;

                theMessagingControl.SendToFilter( "JobTitle is 'Staff'", topicPath, content,
                    theMessagingControl.CreateSendOptionsBuilder().SetHeaders( context.HeadersList.ToList() ).Build(),
                    theSendToFilterCallback );
            }
        }
    }
}