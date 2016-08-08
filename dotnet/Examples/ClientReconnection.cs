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


using System;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Session.Reconnection;

namespace Examples {
    /// <summary>
    /// These examples show how to configure and enable the reconnection feature of the API.
    /// Every method represents a different selfcontained example.
    /// </summary>
    public class ClientReconnection {
        private static int counter = 0;

        /// <summary>
        /// Sets the reconnection timeout that represents the duration in which the client is trying to reconnect to
        /// the server.
        /// If we are not reconnected after the timeout, the client will close the session.
        /// </summary>
        public void SetReconnectionTimeout() {
            // The timeout is set in milliseconds and should be high enough to
            // account for actual reconnection time
            var sessionFactory = Diffusion.Sessions.ReconnectionTimeout( 60000 );
        }

        /// <summary>
        /// Disables the reconnection feature.
        /// </summary>
        public void DisableReconnection() {
            // This will disable the reconnection feature completely and instead of switching to the RECOVERING_RECONNECT
            // session state it will switch straight to CLOSED_BY_SERVER.
            var sessionFactoryNoReconnection = Diffusion.Sessions.NoReconnection();

            // This call has exactly the same effect as the above statement.
            var sessionFactoryNoTimeout = Diffusion.Sessions.ReconnectionTimeout( 0 );
        }

        /// <summary>
        /// This is a custom reconnection strategy that will try to reconnect
        /// to the server up to 3 times and then abort.
        /// </summary>
        public class MyReconnectionStrategy : IReconnectionStrategy {
            /// <summary>
            /// Here we put our actual reconnection logic. The async keyword should always be added since it makes
            /// things easier for a void return type.
            /// </summary>
            /// <param name="reconnectionAttempt">The reconnection attempt wil be given by the session.</param>
            public async Task PerformReconnection( IReconnectionAttempt reconnectionAttempt ) {
                ++counter;
                if ( counter <= 3 ) {
                    // We start the next reconnection attempt
                    reconnectionAttempt.Start();
                } else {
                    counter = 0;

                    // We abort any other reconnection attempt and let the session switch to CLOSED_BY_SERVER.
                    reconnectionAttempt.Abort();
                }
            }
        }

        /// <summary>
        /// This applies the custom reconnection strategy.
        /// </summary>
        public void SetCustomReconnectionStrategy() {
            // We don't need to hold a reference to the reconnection strategy
            var sessionFactoryWithCustomStrategy = Diffusion.Sessions.ReconnectionStrategy( new MyReconnectionStrategy() );
        }

        /// <summary>
        /// Reconnection can be observed via session state changes within the SessionStateChangeHandler.
        /// </summary>
        public void ObserveReconnection() {
            var sessionFactory = Diffusion.Sessions.SessionStateChangedHandler( ( sender, args ) => {
                if ( args.NewState.Equals( SessionState.RECOVERING_RECONNECT ) ) {
                    // This will be set on a connection loss and indicates a reconnection attempt.
                    // Unless reconnection is disabled, at which point the session never gets switched to this state.
                    Console.WriteLine( "We are in the process of reconnecting." );
                } else if ( args.NewState.Equals( SessionState.CONNECTION_ATTEMPT_FAILED ) ) {
                    // If a reconnection attempt fails because the server session timed out, we won't be able
                    // to reconnect anymore. At which point the session will switch to this state.
                    Console.WriteLine( "We couldn't connect." );
                } else if ( args.NewState.Equals( SessionState.CLOSED_BY_SERVER ) ) {
                    // If the reconnection timeout is over, we will switch to this state. In case of disabled
                    // reconnection we will switch directly to this state on a connection loss.
                    Console.WriteLine( "We lost connection." );
                } else if ( args.NewState.Equals( SessionState.CONNECTED_ACTIVE ) ) {
                    // This is the obvious state on our first connection. It is also the state to which we switch
                    // after a successful reconnection attempt.
                    Console.WriteLine( "We are connected." );
                    counter = 0;
                }
            } );
        }
    }
}