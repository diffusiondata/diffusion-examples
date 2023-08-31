/**
 * Copyright © 2020 - 2023 DiffusionData Ltd.
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
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using PushTechnology.ClientInterface.Client.Topics;
using PushTechnology.ClientInterface.Data.JSON;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Publishing {
    /// <summary>
    /// Control client implementation that partially updates a JSON topic by applying a JSON patch.
    /// </summary>
    public sealed class ApplyJSONPatch : IExample {
        /// <summary>
        /// Runs the apply JSON patch control client example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run( CancellationToken cancellationToken, string[] args ) {
            string TOPIC_PREFIX = "test-topics";

            var serverUrl = args[0];
            var session = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);
            var topicControl = session.TopicControl;
            var topicUpdate = session.TopicUpdate;

            var topicPath = $"{TOPIC_PREFIX}/{Guid.NewGuid()}";
            string json = "{\"foo\":\"bar\"}";

            // Add and set the JSON topic
            try
            {
                await topicUpdate.AddAndSetAsync(topicPath, topicControl.NewSpecification(TopicType.JSON), Diffusion.DataTypes.JSON.FromJSONString(json));

                var result = await session.Topics.FetchRequest.First(1).TopicTypes(new[] { TopicType.JSON }).WithValues<IJSON>().FetchAsync(topicPath);

                WriteLine($"Topic '{topicPath}' successfully added with value '{result.Results.First().Value.ToJSONString()}'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to add/set topic '{topicPath}' : {ex}.");
                session.Close();
                return;
            }

            try
            {
                //1) Add new array value
                string patch = "[{\"op\":\"add\",\"path\":\"/a\",\"value\":[\"c\",\"d\"]}]";            

                await topicUpdate.ApplyJSONPatchAsync(topicPath, patch);

                var result = await session.Topics.FetchRequest.First(1).TopicTypes(new[] { TopicType.JSON }).WithValues<IJSON>().FetchAsync(topicPath);

                WriteLine($"Applying json patch '{patch}' gives '{result.Results.First().Value.ToJSONString()}'.");

                //2) Add new array item at start
                patch = "[{\"op\":\"add\",\"path\":\"/a/0\",\"value\":\"qux\"}]";            

                await topicUpdate.ApplyJSONPatchAsync(topicPath, patch);

                result = await session.Topics.FetchRequest.First(1).TopicTypes(new[] { TopicType.JSON }).WithValues<IJSON>().FetchAsync(topicPath);

                WriteLine($"Applying json patch '{patch}' gives '{result.Results.First().Value.ToJSONString()}'.");

                //3) Replace foo's value
                patch = "[{\"op\":\"replace\",\"path\":\"/foo\",\"value\":[1,2,3]}]";

                await topicUpdate.ApplyJSONPatchAsync(topicPath, patch);

                result = await session.Topics.FetchRequest.First(1).TopicTypes(new[] { TopicType.JSON }).WithValues<IJSON>().FetchAsync(topicPath);

                WriteLine($"Applying json patch '{patch}' gives '{result.Results.First().Value.ToJSONString()}'.");

                //4) Replace foo's array item at start
                patch = "[{\"op\":\"replace\",\"path\":\"/foo/0\",\"value\":\"qux\"}]";

                await topicUpdate.ApplyJSONPatchAsync(topicPath, patch);

                result = await session.Topics.FetchRequest.First(1).TopicTypes(new[] { TopicType.JSON }).WithValues<IJSON>().FetchAsync(topicPath);

                WriteLine($"Applying json patch '{patch}' gives '{result.Results.First().Value.ToJSONString()}'.");

                //5) Remove foo's array item at start
                patch = "[{\"op\":\"remove\",\"path\":\"/foo/0\"}]";

                await topicUpdate.ApplyJSONPatchAsync(topicPath, patch);

                result = await session.Topics.FetchRequest.First(1).TopicTypes(new[] { TopicType.JSON }).WithValues<IJSON>().FetchAsync(topicPath);

                WriteLine($"Applying json patch '{patch}' gives '{result.Results.First().Value.ToJSONString()}'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to apply patch: {ex}.");
                session.Close();
                return;
            }

            // Close the session
            session.Close();
        }
    }
}
