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

const diffusion = require('diffusion');
/// tag::log
const { PartiallyOrderedCheckpointTester } = require('../../../../test/util');
/// end::log

export async function sessionTreesPutAndRemoveBranchMappingTable() {
    /// tag::log
    const check = new PartiallyOrderedCheckpointTester([[
        '$Principal is "admin": my/topic/path/for/admin',
        '$Principal is "control": my/topic/path/for/control',
        '$Principal is "": my/topic/path/for/anonymous'
    ]]);
    /// end::log
    /// tag::session_trees_put_and_remove_branch_mapping_table[]
    // Connect to the server.
    const session = await diffusion.connect({
        host: 'localhost',
        port: 8080,
        principal: 'admin',
        credentials: 'password'
    });

    const branchMappingTable = diffusion.newBranchMappingTableBuilder()
        .addBranchMapping('$Principal is "admin"', 'my/topic/path/for/admin')
        .addBranchMapping('$Principal is "control"', 'my/topic/path/for/control')
        .addBranchMapping('$Principal is ""', 'my/topic/path/for/anonymous')
        .create('my/personal/path');
    await session.sessionTrees.putBranchMappingTable(branchMappingTable);
    /// tag::log
    const mappingTable = await session.sessionTrees.getBranchMappingTable('my/personal/path');
    for (const branchMapping of mappingTable.getBranchMappings()) {
        check.log(`${branchMapping.sessionFilter}: ${branchMapping.topicTreeBranch}`);
    }
    /// end::log

    const emptyBranchMappingTable = diffusion.newBranchMappingTableBuilder()
        .create('my/personal/path');
    await session.sessionTrees.putBranchMappingTable(emptyBranchMappingTable);
    /// tag::log
    const mappingTable2 = await session.sessionTrees.getBranchMappingTable('my/personal/path');
    expect(mappingTable2.getBranchMappings().length).toBe(0);
    /// end::log

    await session.closeSession();
    /// end::session_trees_put_and_remove_branch_mapping_table[]
    /// tag::log
    await check.done();
    /// end::log
}
