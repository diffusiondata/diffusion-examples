/*******************************************************************************
 * Copyright (C) 2018, 2019 Push Technology Ltd.
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
import static com.pushtechnology.diffusion.client.topics.details.TopicType.JSON;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;

/**
 * This example control client provides a methods that allows a topic to be
 * created that will be automatically removed when the number of subscribers
 * falls to and remains at zero for 10 seconds.
 *
 * @author Push Technology Limited
 */
public final class ControlClientDeletesTopicsWithoutSubscribers {

    private final Session session;
    private final TopicControl topicControl;
    private final TopicSpecification specification;

    /**
     * Construct client that can add topics that will be automatically
     * removed.
     *
     * @param serverURL url of the server to connect to.
     */
    public ControlClientDeletesTopicsWithoutSubscribers(String serverURL) {

        session = Diffusion.sessions()
            .principal("control").password("password")
            .open(serverURL);

        topicControl = session.feature(TopicControl.class);

        specification =
            newTopicSpecification(JSON)
                .withProperty(REMOVAL, "when subscriptions < 1 for 10s");

    }

    /**
     * Creates a topic that will be automatically removed after it has had
     * no subscribers for at least 10 seconds.
     *
     * @param topicPath the topic path
     * @throws InterruptedException
     * @throws ExecutionException if the add topic request failed
     * @throws TimeoutException timed out waiting for response from add topic
     * request
     */
    public void addTopic(String topicPath)
        throws InterruptedException, ExecutionException, TimeoutException {
        topicControl.addTopic(topicPath, specification).get(10, SECONDS);
    }

    /**
     * Close session.
     */
    public void close() {
        session.close();
    }

}