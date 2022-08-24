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
using PushTechnology.ClientInterface.Client.Features.Control.Topics;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Topics.Details;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation of a listener for topic notifications.
    /// </summary>
    public sealed class TopicNotificationListener : IExample {
        private const string TOPIC_PREFIX = "topic-notifications";

        /// <summary>
        /// Runs the client topic notification listener example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            var selector = $"?{TOPIC_PREFIX}//";

            var serverUrl = args[ 0 ];
            var session = Diffusion.Sessions.Principal( "control" ).Password( "password" )
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open( serverUrl );

            var notifications = session.TopicNotifications;

            INotificationRegistration registration = null;
            string path = string.Empty;

            try
            {
                //Register a listener to receive topic notifications
                registration = await notifications.AddListenerAsync(new Listener());
            }
            catch(Exception ex)
            {
                WriteLine($"Failed to add listener : {ex}.");
                session.Close();
                return;
            }

            try
            {
                //Start receiving notifications
                await registration.SelectAsync(selector);
            }
            catch (Exception ex)
            {
                WriteLine($"Selector '{selector}' registration failed : {ex}.");
                session.Close();
                return;
            }

            try
            {
                //Add topic
                path = $"{TOPIC_PREFIX}/{DateTime.Now.ToFileTimeUtc()}";
                var specification = session.TopicControl.NewSpecification(TopicType.STRING);

                await session.TopicControl.AddTopicAsync(path, specification);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{path}' : {ex}.");
                session.Close();
                return;
            }

            try
            {
                //Remove topic
                await session.TopicControl.RemoveTopicsAsync(path);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to remove topic '{path}' : {ex}.");
                session.Close();
                return;
            }

            try
            {
                //Stop receiving notifications
                await registration.DeselectAsync(selector);
            }
            catch (Exception ex)
            {
                WriteLine($"Deselection failed for selector '{selector}' : {ex}.");
                session.Close();
                return;
            }

            try
            {
                //Unregister the listener
                await registration.CloseAsync();
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to unregister the listener : {ex}.");
            }

            // Close the session
            session.Close();
        }

        /// <summary>
        /// The listener for topic notifications.
        /// </summary>
        private class Listener : ITopicNotificationListener {
            /// <summary>
            /// Indicates that the stream was closed.
            /// </summary>
            public void OnClose()
            {
                WriteLine("The listener was closed.");
            }

            /// <summary>
            /// Notification for an immediate descendant of a selected topic path.
            /// </summary>
            public void OnDescendantNotification(string topicPath, NotificationType type)
            {
                WriteLine($"Descendant topic '{topicPath}' has been {type}.");
            }

            /// <summary>
            /// Indicates an error received by the callback.
            /// </summary>
            public void OnError(ErrorReason errorReason)
            {
                WriteLine($"The listener received the error: '{errorReason}'.");
            }

            /// <summary>
            /// Notification for a selected topic.
            /// </summary>
            public void OnTopicNotification(string topicPath, ITopicSpecification specification, NotificationType type)
            {
                WriteLine($"Topic '{topicPath}' has been {type}.");
            }
        }
    }
}
