/*******************************************************************************
 * Copyright (C) 2026 DiffusionData Ltd.
 * <P>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <P>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pushtechnology.client.sdk.example.pubsub.remove;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * This example demonstrates how to configure automatic topic removal using the
 * <code>remove branch</code> clause in Diffusion.
 * <p>
 * The root of a branch is created with a time based removal criteria qualified
 * by <code>remove branch</code>, and further topics are created below it. The
 * topics below the root need no removal criteria of their own; without the
 * clause only the root topic would be removed when the criteria is met, but
 * with it the root topic and every topic below it are removed together.
 * <p>
 * The topics in the branch are counted with a fetch request before the branch
 * is created, once it has been created, and again after the removal time has
 * passed, to show the whole branch being removed.
 *
 * @author DiffusionData Limited
 */
public class RemoveMultipleTopicsWithAutomaticTopicRemovalRemoveBranchExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(RemoveMultipleTopicsWithAutomaticTopicRemovalRemoveBranchExample.class);

    public static void main(String[] args) throws Exception {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final TopicControl topicControl = session.feature(TopicControl.class);
            final Topics topics = session.feature(Topics.class);

            final String branchRoot = "my/topic/branch/to/be/removed";

            // Selects the root of the branch and every topic below it
            final String branchSelector = "?" + branchRoot + "//";

            LOG.info("Topics in the branch before it is created: {}",
                countTopicsInBranch(topics, branchSelector));

            // The removal criteria is met five seconds from now, leaving ample
            // time to create the topics below the root of the branch
            final long removalTime = System.currentTimeMillis() + 5_000;

            // 'remove branch' extends the removal criteria to the whole branch,
            // so that the root topic and every topic below it are automatically
            // removed
            final TopicSpecification branchRootSpecification =
                Diffusion.newTopicSpecification(TopicType.JSON)
                    .withProperty(TopicSpecification.REMOVAL,
                        "when time after " + removalTime + " remove branch");

            LOG.info("Adding {}: {}", branchRoot,
                topicControl.addTopic(branchRoot, branchRootSpecification)
                    .join());

            final TopicSpecification specification =
                Diffusion.newTopicSpecification(TopicType.JSON);

            final String[] pathsBelowBranchRoot = {
                branchRoot + "/a",
                branchRoot + "/b",
                branchRoot + "/b/c"
            };

            for (final String topicPath : pathsBelowBranchRoot) {
                LOG.info("Adding {}: {}", topicPath,
                    topicControl.addTopic(topicPath, specification)
                        .join());
            }

            LOG.info("Topics in the branch once it has been created: {}",
                countTopicsInBranch(topics, branchSelector));

            final long giveUpTime = removalTime + 15_000;

            int topicsInBranch = countTopicsInBranch(topics, branchSelector);

            while (topicsInBranch > 0 &&
                System.currentTimeMillis() < giveUpTime) {

                SECONDS.sleep(1);

                topicsInBranch = countTopicsInBranch(topics, branchSelector);
            }

            LOG.info("Topics in the branch after the removal time has passed: {}",
                topicsInBranch);
        }
    }

    /**
     * Counts the topics in the branch, using a fetch request for the root of
     * the branch and every topic below it.
     */
    private static int countTopicsInBranch(
        Topics topics,
        String branchSelector) {

        return topics.fetchRequest()
            .fetch(branchSelector)
            .join()
            .size();
    }
}
