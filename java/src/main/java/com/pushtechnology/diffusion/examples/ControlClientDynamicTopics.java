/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicAddFailReason;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddContextCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.MissingTopicHandler;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.MissingTopicNotification;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * An example of using control client to create topics dynamically (i.e. when
 * topics that do not exist are requested).
 * <P>
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlClientDynamicTopics {

    private final Session session;

    /**
     * Constructor.
     */
    public ControlClientDynamicTopics() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        final TopicControl tc = session.feature(TopicControl.class);

        final AddTopicAndProceed proceedCallback = new AddTopicAndProceed();

        // Add a handler that, upon receiving subscriptions or fetches for any
        // topic under the 'topicroot', creates a topic. If the topic name
        // starts with SV, it creates a single value topic otherwise a
        // delegated topic.
        tc.addMissingTopicHandler(
            "topicroot",
            new MissingTopicHandler.Default() {
                @Override
                public void onMissingTopic(
                    final MissingTopicNotification request) {

                    final String topicPath = request.getTopicPath();

                    if (topicPath.startsWith("topicroot/SV")) {
                        tc.addTopic(
                            topicPath,
                            TopicType.SINGLE_VALUE,
                            request,
                            proceedCallback);
                    }
                    else {
                        tc.addTopic(
                            topicPath,
                            TopicType.DELEGATED,
                            request,
                            proceedCallback);
                    }
                }
            });
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

    /**
     * Proceed whatever the outcome of adding the topic. We rely on the server
     * to log the failure.
     */
    private static class AddTopicAndProceed
        implements AddContextCallback<MissingTopicNotification> {

        @Override
        public void onTopicAdded(
            final MissingTopicNotification notification,
            final String topic) {

            notification.proceed();
        }

        @Override
        public void onTopicAddFailed(
            final MissingTopicNotification notification,
            final String name,
            final TopicAddFailReason reason) {

            notification.proceed();
        }

        @Override
        public void onDiscard(
            final MissingTopicNotification notification) {

            notification.proceed();
        }
    }
}
