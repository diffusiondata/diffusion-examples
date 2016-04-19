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

using PushTechnology.ClientInterface.Client.Enums;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.CommandServices.Commands.Control.Client;

namespace Examples
{
    /// <summary>
    /// This demonstrates the use of a control client to apply both throttling and conflation to clients.  It throttles
    /// and conflates all clients that reach their queue thresholds and remove when they go down again.
    /// 
    /// This uses the <see cref="IClientControl"/> feature.
    /// </summary>
    public class ControlClientConflateAndThrottle
    {
        #region Fields

        private readonly ISession session;

        #endregion Fields

        #region Constructor

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="callback">Notifies callback from throttle requests.</param>
        public ControlClientConflateAndThrottle( IClientCallback callback )
        {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            // Create the client control feature with a handler that sets queue thresholds on new connecting clients
            // and sets a listener for queue events.
            var clientControl = session.GetClientControlFeature();

            // To register a queue event handler, the client session must have the REGISTER_HANDLER and VIEW_SESSION
            // permissions.
            clientControl.SetQueueEventHandler( new MyThresholdHandler( clientControl, callback ) );
        }

        #endregion Constructor

        #region Public Methods

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close()
        {
            session.Close();
        }

        #endregion Public Methods

        #region Private Classes

        private class MyThresholdHandler : QueueEventHandlerDefault
        {
            #region Fields

            private readonly IClientControl theClientControl;
            private readonly IClientCallback theClientCallback;

            #endregion Fields

            #region Constructor

            public MyThresholdHandler( IClientControl clientControl, IClientCallback clientCallback )
            {
                theClientControl = clientControl;
                theClientCallback = clientCallback;
            }

            #endregion Constructor

            #region Overrides

            /// <summary>
            /// The configured upper queue threshold for a client's queue has been reached.
            /// </summary>
            /// <param name="client">The client session identifier.</param>
            /// <param name="policy">The message queue policy.</param>
            public override void OnUpperThresholdCrossed( SessionId client, IMessageQueuePolicy policy )
            {
                // The SetThrottled method enables throttling and conflation.  This method requires the client session
                // to have the MODIFY_SESSION topic permission.
                theClientControl.SetThrottled( client, ThrottlerType.MESSAGE_INTERVAL, 10, theClientCallback );
            }

            /// <summary>
            /// The configured lower threshold for a client's queue has been reached.
            /// </summary>
            /// <param name="client">The client session identifier.</param>
            /// <param name="policy">The message queue policy.</param>
            public override void OnLowerThresholdCrossed( SessionId client, IMessageQueuePolicy policy )
            {
                // The SetThrottled method enables throttling and conflation.  This method requires the client session
                // to have the MODIFY_SESSION topic permission.
                theClientControl.SetThrottled( client, ThrottlerType.MESSAGE_INTERVAL, 1000, theClientCallback );
            }

            #endregion Overrides
        }

        #endregion Private Classes
    }
}