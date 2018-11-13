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

import java.util.concurrent.CompletableFuture;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddTopicResult;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * This is a simple example of a client that uses asynchronous connection to
 * connect, create a topic and then disconnect.
 *
 * @author Push Technology Limited
 * @since 5.3
 */
public final class ControlClientConnectingAsynchronously {

    private final SessionFactory sessionFactory =
        Diffusion.sessions().principal("control").password("password");

    /**
     * Create a session to the server, add a string topic, then close the
     * session.
     *
     * @param topicPath path of session to add
     * @return a CompletableFuture that completes when the topic has been added
     */
    public CompletableFuture<AddTopicResult> createTopic(String topicPath) {

        return sessionFactory
            .openAsync("ws://diffusion.example.com:80")
            .thenCompose(session -> {
                return session
                    .feature(TopicControl.class)
                    .addTopic(topicPath, TopicType.STRING)
                    .whenComplete((result, ex) -> session.close());
            });
    }
}
