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

namespace Examples {
    /// <summary>
    /// This demonstrates using a control client to be notified of subscription requests to routing topics.
    ///
    /// This uses the <see cref="ISubscriptionControl"/> feature.
    /// </summary>
    public class ControlClientSubscriptionControlRouting {
        private readonly ISession session;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="routingCallback">The callback for routing subscription requests.</param>
        public ControlClientSubscriptionControlRouting( ISubscriptionCallback routingCallback ) {
            session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .Open( "ws://diffusion.example.com:80" );

            var subscriptionControl = session.GetSubscriptionControlFeature();

            // Sets up a handler so that all subscriptions to topic 'a/b' are routed to the routing/target topic.
            // To do this, the client session requires the VIEW_SESSION, MODIFY_SESSION and REGISTER_HANDLER
            // permissions.
            subscriptionControl.AddRoutingSubscriptionHandler( "a/b", new SubscriptionHandler( routingCallback ) );
        }

        /// <summary>
        /// Close the session.
        /// </summary>
        public void Close() {
            session.Close();
        }

        private class SubscriptionHandler : RoutingSubscriptionRequestHandlerDefault {
            private readonly ISubscriptionCallback theRoutingCallback;

            public SubscriptionHandler( ISubscriptionCallback callback ) {
                theRoutingCallback = callback;
            }

            /// <summary>
            /// A request to subscribe to a specific routing topic.
            /// </summary>
            /// <param name="request"></param>
            public override void OnSubscriptionRequest( IRoutingSubscriptionRequest request ) {
                request.Route( "routing/target/topic", theRoutingCallback );
            }
        }
    }
}