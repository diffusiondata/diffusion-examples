/**
 * Copyright © 2021 Push Technology Ltd.
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
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Types;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that subscribes to topics with a filter.
    /// </summary>
    public sealed class SubscriptionControlSubscribeFilter : IExample
    {
        /// <summary>
        /// Runs the control client example that subscribes to topics with a filter.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var topic = $"?topic-example//";

            var serverUrl = args[0];
            var controlSession = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            WriteLine($"Session with id '{controlSession.SessionId}' created.");

            var filter = "$SessionId is \"" + controlSession.SessionId + "\"";

            var subscriptionControl = controlSession.SubscriptionControl;

            var filterCallback = new SubscriptionByFilterCallback();

            try
            {
                subscriptionControl.SubscribeByFilter(filter, topic, filterCallback);

                WriteLine($"Sessions satisfying filter '{filter}' are subscribed to '{topic}'.");

                await Task.Delay(TimeSpan.FromMilliseconds(300));
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to subscribe by filter '{filter}' : {ex}.");
                controlSession.Close();
                return;
            }

            try
            {
                subscriptionControl.UnsubscribeByFilter(filter, topic, filterCallback);

                WriteLine($"Sessions satisfying filter '{filter}' are unsubscribed to '{topic}'.");

                await Task.Delay(TimeSpan.FromMilliseconds(300));
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to unsubscribe by filter '{filter}' : {ex}.");
            }

            controlSession.Close();
        }

        /// <summary>
        /// The callback for filtered subscriptions and unsubscriptions.
        /// </summary>
        private class SubscriptionByFilterCallback : ISubscriptionByFilterCallback
        {
            /// <summary>
            /// Indicates successful processing of the request at the server.
            /// </summary>
            /// <param name="numberSelected">Indicates the number of sessions that satisfied the filter and which qualified
            /// for subscription/unsubscription.</param>
            public void OnComplete(int numberSelected)
            {
                WriteLine($"The number of sessions that qualified for subscription/unsubscription is: {numberSelected}.");
            }

            /// <summary>
            /// The filter was rejected. No sessions were subscribed/unsubscribed.
            /// </summary>
            /// <param name="errors">Errors found.</param>
            public void OnRejected(ICollection<IErrorReport> errors)
            {
                WriteLine($"The following errors occured:");

                foreach(var error in errors)
                {
                    WriteLine($"{error}.");
                }
            }

            /// <summary>
            /// Notification of a contextual error related to this callback.
            /// </summary>
            /// <param name="errorReason">Error reason provided.</param>
            public void OnError(ErrorReason errorReason)
            {
                WriteLine($"An error has occured : {errorReason}.");
            }
        }
    }
}
