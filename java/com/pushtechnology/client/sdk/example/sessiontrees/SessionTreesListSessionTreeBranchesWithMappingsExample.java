/*******************************************************************************
 * Copyright (C) 2023 - 2024 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.sessiontrees;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.SessionTrees;
import com.pushtechnology.diffusion.client.session.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionTreesListSessionTreeBranchesWithMappingsExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(SessionTreesListSessionTreeBranchesWithMappingsExample.class);

    public static void main(String[] args) {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final SessionTrees sessionTrees = session.feature(SessionTrees.class);

            final SessionTrees.BranchMappingTable branchMappingTable = Diffusion.newBranchMappingTableBuilder()
                .addBranchMapping("$Principal is 'admin'", "my/topic/path/for/admin")
                .addBranchMapping("$Principal is 'control'", "my/topic/path/for/control")
                .addBranchMapping("$Principal is ''", "my/topic/path/for/anonymous")
                .create("my/personal/path");

            sessionTrees.putBranchMappingTable(branchMappingTable)
                .join();

            final SessionTrees.BranchMappingTable branchMappingTable2 = Diffusion.newBranchMappingTableBuilder()
                .addBranchMapping("$Transport is 'WEBSOCKET'", "my/alternate/path/for/websocket")
                .addBranchMapping("$Transport is 'HTTP_LONG_POLL'", "my/alternate/path/for/http")
                .addBranchMapping("$Transport is 'TCP'", "my/alternate/path/for/tcp")
                .create("my/alternate/path");

            sessionTrees.putBranchMappingTable(branchMappingTable2)
                .join();

            LOG.info("Session tree mappings added.");

            sessionTrees.listSessionTreeBranchesWithMappings()
                .join()
                .forEach(sessionTreeBranch -> LOG.info("{}:", sessionTreeBranch));
        }
    }
}
