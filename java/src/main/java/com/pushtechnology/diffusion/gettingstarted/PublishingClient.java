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
package com.pushtechnology.diffusion.gettingstarted;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

import java.util.concurrent.CountDownLatch;

/**
 * A client that publishes an incrementing count to the JSON topic 'foo/counter'.
 *
 * @author Push Technology Limited
 * @since 5.9
 */
public final class PublishingClient {
	/**
     * Main.
     */
    public static void main(String... arguments) throws InterruptedException {

        // Connect using a principal with 'modify_topic' and 'update_topic'
        // permissions
    	// Change 'host' to the hostname/address of your Diffusion server
        final  Session session = Diffusion.sessions().principal("control")
                .password("password").open("ws://host:8080");

        // Get the TopicControl and TopicUpdateControl feature
        final TopicControl topicControl = session.feature(TopicControl.class);

        final TopicUpdateControl updateControl =
            session.feature(TopicUpdateControl.class);
        
        final JSONDataType jsonDataType = Diffusion.dataTypes().json();

        final CountDownLatch waitForStart = new CountDownLatch(1);
        
        // Create a JSON topic 'foo/counter'
        topicControl.addTopic(
            "foo/counter",
            TopicType.JSON,
            new AddCallback.Default() {
                @Override
                public void onTopicAdded(String topicPath) {
                    waitForStart.countDown();
                }
            });

        // Wait for the onTopicAdded() callback.
        waitForStart.await();

        // Update the topic
        final UpdateCallback updateCallback = new UpdateCallback.Default();
        for (int i = 0; i < 1000; ++i) {
        	final JSON value = jsonDataType.fromJsonString(String.format("{\"count\" : %d }", i));
            // Use the non-exclusive updater to update the topic without locking it
            updateControl.updater().update(
                "foo/counter",
                value,
                updateCallback);

            Thread.sleep(1000);
        }
    }
}