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
using PushTechnology.ClientInterface.Client.Callbacks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Features.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing
{
    /// <summary>
    /// Control client implementation of a fallback stream for topics.
    /// </summary>
    public sealed class AddFallbackStream : IExample
    {
        private const string TOPIC_PREFIX = "topic";

        /// <summary>
        /// Runs the client topic fallback stream example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var selector = $"?{TOPIC_PREFIX}//";

            var serverUrl = args[0];
            var session = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var clientSession = Diffusion.Sessions.Principal("client").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            try// Add topics and set their values
            {
                await session.TopicUpdate.AddAndSetAsync("topic/foo", session.TopicControl.NewSpecification(TopicType.STRING), "foo-string");

                await session.TopicUpdate.AddAndSetAsync("topic/bar", session.TopicControl.NewSpecification(TopicType.STRING), "bar-string");

                await session.TopicUpdate.AddAndSetAsync("topic/baz", session.TopicControl.NewSpecification(TopicType.STRING), "baz-string");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic and set its value : {ex}.");

                clientSession.Close();
                session.Close();
                return;
            }

            IValueStream<string> stringStream = null;
            IValueStream<string> fallbackStringStream = null;

            try
            {
                // Add a topic stream for 'topic/foo'
                stringStream = new StringStream();
                clientSession.Topics.AddStream("topic/foo", stringStream);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add value stream : {ex}.");

                await session.TopicControl.RemoveTopicsAsync(selector);

                clientSession.Close();
                session.Close();
                return;
            }

            try
            {
                // Add a fallback stream for the other topics
                fallbackStringStream = new FallbackStringStream();
                clientSession.Topics.AddFallbackStream(fallbackStringStream);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add fallback stream : {ex}.");

                await session.TopicControl.RemoveTopicsAsync(selector);

                clientSession.Topics.RemoveStream(stringStream);

                clientSession.Close();
                session.Close();
                return;
            }

            try
            {
                WriteLine($"Subscribing to topics.");

                await clientSession.Topics.SubscribeAsync(selector, cancellationToken);

                await Task.Delay(TimeSpan.FromSeconds(1));
            }
            catch (Exception ex)
            {
                WriteLine($"Subscribing to topics failed : {ex}.");

                await session.TopicControl.RemoveTopicsAsync(selector);

                clientSession.Topics.RemoveStream(fallbackStringStream);
                clientSession.Topics.RemoveStream(stringStream);

                clientSession.Close();
                session.Close();
                return;
            }

            try//Clear up
            {
                await session.TopicControl.RemoveTopicsAsync(selector);

                WriteLine($"Topics removed.");

                clientSession.Topics.RemoveStream(fallbackStringStream);
                clientSession.Topics.RemoveStream(stringStream);

                await clientSession.Topics.UnsubscribeAsync(selector, cancellationToken);

                WriteLine($"Unsubscribed to topics.");
            }
            catch (Exception ex)
            {
                WriteLine($"Clear up failed : {ex}.");
            }

            clientSession.Close();
            session.Close();
        }

        /// <summary>
        /// Basic implementation of the IValueStream for string topics.
        /// </summary>
        private sealed class StringStream : IValueStream<string>
        {
            /// <summary>
            /// Notification of stream being closed normally.
            /// </summary>
            public void OnClose()
                => WriteLine("The subscription stream is now closed.");

            /// <summary>
            /// Notification of a contextual error related to this callback.
            /// </summary>
            /// <remarks>
            /// Situations in which <code>OnError</code> is called include the session being closed, a communication
            /// timeout, or a problem with the provided parameters. No further calls will be made to this callback.
            /// </remarks>
            /// <param name="errorReason">Error reason.</param>
            public void OnError(ErrorReason errorReason)
                => WriteLine($"An error has occured : {errorReason}.");

            /// <summary>
            /// Notification of a successful subscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            public void OnSubscription(string topicPath, ITopicSpecification specification)
                => WriteLine($"Client subscribed to '{topicPath}'.");

            /// <summary>
            /// Notification of a successful unsubscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="reason">Error reason.</param>
            public void OnUnsubscription(string topicPath, ITopicSpecification specification, TopicUnsubscribeReason reason)
                => WriteLine($"Client unsubscribed from '{topicPath}' : {reason}.");

            /// <summary>
            /// Topic update received.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="oldValue">Value prior to update.</param>
            /// <param name="newValue">Value after update.</param>
            public void OnValue(string topicPath, ITopicSpecification specification, string oldValue, string newValue)
                => WriteLine($"New value of '{topicPath}' is '{newValue}'.");
        }

        /// <summary>
        /// Basic implementation of the IValueStream for string topics as a fallback stream
        /// </summary>
        private sealed class FallbackStringStream : IValueStream<string>
        {
            /// <summary>
            /// Notification of stream being closed normally.
            /// </summary>
            public void OnClose()
                => WriteLine("The fallback stream is now closed.");

            /// <summary>
            /// Notification of a contextual error related to this callback.
            /// </summary>
            /// <remarks>
            /// Situations in which <code>OnError</code> is called include the session being closed, a communication
            /// timeout, or a problem with the provided parameters. No further calls will be made to this callback.
            /// </remarks>
            /// <param name="errorReason">Error reason.</param>
            public void OnError(ErrorReason errorReason)
                => WriteLine($"An error has occured : {errorReason}.");

            /// <summary>
            /// Notification of a successful subscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            public void OnSubscription(string topicPath, ITopicSpecification specification)
                => WriteLine($"Client subscribed to '{topicPath}' using fallback stream.");

            /// <summary>
            /// Notification of a successful unsubscription.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="reason">Error reason.</param>
            public void OnUnsubscription(string topicPath, ITopicSpecification specification, TopicUnsubscribeReason reason)
                => WriteLine($"Client unsubscribed from '{topicPath}' : {reason}.");

            /// <summary>
            /// Topic update received.
            /// </summary>
            /// <param name="topicPath">Topic path.</param>
            /// <param name="specification">Topic specification.</param>
            /// <param name="oldValue">Value prior to update.</param>
            /// <param name="newValue">Value after update.</param>
            public void OnValue(string topicPath, ITopicSpecification specification, string oldValue, string newValue)
                => WriteLine($"New value of '{topicPath}' is '{newValue}'.");
        }
    }
}
