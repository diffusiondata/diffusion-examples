/*******************************************************************************
 * Copyright (C) 2018 Push Technology Ltd.
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

import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.OWNER;
import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.PERSISTENT;
import static com.pushtechnology.diffusion.client.topics.details.TopicSpecification.REMOVAL;
import static com.pushtechnology.diffusion.client.topics.details.TopicType.STRING;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * An example of using a control client to create topics using the various topic
 * lifecycle properties.
 * <p>
 * This also includes an example of how to create a branch of the topic tree
 * that is for the specific use of a named principal and will be automatically
 * removed when the principal no longer has a session.
 * <p>
 * For simplicity all topics created are {@link TopicType#STRING string} topics.
 * Also for simplicity all topic creations are blocking and there is no specific
 * error handling.
 *
 * @author Push Technology Limited
 * @since 6.1
 */
public final class ControlClientUsingTopicLifecycleProperties {

    private final Session session;
    private final TopicControl topicControl;
    private final TopicSpecification stringSpecification;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     */
    public ControlClientUsingTopicLifecycleProperties(String serverUrl)
        throws Exception {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open(serverUrl);

        topicControl = session.feature(TopicControl.class);

        stringSpecification = topicControl.newSpecification(STRING);

    }

    /**
     * Creates a topic that will be removed after a specified date and time.
     */
    public void createTopicRemovedAfter(String topicPath, Date dateAndTime)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when time after " + dateAndTime.getTime()))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will be removed after a a time specified in RFC_1123
     * format (e.g. "Sun, 3 Jun 2018 11:05:30 GMT"). The topic itself and all
     * descendants of the topic will be removed after the specified time.
     */
    public void createTopicRemovedAfter(String topicPath, String dateAndTime)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when time after '" + dateAndTime + "' remove '*" +
                        topicPath + "//'"))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will be removed when it has had no subscriptions for
     * 5 minutes after an initial period of 1 hour.
     */
    public void createTopicRemovedWhenNoSubscriptions(String topicPath)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when subscriptions < 1 for 5m after 1h"))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will be removed when it has had no updates for 5
     * minutes after an initial period of 1 hour.
     */
    public void createTopicRemovedWhenNoUpdates(String topicPath)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when no updates for 5m after 1h"))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will be removed when the session creating it closes.
     */
    public void createTopicRemovedWhenSessionCloses(String topicPath)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when this session closes"))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will be removed when there are have been no sessions
     * authenticated with the given principal for 30 minutes.
     */
    public void createTopicRemovedWhenNoPrincipal(
        String topicPath,
        String principal)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when no session has '$Principal is \"" +
                        principal +
                        "\"' for 30m"))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will be removed when there are have been no sessions
     * with the 'Department' session property set to 'Accounts' for 1 hour but
     * only checking after 1 day has elapsed.
     */
    public void createTopicRemovedWhenNoAccountsSession(
        String topicPath,
        String principal)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when no session has 'Department is \"Accounts\"' for 1h after 1d"))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will be removed when a specified date and time (in
     * RFC_1123 format) has passed AND either the topic has had no subscriptions
     * for a continuous period of 10 minutes after that time plus one hour OR it
     * has had no updates for a continuous period of 20 minutes.
     */
    public void createTopicRemovedWhenTimeAfterAndNoSubscriptionsOrUpdates(
        String topicPath,
        String dateAndTime)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when time after '" + dateAndTime +
                        "' and (subscriptions < 1 for 10m after 1h or no updates for 20m) "))
            .get(5, SECONDS);
    }

    /**
     * Creates a topic that will not be persisted if persistence is in use at
     * the server and will be removed after a specified date and time.
     */
    public void createNonPersistentTopicRemovedAfter(
        String topicPath,
        Date dateAndTime)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(
                    REMOVAL,
                    "when time after " + dateAndTime.getTime())
                .withProperty(PERSISTENT, "false"))
            .get(5, SECONDS);
    }

    /**
     * Creates a branch of the topic tree that the given principal will have
     * READ_TOPIC, MODIFY_TOPIC, and UPDATE_TOPIC permissions to and will be
     * automatically removed when there have been no sessions authenticated with
     * the principal for a period of 5 minutes.
     * <p>
     * The names of the topics to be created within the branch are supplied. The
     * principal will have permissions to all of the topics in the branch and
     * all of the topics in the branch will be removed when no sessions for the
     * principal remain.
     * <p>
     * None of the topics created will be persisted.
     */
    public void createPrivateTopicBranch(
        String topicPath,
        String principal,
        String... subTopics)
        throws IllegalArgumentException, InterruptedException,
        ExecutionException, TimeoutException {

        topicControl.addTopic(
            topicPath,
            stringSpecification
                .withProperty(OWNER, "$Principal is '" + principal + "'")
                .withProperty(
                    REMOVAL,
                    "when no session has '$Principal is \"" + principal +
                        "\"' for 5m remove '*" + topicPath + "//'")
                .withProperty(PERSISTENT, "false"))
            .get(5, SECONDS);

        for (String subTopic : subTopics) {
            topicControl.addTopic(
                topicPath + "/" + subTopic,
                stringSpecification
                    .withProperty(OWNER, "$Principal is '" + principal + "'")
                    .withProperty(PERSISTENT, "false"))
                .get(5, SECONDS);
        }
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
