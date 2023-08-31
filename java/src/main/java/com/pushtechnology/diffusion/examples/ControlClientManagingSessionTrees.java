/*******************************************************************************
 * Copyright (C) 2021, 2023 DiffusionData Ltd.
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
package com.pushtechnology.diffusion.examples;

import static com.pushtechnology.diffusion.client.Diffusion.newBranchMappingTableBuilder;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.SessionTrees;
import com.pushtechnology.diffusion.client.features.control.topics.SessionTrees.BranchMappingTable;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * An example of using session trees.
 * <p>
 * This demonstrates creating, retrieving, and removing a branch mapping table
 * for the {@code market/prices} topic tree branch.
 *
 * @author DiffusionData Limited
 * @since 6.7
 */
public final class ControlClientManagingSessionTrees {

    private final Session session;

    /**
     * Constructor.
     * <p>
     * A session is connected to the supplied url.
     *
     * @param serverUrl url for server (or any server in a cluster)
     */
    public ControlClientManagingSessionTrees(String serverUrl) {
        session = Diffusion.sessions().principal("admin").password("password")
            .open(serverUrl);
    }

    /**
     * Creates (or replaces) a branch mapping table for the
     * {@code market/prices} branch.
     * <p>
     * In this example a session subscribing to topic path market/prices/X would
     * map to actual topics as follows:-
     * <ul>
     * <li>User tier 1 or Germany &ndash; {@code backend/discounted_prices/X}
     * <li>User tier 2 &ndash; {@code backend/standard_prices/X}
     * <li>Anonyomous user &ndash; {@code backend/delayed_prices/X}
     * <li>All others &ndash; {@code market/prices/X}
     * </ul>
     * The mappings are evaluated in order.
     * <p>
     * A subscription only completes if there is actually a topic at the derived
     * path.
     * <p>
     * No matter what topic the session actually gets subscribed to it will see
     * it as {@code market/prices/X}.
     */
    public void createBranchMappingTable()
        throws InterruptedException, ExecutionException, TimeoutException {

        final BranchMappingTable branchMappingTable =
            newBranchMappingTableBuilder()
                .addBranchMapping(
                    "USER_TIER is '1' or $Country is 'DE'",
                    "backend/discounted_prices")
                .addBranchMapping(
                    "USER_TIER is '2'",
                    "backend/standard_prices")
                .addBranchMapping(
                    "$Principal is ''",
                    "backend/delayed_prices")
                .create("market/prices");

        session.feature(SessionTrees.class)
            .putBranchMappingTable(branchMappingTable).get(5, SECONDS);
    }

    /**
     * Returns the {@code market/prices} branch mapping table.
     */
    public BranchMappingTable getBranchMappingTable()
        throws InterruptedException, ExecutionException, TimeoutException {

        return session.feature(SessionTrees.class)
            .getBranchMappingTable("market/prices").get(5, SECONDS);
    }

    /**
     * Removes the {@code market/prices} branch mapping table.
     */
    public void removeBranchMappingTable()
        throws InterruptedException, ExecutionException, TimeoutException {

        // Removal is achieved by putting an empty table
        final BranchMappingTable branchMappingTable =
            newBranchMappingTableBuilder().create("market/prices");

        session.feature(SessionTrees.class)
            .putBranchMappingTable(branchMappingTable).get(5, SECONDS);
    }

    /**
     * Closes the session.
     */
    public void close() {
        session.close();
    }

}
