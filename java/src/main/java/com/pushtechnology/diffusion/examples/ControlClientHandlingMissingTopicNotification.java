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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.MissingTopicNotification;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.MissingTopicNotificationStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.TopicSelector.Type;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * An example of registering a missing topic notification handler and processing
 * notifications using a control client.
 *
 * @author DiffusionData Limited
 */
public final class ControlClientHandlingMissingTopicNotification {

    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientHandlingMissingTopicNotification.class);

    private final Session session;
    private final TopicControl topicControl;

    /**
     * Constructor.
     */
    public ControlClientHandlingMissingTopicNotification(String serverUrl)
        throws InterruptedException, ExecutionException, TimeoutException {
        // Create a session
        session = Diffusion.sessions().password("password").principal("admin")
            .open(serverUrl);

        topicControl = session.feature(TopicControl.class);

        // Registers a missing topic notification on a topic path
        topicControl.addMissingTopicHandler(
            "Accounts",
            new NotificationStream()).get(5, TimeUnit.SECONDS);

    }

    private final class NotificationStream implements
        MissingTopicNotificationStream {
        @Override
        public void onClose() {
        }

        @Override
        public void onError(ErrorReason errorReason) {
        }

        @Override
        public void onMissingTopic(MissingTopicNotification notification) {
            // This handler will create a missing topic if a path selector
            // requesting a topic starting with "Accounts/" is selected and
            // the requesting session has the principal 'control'.
            if (notification.getTopicSelector().getType() == Type.PATH) {
                final String path = notification.getTopicPath();
                if (path.startsWith("Accounts/") &&
                    "control".equals(
                        notification.getSessionProperties().get(Session.PRINCIPAL))) {

                    topicControl.addTopic(
                        path,
                        TopicType.STRING).whenComplete((result, ex) -> {
                            if (ex == null) {
                                LOG.info("Missing topic " + path + " " + result);
                            }
                            else {
                                LOG.warn("Failed to create missing topic " + path, ex);
                            }
                        });
                }
            }
        }
    }

}
