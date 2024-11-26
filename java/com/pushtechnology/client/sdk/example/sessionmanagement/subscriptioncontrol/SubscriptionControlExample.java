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
package com.pushtechnology.client.sdk.example.sessionmanagement.subscriptioncontrol;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.TopicUpdate;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl;
import com.pushtechnology.diffusion.client.features.control.clients.ClientControl.SessionEventParameters;
import com.pushtechnology.diffusion.client.features.control.topics.SubscriptionControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionId;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

public class SubscriptionControlExample {

    private static final Logger LOG =
        LoggerFactory.getLogger(SubscriptionControlExample.class);

    public static void main(String[] args) throws Exception {

        Session session = Diffusion.sessions()
            .principal("admin")
            .password("password")
            .open("ws://localhost:8080");

        ClientControl clientControl = session.feature(ClientControl.class);
        TopicUpdate topicUpdate = session.feature(TopicUpdate.class);

        topicUpdate.addAndSet(
            "my/topic/path/hello",
            Diffusion.newTopicSpecification(TopicType.STRING),
            String.class,
            "Hello World!");

        clientControl.addSessionEventListener(new MyEventStream(session),
            SessionEventParameters.DEFAULT);

        Session clientSession = Diffusion.sessions()
            .principal("client")
            .password("password")
            .open("ws://localhost:8080");

        clientSession.feature(Topics.class)
            .addFallbackStream(String.class, new MyFallbackStream());

        clientControl.setSessionProperties(
            clientSession.getSessionId(),
            Collections.singletonMap("$Location", "Canada")
        ).join();

        session.close();
        clientSession.close();

        LOG.info("created client subscription");
    }

    static class MyEventStream implements ClientControl.SessionEventStream {

        private final Session controlSession;
        private final SessionId callerId;

        MyEventStream(Session controlSession) {
            this.controlSession = controlSession;
            callerId = controlSession.getSessionId();
        }

        @Override
        public void onSessionEvent(Event event) {
            if (event.isOpenEvent()) {
                final SessionId sessionId = event.sessionId();

                // if this is not the calling session, subscribe and unsubscribe it
                if(!callerId.equals(sessionId)) {
                    controlSession.feature(SubscriptionControl.class)
                        .subscribe(sessionId, "?my/topic/path//");

                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    controlSession.feature(SubscriptionControl.class)
                        .unsubscribe(sessionId, "?my/topic/path//");
                }
            }
        }

        @Override
        public void onClose() {
            System.out.println("Stream closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            System.out.println("On error: " + errorReason.toString());
        }
    }

    static class MyFallbackStream implements Topics.ValueStream<String> {

        @Override
        public void onValue(String topicPath,
            TopicSpecification topicSpecification, String oldValue, String newValue) {
            System.out.printf("%s : %s\n", topicPath, newValue);
        }

        @Override
        public void onSubscription(String topicPath,
            TopicSpecification topicSpecification) {
            System.out.printf("Subscribed to %s\n", topicPath);
        }

        @Override
        public void onUnsubscription(String topicPath,
            TopicSpecification topicSpecification,
            Topics.UnsubscribeReason unsubscribeReason) {
            System.out.printf("Unsubscribed to %s: %s\n", topicPath, unsubscribeReason);
        }

        @Override
        public void onClose() {}

        @Override
        public void onError(ErrorReason errorReason) {}
    }
}
