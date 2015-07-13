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

using System.Collections.Generic;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Types;

namespace Examples
{
    /// <summary>
    /// This is a simple example of a client that uses the 'Messaging' feature to send messages on a topic path.
    /// 
    /// To send messages on a topic path, the client session requires the <see cref="TopicPermission.SEND_TO_MESSAGE_HANDLER"/>
    /// permission.
    /// </summary>
    public class ClientSendingMessages
    {
        #region Fields

        private readonly ISession session;
        private readonly IMessaging messaging;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructs a message sending application.
        /// </summary>
        public ClientSendingMessages()
        {
            session = Diffusion.Sessions.Principal( "client" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            messaging = session.GetMessagingFeature();
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Sends a simple string message to a specified topic path.
        /// 
        /// There will be no context with the message so callback will be directed to the 'no context' callback.
        /// </summary>
        /// <param name="topicPath">The topic path.</param>
        /// <param name="message">The message to send.</param>
        /// <param name="callback">Notifies that the message was sent.</param>
        public void Send( string topicPath, string message, ISendCallback callback )
        {
            messaging.Send( topicPath, Diffusion.Content.NewContent( message ), callback );
        }

        /// <summary>
        /// Sends a simple string message to a specified topic path with context string.
        /// 
        /// The callback will be directed to the contextual callback with the string provided.
        /// </summary>
        /// <param name="topicPath"></param>
        /// <param name="message"></param>
        /// <param name="context"></param>
        /// <param name="callback"></param>
        public void Send( string topicPath, string message, string context, ISendContextCallback<string> callback )
        {
            messaging.Send( topicPath, Diffusion.Content.NewContent( message ), context, callback );
        }

        /// <summary>
        /// Sends a string message to a specified topic with headers.
        /// 
        /// There will be no context with the message so callback will be directed to the 'no context' callback.
        /// </summary>
        /// <param name="topicPath">The topic path.</param>
        /// <param name="message">The message to send.</param>
        /// <param name="headers">The headers to send with the message.</param>
        /// <param name="callback">Notifies that the message was sent.</param>
        public void SendWithHeaders( string topicPath, string message, List<string> headers, ISendCallback callback )
        {
            messaging.Send(
                topicPath,
                Diffusion.Content.NewContent( message ),
                messaging.CreateSendOptionsBuilder().SetHeaders( headers ).Build(),
                callback );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            session.Close();
        }

        #endregion Public Methods
    }
}