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
using PushTechnology.ClientInterface.Client.Features.Control.Clients;
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that subscribes to topics.
    /// </summary>
    public sealed class SubscriptionControlSubscribe : IExample
    {
        /// <summary>
        /// Runs the control client example that subscribes to topics.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var topic = $"topic/example";

            var serverUrl = args[0];
            var controlSession = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var subscriptionControl = controlSession.SubscriptionControl;

            try
            {
                await controlSession.TopicControl.AddTopicAsync(topic, TopicType.STRING, cancellationToken);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{topic}' : {ex}.");
                controlSession.Close();
                return;
            }

            ISession session = Diffusion.Sessions.Principal("client")
                .Credentials(Diffusion.Credentials.Password("password"))
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .NoReconnection()
                .Open(serverUrl);

            WriteLine($"Session with id '{session.SessionId}' created.");

            var subscriptionCallback = new SubscriptionCallback();

            var topicSelector = "?topic//";

            try
            {
                subscriptionControl.Subscribe(session.SessionId, topicSelector, subscriptionCallback);

                WriteLine($"Session with id '{session.SessionId}' is subscribed to '{topicSelector}'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to subscribe to '{topicSelector}' : {ex}.");
                session.Close();
                controlSession.Close();
                return;
            }

            try
            {
                subscriptionControl.Unsubscribe(session.SessionId, topicSelector, subscriptionCallback);

                WriteLine($"Session with id '{session.SessionId}' is unsubscribed to '{topicSelector}'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to unsubscribe to '{topicSelector}' : {ex}.");
            }

            // Close the sessions
            session.Close();
            controlSession.Close();
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
