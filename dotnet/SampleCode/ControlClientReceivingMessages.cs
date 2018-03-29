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

using System.Linq;
using PushTechnology.ClientInterface.Client.Content;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Types;

namespace Examples {
    /// <summary>
    /// This is an example of a control client using the <see cref="IMessagingControl"/> feature to receive messages
    /// from clients and also send messages to clients.
    ///
    /// It is a trivial example that simply responds to all messages on a particular branch of the topic tree by
    /// echoing them back to the client exactly as they are, complete with headers.
    /// </summary>
    public class ControlClientReceivingMessages {
        private readonly ISession session;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="callback">The callback to receive the result of message sending.</param>
        public ControlClientReceivingMessages( ISendCallback callback ) {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            var messagingControl = session.MessagingControl;

            // Register to receive all messages sent by clients on the "foo" branch.
            // To do this, the client session must have the REGISTER_HANDLER permission.
            messagingControl.AddMessageHandler( "foo", new EchoHandler( messagingControl, callback ) );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close() {
            session.Close();
        }

        private class EchoHandler : MessageHandlerDefault {
            private readonly IMessagingControl theMessagingControl;
            private readonly ISendCallback theSendCallback;

            public EchoHandler( IMessagingControl messagingControl, ISendCallback sendCallback ) {
                theMessagingControl = messagingControl;
                theSendCallback = sendCallback;
            }

            /// <summary>
            /// Receives content sent from a session via a topic.
            /// </summary>
            /// <param name="sessionId">Identifies the client session that sent the content.</param>
            /// <param name="topicPath">The path of the topic that the content was sent on.</param>
            /// <param name="content">The content sent by the client.</param>
            /// <param name="context">The context associated with the content.</param>
            public override void OnMessage( SessionId sessionId, string topicPath, IContent content,
                IReceiveContext context ) {
                theMessagingControl.Send( sessionId, topicPath, content,
                    theMessagingControl.CreateSendOptionsBuilder().SetHeaders( context.HeadersList.ToList() ).Build(),
                    theSendCallback );
            }
        }

        private class MessageHandlerDefault : TopicTreeHandlerDefault, IMessageHandler {
            /// <summary>
            /// Receives content sent from a session via a topic.
            /// </summary>
            /// <param name="sessionId">Identifies the client session that sent the content.</param>
            /// <param name="topicPath">The path of the topic that the content was sent on.</param>
            /// <param name="content">The content sent by the client.</param>
            /// <param name="context">The context associated with the content.</param>
            public virtual void OnMessage( SessionId sessionId, string topicPath, IContent content,
                IReceiveContext context ) {
            }
        }
    }
}