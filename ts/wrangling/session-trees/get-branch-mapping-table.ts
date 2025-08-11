/*******************************************************************************
 * Copyright (C) 2024 Diffusion Data Ltd.
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

import { connect, newBranchMappingTableBuilder, Session } from 'diffusion';
/// tag::log
import { PartiallyOrderedCheckpointTester } from '../../../../test/util';
/// end::log

export async function sessionTreesGetBranchMappingTable(): Promise<void> {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        'my/personal/path',
        '$Principal is "admin": my/topic/path/for/admin',
        '$Principal is "control": my/topic/path/for/control',
        '$Principal is "": my/topic/path/for/anonymous',
        'my/alternate/path',
        '$Transport is "WEBSOCKET": my/alternate/path/for/websocket',
        '$Transport is "HTTP_LONG_POLL": my/alternate/path/for/http',
        '$Transport is "TCP": my/alternate/path/for/tcp'
    ]]);
    /// end::log
    /// tag::session_trees_get_branch_mapping_table[]
    // Connect to the server.
    const session = await connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const branchMappingTable1 = newBranchMappingTableBuilder()
        .addBranchMapping('$Principal is "admin"', 'my/topic/path/for/admin')
        .addBranchMapping('$Principal is "control"', 'my/topic/path/for/control')
        .addBranchMapping('$Principal is ""', 'my/topic/path/for/anonymous')
        .create('my/personal/path');
    await session.sessionTrees.putBranchMappingTable(branchMappingTable1);

    const branchMappingTable2 = newBranchMappingTableBuilder()
        .addBranchMapping('$Transport is "WEBSOCKET"', 'my/alternate/path/for/websocket')
        .addBranchMapping('$Transport is "HTTP_LONG_POLL"', 'my/alternate/path/for/http')
        .addBranchMapping('$Transport is "TCP"', 'my/alternate/path/for/tcp')
        .create('my/alternate/path');
    await session.sessionTrees.putBranchMappingTable(branchMappingTable2);

    const sessionTrees = await session.sessionTrees.getSessionTreeBranchesWithMappings();
    for (const sessionTreeBranch of sessionTrees) {
        console.log(`${sessionTreeBranch}:`);
        /// tag::log
        check.log(sessionTreeBranch);
        /// end::log
        const mappingTable = await session.sessionTrees.getBranchMappingTable(sessionTreeBranch);
        for (const branchMapping of mappingTable.getBranchMappings()) {
            console.log(`    ${branchMapping.sessionFilter}: ${branchMapping.topicTreeBranch}`);
            /// tag::log
            check.log(`${branchMapping.sessionFilter}: ${branchMapping.topicTreeBranch}`);
            /// end::log
        }
    }

    await session.closeSession();
    /// end::session_trees_get_branch_mapping_table[]
    /// tag::log
    await check.done();
    /// end::log
}
