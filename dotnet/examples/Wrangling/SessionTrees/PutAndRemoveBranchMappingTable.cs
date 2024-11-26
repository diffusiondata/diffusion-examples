/**
 * Copyright © 2024 Diffusion Data Ltd.
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
using static System.Console;
using static PushTechnology.ClientInterface.Examples.Program;

namespace PushTechnology.ClientInterface.Examples.Wrangling.SessionTrees
{
    public sealed class PutAndRemoveBranchMappingTable : Example
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

            WriteLine($"Session tree mappings added.");

            await session.SessionTrees.PutBranchMappingTableAsync(Diffusion.NewBranchMappingTableBuilder().Create("my/personal/path"), cancellationToken);

            WriteLine($"Session tree mapping table and mappings removed.");

            session.Close();
        }
    }
}
