/*******************************************************************************
 * Copyright (C) 2014, 2018 Push Technology Ltd.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddTopicResult;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.RemovalCallback;
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
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlClientAddingAndRemovingTopics {

    private final Session session;

    private final TopicControl topicControl;

    /**
     * Constructor.
     */
    public ControlClientAddingAndRemovingTopics() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

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
        final AddTopicResult result =
            topicControl.addTopic(topicPath, topicType).get(5, TimeUnit.SECONDS);
        return result == AddTopicResult.CREATED;
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
        topicControl.addTopic(
            topicPath, specification).get(5, TimeUnit.SECONDS);
    }

    /**
     * This shows the action of adding a slave topic which must be added with a
     * specification because it requires the master topic property to be set.
     *
     * @param topicPath the path of the slave topic
     * @param masterTopicPath the path of the master topic
     * @throws ExecutionException if adding the topic fails
     * @throws TimeoutException if operation does not complete within 5 seconds
     * @throws InterruptedException if the current thread is interrupted whilst
     *         waiting for the result
     * @since 6.0
     */
    public void addSlaveTopic(String topicPath, String masterTopicPath)
        throws InterruptedException, ExecutionException, TimeoutException {
        topicControl.addTopic(
            topicPath,
            topicControl.newSpecification(TopicType.SLAVE).withProperty(
                TopicSpecification.SLAVE_MASTER_TOPIC, masterTopicPath))
            .get(5, TimeUnit.SECONDS);
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
            topicControl.newSpecification(topicType).withProperty(
                TopicSpecification.REMOVAL,
                "When no session has '$SessionId is \"" +
                session.getSessionId().toString() +
                "\"' remove '" +
                "?" + topicPath + "//'");
        final AddTopicResult result =
            topicControl.addTopic(topicPath, specification).get(5, TimeUnit.SECONDS);
        return result == AddTopicResult.CREATED;
    }

    /**
     * Remove a single topic given its path.
     *
     * @param topicPath the topic path
     * @param callback notifies result of operation
     */
    public void removeTopic(String topicPath, RemovalCallback callback) {
        topicControl.remove(topicPath, callback);
    }

    /**
     * Remove a topic and all of its descendants.
     *
     * @param topicPath the topic path
     * @param callback notifies result of operation
     */
    public void removeTopicBranch(String topicPath, RemovalCallback callback) {
        topicControl.remove("?" + topicPath + "//", callback);
    }

    /**
     * Remove one or more topics using a topic selector expression.
     *
     * @param topicSelector the selector expression
     * @param callback notifies result of operation
     */
    public void removeTopics(String topicSelector, RemovalCallback callback) {
        topicControl.remove(topicSelector, callback);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
