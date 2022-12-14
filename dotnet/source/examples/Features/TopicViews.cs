/**
 * Copyright © 2022 Push Technology Ltd.
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
using System.Text;
using System.Threading.Tasks;
using System.Threading;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Features;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;
using System.Linq;

namespace PushTechnology.ClientInterface.Example.Features {
    /// <summary>
    /// Client implementation that creates and manages topic views.
    /// </summary>
    public sealed class TopicViews : IExample
    {
        /// <summary>
        /// Runs the topic views example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            var serverUrl = args[0];
            var session = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            // Create a topic
            var topic = "a/path/0";

            try
            {
                await session.TopicControl.AddTopicAsync(topic, TopicType.JSON, cancellationToken);
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add topic '{topic}' : {ex}.");
                session.Close();
                return;
            }

            try
            {
                var specification = "map ?a/path/ to prefix/<path(0)>";

                ITopicView view1 = await session.TopicViews.CreateTopicViewAsync("View1", specification, CancellationToken.None);
                WriteLine($"Topic view '{view1.Name}' was created.");

                ITopicView view2 = await session.TopicViews.CreateTopicViewAsync("View2", specification, CancellationToken.None);
                WriteLine($"Topic view '{view2.Name}' was created.");
            }
            catch (Exception ex)
            {
                WriteLine($"Topic view could not be created : {ex}.");
                session.Close();
                return;
            }

            try
            {
                var listTask = await session.TopicViews.ListTopicViewsAsync(CancellationToken.None);
                List<ITopicView> views = listTask.ToList();

                WriteLine($"Listing topic views - {views.Count} views were found:");

                foreach (var view in views)
                {
                    WriteLine($"Topic view '{view.Name}'");
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Topic views could not be listed : {ex}.");
            }

            try
            {
                ITopicView view1 = await session.TopicViews.GetTopicViewAsync("View1", CancellationToken.None);
                WriteLine($"Topic view '{view1.Name}' was retrieved with specification '{view1.Specification}' and roles:");

                foreach (var role in view1.Roles)
                {
                    WriteLine($"'{role}'");
                }
            }
            catch (Exception ex)
            {
                WriteLine($"Topic view 'View1' could not be retrieved : {ex}.");
                session.Close();
                return;
            }

            try
            {
                await session.TopicViews.RemoveTopicViewAsync("View1", CancellationToken.None);
                WriteLine($"Topic view 'View1' was removed.");
                await session.TopicViews.RemoveTopicViewAsync("View2", CancellationToken.None);
                WriteLine($"Topic view 'View2' was removed.");
            }
            catch (Exception ex)
            {
                WriteLine($"Topic view could not be removed : {ex}.");
            }

            // Close the session
            session.Close();
        }
    }
}
