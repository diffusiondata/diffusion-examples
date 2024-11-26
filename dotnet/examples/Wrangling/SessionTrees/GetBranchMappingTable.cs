/**
 * Copyright © 2023 - 2024 Diffusion Data Ltd.
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
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.Wrangling.SessionTrees
{
    public sealed class GetBranchMappingTable : Example
    {
        public override async Task Run(CancellationToken cancellationToken, string[] args)
        {
            string serverUrl = args[0];

            var session = Diffusion.Sessions
                .Principal("admin")
                .Credentials(Diffusion.Credentials.Password("password"))
                .Open(serverUrl);

            var table = Diffusion.NewBranchMappingTableBuilder()
                .AddBranchMapping("$Principal is 'admin'", "my/topic/path/for/admin")
                .AddBranchMapping("$Principal is 'control'", "my/topic/path/for/control")
                .AddBranchMapping("$Principal is ''", "my/topic/path/for/anonymous")
                .Create("my/personal/path");

            await session.SessionTrees.PutBranchMappingTableAsync(table, cancellationToken);

            var table2 = Diffusion.NewBranchMappingTableBuilder()
                .AddBranchMapping("$Transport is 'WEBSOCKET'", "my/alternate/path/for/websocket")
                .AddBranchMapping("$Transport is 'HTTP_LONG_POLL'", "my/alternate/path/for/http")
                .AddBranchMapping("$Transport is 'TCP'", "my/alternate/path/for/tcp")
                .Create("my/alternate/path");

            await session.SessionTrees.PutBranchMappingTableAsync(table2, cancellationToken);

            var listSessionTreeBranches = await session.SessionTrees.GetSessionTreeBranchesWithMappingsAsync(cancellationToken);

            foreach (string sessionTreeBranch in listSessionTreeBranches)
            {
                WriteLine($"{sessionTreeBranch}:");

                var branchMappingTable = await session.SessionTrees.GetBranchMappingTableAsync(sessionTreeBranch, cancellationToken);

                foreach (var branchMapping in branchMappingTable.BranchMappings)
                {
                    WriteLine($"{branchMapping.SessionFilter}: {branchMapping.TopicTreeBranch}");
                }
            }

            session.Close();
        }
    }
}
