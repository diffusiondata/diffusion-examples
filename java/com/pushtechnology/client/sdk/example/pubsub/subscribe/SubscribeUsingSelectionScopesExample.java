/*******************************************************************************
 * Copyright (C) 2025 DiffusionData Ltd.
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
package com.pushtechnology.client.sdk.example.pubsub.subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * This example demonstrates subscribing to a topic in Diffusion using
 * selections scopes.
 * <P>
 * Multiple application components can use the same session to subscribe and unsubscribe
 * to topics using unique selection scopes which prevent unsubscriptions from
 * one component affecting the other.
 *
 * @author DiffusionData Limited
 */
public class SubscribeUsingSelectionScopesExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(SubscribeUsingSelectionScopesExample.class);

    public static void main(String[] args) throws Exception {

        try (Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080")) {

            final String topicPath = "my/topic/path";
            final String myOtherPath = "my/other/path";

            final TopicControl topicControl = session.feature(TopicControl.class);
            final SubscriptionControl subscriptionControl = session.feature(SubscriptionControl.class);

            topicControl.addTopic(topicPath, TopicType.STRING).join();
            topicControl.addTopic(myOtherPath, TopicType.STRING).join();

            final Topics componentA = session.feature(Topics.class);
            final Topics componentB = session.feature(Topics.class);

            final MyStream streamA = new MyStream("componentA");
            final MyStream streamB = new MyStream("componentB");

            // each component registers a stream for the topics they are interested in
            // and subscribe to the topics with their own scope

            componentA.addStream(topicPath, String.class, streamA);
            componentB.addStream("?my//", String.class, streamB);

            componentA.subscribe(topicPath, "scopeA").join();
            componentB.subscribe(topicPath, "scopeB").join();
            componentB.subscribe(myOtherPath, "scopeB").join();

            showSelectionScopes(session);

            // componentB unsubscribes from topicPath, scopeA is unaffected
            componentB.unsubscribe(topicPath, "scopeB").join();

            showSelectionScopes(session);

            // componentA unsubscribes from all topics, scopeB is unaffected
            // topicPath is no longer in any scopes and is unsubscribed for the session
            componentA.unsubscribe("?.*//", "scopeA").join();

            // componentA removes its stream and will no longer receive updates
            componentA.removeStream(streamA);

            // a control client unsubscribes all remaining scopes for myOtherPath which
            // is now unsubscribed for the session
            subscriptionControl.unsubscribeAllScopes(session.getSessionId(), myOtherPath).join();

            componentB.removeStream(streamB);
        }
    }

    public static class MyStream implements Topics.ValueStream<String> {

        private final String scopeName;

        public MyStream(String scopeName) {
            this.scopeName = scopeName;
        }

        @Override
        public void onValue(
            String topicPath,
            TopicSpecification topicSpecification,
            String oldValue,
            String newValue) {

            LOG.info("{}: '{}' changed from '{}' to '{}'.",
                scopeName, topicPath, oldValue, newValue);
        }

        @Override
        public void onSubscription(
            String topicPath,
            TopicSpecification topicSpecification) {

            LOG.info("{}: Subscribed to: '{}'.", scopeName, topicPath);
        }

        @Override
        public void onUnsubscription(
            String topicPath,
            TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {

            LOG.info("{}: Unsubscribed from: '{}', reason: {}.",
                scopeName, topicPath, unsubscribeReason);
        }

        @Override
        public void onClose() {
            LOG.info("{}: stream closed.", scopeName);
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("{}: On error: {}.", scopeName, errorReason);
        }
    }

    /**
     * Fetch and log selection scopes for the given session.
     */
    private static void showSelectionScopes(Session session) {
        session.feature(SubscriptionControl.class)
            .getTopicSelections(session.getSessionId()).join()
            .forEach((scopeName, selections)
                -> LOG.info("{} scopes:{}", scopeName, selections));
    }
}
