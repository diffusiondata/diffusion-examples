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
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that routes subscriptions.
    /// </summary>
    public sealed class SubscriptionControlRoutingHandler : IExample
    {
        /// <summary>
        /// Runs the control client example that routes subscriptions.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var topic = "branch/routingTopic";

            var serverUrl = args[0];
            var controlSession = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var subscriptionControl = controlSession.SubscriptionControl;

            try
            {
                await controlSession.TopicControl.AddTopicAsync(topic, controlSession.TopicControl.NewSpecification(TopicType.ROUTING), cancellationToken);

                await controlSession.TopicControl.AddTopicAsync("branch/someTopic", TopicType.STRING, cancellationToken);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{topic}' : {ex}.");
                controlSession.Close();
                return;
            }

            try
            {
                // Sets up a handler so that all subscriptions to topic 'branch' are routed.
                subscriptionControl.AddRoutingSubscriptionHandler("branch", new RoutingSubscriptionRequestHandler());

                WriteLine($"Routing handler added for topic 'branch'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add routing handler for topic 'branch' : {ex}.");
            }

            ISession session = Diffusion.Sessions.Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .NoReconnection()
                .Open(serverUrl);

            WriteLine($"Session with id '{session.SessionId}' created.");

            try
            {
                await session.Topics.SubscribeAsync("?branch/", cancellationToken);

                WriteLine($"Session with id '{session.SessionId}' is subscribed to '?branch/'.");

                await Task.Delay(TimeSpan.FromMilliseconds(300));
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to subscribe to '?branch/' : {ex}.");
                session.Close();
                controlSession.Close();
                return;
            }

            try
            {
                WriteLine($"Removing topics for path '?branch/'.");

                var result = await session.TopicControl.RemoveTopicsAsync("?branch/", cancellationToken);

                WriteLine($"{result.RemovedCount} topics successfully removed.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to remove topics for path '?branch/' : {ex}.");
            }

            // Close the sessions
            controlSession.Close();
        }

        /// <summary>
        /// The handler for routing subscription requests.
        /// </summary>
        private class RoutingSubscriptionRequestHandler : IRoutingSubscriptionRequestHandler
        {
            /// <summary>
            /// Called when the handler has been registered at the server and is now active.
            /// </summary>
            /// <param name="topicPath">Path to topic.</param>
            /// <param name="registeredHandler">Reference to a registered handler.</param>
            public void OnActive(string topicPath, IRegisteredHandler registeredHandler)
            {
                WriteLine($"Handler registered for '{topicPath}'.");
            }

            /// <summary>
            /// Called if the handler is closed.
            /// </summary>
            /// <param name="topicPath">Path to topic.</param>
            public void OnClose(string topicPath)
            {
                WriteLine($"Handler closed for '{topicPath}'.");
            }

            /// <summary>
            /// A request to subscribe to a specific routing topic.
            /// </summary>
            /// <param name="request">The request to subscribe to a routing topic.</param>
            public void OnSubscriptionRequest(IRoutingSubscriptionRequest request)
            {
                var topic = "branch/someTopic";

                try
                {
                    WriteLine($"Routing subscription to '{topic}'.");
                    request.Route(topic, new SubscriptionCallback());
                }
                catch(Exception ex)
                {
                    WriteLine($"Subscription routing failed: {ex}.");
                }
            }
        }

        /// <summary>
        /// The callback for subscription operations.
        /// </summary>
        private class SubscriptionCallback : ISubscriptionCallback
        {
            /// <summary>
            /// Indicates that the session was closed.
            /// </summary>
            public void OnDiscard()
            {
                WriteLine("The session was closed.");
            }

            /// <summary>
            /// Indicates that a requested operation has been handled by the server.
            /// </summary>
            public void OnComplete()
            {
                WriteLine("Subscription complete.");
            }
        }
    }
}
