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
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.RemoveCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * An example of using a control client to create and update a topic in non
 * exclusive mode (as opposed to acting as an exclusive update source). In this
 * mode other clients could update the same topic (on a last update wins basis).
 * <P>
 * This uses the 'TopicControl' feature to create a topic and the
 * 'TopicUpdateControl' feature to send updates to it.
 * <P>
 * To send updates to a topic, the client session requires the 'update_topic'
 * permission for that branch of the topic tree.
 *
 * @author Push Technology Limited
 * @since 5.3
 */
public final class ControlClientUpdatingSingleValueTopic {

    private static final String TOPIC = "MyTopic";

    private final Session session;
    private final TopicControl topicControl;
    private final TopicUpdateControl updateControl;

    /**
     * Constructor.
     */
    public ControlClientUpdatingSingleValueTopic() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        topicControl = session.feature(TopicControl.class);
        updateControl = session.feature(TopicUpdateControl.class);

        // Create a single value topic
        topicControl.addTopic(
            TOPIC,
            TopicType.SINGLE_VALUE,
            new AddCallback.Default());

    }

    /**
     * Update the topic with a string value.
     *
     * @param value the update value
     * @param callback the update callback
     */
    public void update(String value, UpdateCallback callback) {
        updateControl.updater().update(TOPIC, value, callback);
    }

    /**
     * Close the session.
     */
    public void close() {
        // Remove our topic and close session when done
        topicControl.removeTopics(
            ">" + TOPIC,
            new RemoveCallback() {
                @Override
                public void onDiscard() {
                    session.close();
                }

                @Override
                public void onTopicsRemoved() {
                    session.close();
                }
            });

    }
}
