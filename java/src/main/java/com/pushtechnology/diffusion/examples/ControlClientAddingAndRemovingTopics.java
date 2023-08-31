/*******************************************************************************
 * Copyright (C) 2014, 2023 DiffusionData Ltd.
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

import static com.pushtechnology.diffusion.client.Diffusion.newTopicSpecification;
import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.REMOVAL;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddTopicResult;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * An example of using a control client to add topics.
 * <P>
 * This uses the 'TopicControl' feature only.
 * <P>
 * To add or remove topics, the client session must have the 'modify_topic'
 * permission for that branch of the topic tree.
 *
 * @author DiffusionData Limited
 * @since 5.0
 */
public class ControlClientAddingAndRemovingTopics {

    private final Session session;

    private final TopicControl topicControl;

    /**
     * Constructor.
     */
    public ControlClientAddingAndRemovingTopics(String serverUrl) {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        topicControl = session.feature(TopicControl.class);

    }

    /**
     * This shows the simplest method for adding a topic.
     * <P>
     * This caters for adding most topic types with their default properties.
     *
     * @param topicPath the topic path
     * @param topicType the topic type
     * @return true if the topic was added or false if it already existed
     * @throws ExecutionException if adding the topic fails
     * @throws TimeoutException if operation does not complete within 5 seconds
     * @throws InterruptedException if the current thread is interrupted whilst
     *         waiting for the result
     * @since 6.0
     */
    public boolean addTopic(String topicPath, TopicType topicType)
        throws ExecutionException, TimeoutException, InterruptedException {

        return topicControl.addTopic(topicPath, topicType).get(5, SECONDS) ==
            AddTopicResult.CREATED;
    }

    /**
     * This shows the action of adding a topic with a specification instead of
     * simply a type.
     * <P>
     * This mechanism is suitable for adding most types of topic when something
     * other than the default topic properties are required.
     *
     * @param topicPath the topic path
     * @param specification the topic specification
     * @throws ExecutionException if adding the topic fails
     * @throws TimeoutException if operation does not complete within 5 seconds
     * @throws InterruptedException if the current thread is interrupted whilst
     *         waiting for the result
     * @since 6.0
     */
    public void addTopic(String topicPath, TopicSpecification specification)
        throws InterruptedException, ExecutionException, TimeoutException {
        topicControl.addTopic(topicPath, specification).get(5, SECONDS);
    }

    /**
     * This shows how to create a topic that will be automatically removed,
     * along with all of its descendants, when the session closes.
     *
     * @param topicPath the topic path
     * @param topicType the topic type
     * @return true if the topic was added or false if it already existed
     * @throws ExecutionException if adding the topic fails
     * @throws TimeoutException if operation does not complete within 5 seconds
     * @throws InterruptedException if the current thread is interrupted whilst
     *         waiting for the result
     * @since 6.1
     */
    public boolean addSessionTopic(String topicPath, TopicType topicType)
        throws ExecutionException, TimeoutException, InterruptedException {

        final TopicSpecification specification =
            newTopicSpecification(topicType).withProperty(
                REMOVAL,
                "when this session closes remove '?" + topicPath + "//'");

        return
            topicControl.addTopic(topicPath, specification).get(5, SECONDS) ==
                AddTopicResult.CREATED;
    }

    /**
     * Remove a single topic given its path.
     *
     * @param topicPath the topic path
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<Integer> removeTopic(String topicPath) {
        return removeTopics(">" + topicPath);
    }

    /**
     * Remove a topic and all of its descendants.
     *
     * @param topicPath the topic path
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<Integer> removeTopicBranch(String topicPath) {
        return removeTopics("?" + topicPath + "//");
    }

    /**
     * Remove one or more topics using a topic selector expression.
     *
     * @param topicSelector the selector expression
     * @return a CompletableFuture that completes when a response is received
     *         from the server
     */
    public CompletableFuture<Integer> removeTopics(String topicSelector) {
        return topicControl.removeTopics(topicSelector)
            .thenApply(TopicControl.TopicRemovalResult::getRemovedCount);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
