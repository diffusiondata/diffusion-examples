/*******************************************************************************
 * Copyright (C) 2014, 2017 Push Technology Ltd.
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
import com.pushtechnology.diffusion.client.session.Session;
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

    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientConnectingAsynchronously.class);

    /**
     * Constructor.
     * @param topicPath the path of the topic to create
     */
    public ControlClientConnectingAsynchronously(String topicPath) {
        Diffusion.sessions().principal("control").password("password")
            .open("ws://diffusion.example.com:80", new TopicAdder(topicPath));

    }

    private final class TopicAdder implements SessionFactory.OpenCallback {

        private final String topicToAdd;

        /**
         * Constructor.
         * @param topicPath
         */
        private TopicAdder(String topicPath) {
            super();
            topicToAdd = topicPath;
        }

        @Override
        public void onError(ErrorReason errorReason) {
            LOG.error("Unable to connect a session" + errorReason);
        }

        @Override
        public void onOpened(final Session session) {
            try {
                session.feature(TopicControl.class).addTopic(
                    topicToAdd,
                    TopicType.STRING).get(10, TimeUnit.SECONDS);
            }
            catch (ExecutionException ex) {
                LOG.error("Adding topic failed", ex.getCause());
            }
            catch (InterruptedException | TimeoutException ex) {
                LOG.error("Adding topic failed", ex);
            }
            session.close();
        }

    }

}
