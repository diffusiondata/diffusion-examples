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
using PushTechnology.ClientInterface.Client.Factories;
using PushTechnology.ClientInterface.Client.Session;
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Runner.Program;

namespace PushTechnology.ClientInterface.Example.Features
{
    /// <summary>
    /// Client implementation that demonstrates session trees.
    /// </summary>
    public sealed class SessionTrees : IExample
    {
        /// <summary>
        /// Runs the session trees example.
        /// </summary>
        /// <param name="cancellationToken">A token used to end the client example.</param>
        /// <param name="args">A single string should be used for the server url.</param>
        public async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];
            var session = Diffusion.Sessions.Principal("control").Password("password")
                .CertificateValidation((cert, chain, errors) => CertificateValidationResult.ACCEPT)
                .Open(serverUrl);

            try
            {
                var table = Diffusion.NewBranchMappingTableBuilder()
                    .AddBranchMapping("$Principal is 'control'", "target/1")
                    .AddBranchMapping("all", "target/2")
                    .Create("source/path");

                await session.SessionTrees.PutBranchMappingTableAsync(table);

                WriteLine($"Branch mapping table created for session tree branch '{table.SessionTreeBranch}'.");
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to create branch mapping table : {ex}.");
                session.Close();
                return;
            }

            IReadOnlyCollection<string> listSessionTreeBranches = null;

            try
            {
                WriteLine($"Retrieving session tree branches.");

                listSessionTreeBranches = await session.SessionTrees.GetSessionTreeBranchesWithMappingsAsync();
            }
            catch (Exception ex)
            {
                WriteLine($"Failed to retrieve session tree branches : {ex}.");
                session.Close();
                return;
            }

            try
            {
                WriteLine($"Retrieving branch mapping table:");

                foreach (string sessionTreeBranch in listSessionTreeBranches)
                {
                    var branchMappingTable = await session.SessionTrees.GetBranchMappingTableAsync(sessionTreeBranch);

                    foreach (var branchMapping in branchMappingTable.BranchMappings)
                    {
                        WriteLine($"Session tree branch: '{sessionTreeBranch}', Session filter: '{branchMapping.SessionFilter}', Topic tree branch: '{branchMapping.TopicTreeBranch}'");
                    }
                }
            }
            catch(Exception ex)
            {
                WriteLine($"Failed to retrieve a branch mapping : {ex}.");
            }

            // Close the session
            session.Close();
        }
    }
}