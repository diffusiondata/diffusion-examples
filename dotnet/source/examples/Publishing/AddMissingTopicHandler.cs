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
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing
{
    /// <summary>
    /// Control client implementation of a missing topic handler.
    /// </summary>
    public sealed class AddMissingTopicHandler : IExample
    {
        private const string TOPIC_PREFIX = "Example";
        private ISession session;

        /// <summary>
        /// Runs the client missing topic handler example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var selector = $"?{TOPIC_PREFIX}//";
            var topicPath = $"{TOPIC_PREFIX}";

            var serverUrl = args[0];
            session = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            var clientSession = Diffusion.Sessions.Principal("client").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            IRegistration registration = null;

            try
            {
                WriteLine($"Adding missing topic handler for topic '{topicPath}'.");

                registration = await session.TopicControl.AddMissingTopicHandlerAsync(topicPath, new MissingTopicNotificationStream(session));
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add missing topic handler : {ex}.");

                clientSession.Close();
                session.Close();
                return;
            }

            try
            {
                WriteLine($"Subscribing to topic '{topicPath}'.");

                await clientSession.Topics.SubscribeAsync(selector, cancellationToken);

                await Task.Delay(TimeSpan.FromSeconds(1));
            }
            catch (Exception ex)
            {
                WriteLine($"Subscribing to topic '{topicPath}' failed : {ex}.");

                await registration.CloseAsync();

                clientSession.Close();
                session.Close();
                return;
            }

            try//Clear up
            {
                await session.TopicControl.RemoveTopicsAsync(topicPath);

                WriteLine($"Topic '{topicPath}' removed.");

                await clientSession.Topics.UnsubscribeAsync(selector, cancellationToken);

                WriteLine($"Unsubscribing to topic '{topicPath}'.");

                await registration.CloseAsync();
            }
            catch(Exception ex)
            {
                WriteLine($"Clear up failed : {ex}.");
            }

            clientSession.Close();
            session.Close();
        }

        /// <summary>
        /// Basic implementation of the stream that will be called when a session subscribes using a topic selector
        /// that matches no topics.
        /// </summary>
        private sealed class MissingTopicNotificationStream : IMissingTopicNotificationStream
        {
            private ISession session;

            public MissingTopicNotificationStream(ISession session)
            {
                this.session = session;
            }

            /// <summary>
            /// Notification of stream being closed normally.
            /// </summary>
            public void OnClose()
                => WriteLine("Handler is removed.");

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
            /// Called when a session requests a topic that does not exist, and the topic path
            /// belongs to part of the topic tree for which this stream was registered.
            /// </summary>
            /// <param name="notification"></param>
            public void OnMissingTopic(IMissingTopicNotification notification)
            {
                WriteLine($"Topic '{notification.TopicPath}' does not exist.");

                session.TopicControl.AddTopic(notification.TopicPath, session.TopicControl.NewSpecification(TopicType.STRING), new TopicControlAddCallback(notification));
            }
        }

        /// <summary>
        /// Implementation of a callback interface for adding topics.
        /// </summary>
        private sealed class TopicControlAddCallback : ITopicControlAddCallback
        {
            IMissingTopicNotification notification;

            public TopicControlAddCallback(IMissingTopicNotification notification)
            {
                this.notification = notification;
            }

            /// <summary>
            /// Called to notify that the session is closed.
            /// </summary>
            public void OnDiscard()
            {
                WriteLine("The stream is now closed.");
            }

            /// <summary>
            /// Called to indicate that the topic has been successfully added.
            /// </summary>
            /// <param name="topicPath"></param>
            public void OnTopicAdded(string topicPath)
            {
                notification.Proceed();

                WriteLine($"Topic '{topicPath}' added.");
            }

            /// <summary>
            /// Called to indicate that an attempt to add a topic has failed.
            /// </summary>
            /// <param name="topicPath"></param>
            /// <param name="reason"></param>
            public void OnTopicAddFailed(string topicPath, TopicAddFailReason reason)
            {
                WriteLine($"The topic could not be added with reason: {reason}.");
            }
        }
    }
}
