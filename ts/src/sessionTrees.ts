/*******************************************************************************
 * Copyright (C) 2021 - 2023 DiffusionData Ltd.
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
 *******************************************************************************/

import {
    connect,
    Session,
    BranchMappingTable ,
    newBranchMappingTableBuilder,
} from 'diffusion';

// example showcasing how create and query session tree branch mappings
export async function sessionTreesExample(): Promise<void> {

    // Connect to the server. Change these options to suit your own environment.
    // Node.js does not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    const table: BranchMappingTable = newBranchMappingTableBuilder()
        .addBranchMapping("$Principal is 'control'", "target/1")
        .addBranchMapping("all", "target/2")
        .create("topic/path");

    await session.sessionTrees.putBranchMappingTable(table);

    console.log(`Branch mapping table for session tree branch ${table.getSessionTreeBranch()} has been added`);

    const listSessionTreeBranches: string[] = await session.sessionTrees.getSessionTreeBranchesWithMappings();

    console.log(`Session tree branches with mappings obtained: ${listSessionTreeBranches.join(', ')}`);

    for (const sessionTreeBranch of listSessionTreeBranches)
    {
        const branchMappingTable: BranchMappingTable = await session.sessionTrees.getBranchMappingTable(sessionTreeBranch);

        for (const branchMapping of branchMappingTable.getBranchMappings())
        {
            console.log(`Session tree branch: '${sessionTreeBranch}', Session filter: '${branchMapping.sessionFilter}', Topic tree branch: '${branchMapping.topicTreeBranch}'`);
        }
    }
}
