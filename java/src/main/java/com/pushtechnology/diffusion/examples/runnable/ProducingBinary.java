/*******************************************************************************
 * Copyright (C) 2016, 2018 Push Technology Ltd.
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
package com.pushtechnology.diffusion.examples.runnable;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.binary.Binary;

/**
 * A client that creates and updates Binary topics.
 *
 * @author Push Technology Limited
 * @since 5.7
 */
public final class ProducingBinary extends AbstractClient {
    private static final ScheduledExecutorService EXECUTOR = Executors
        .newSingleThreadScheduledExecutor();
    private static final UpdateCallback.Default UPDATE_CALLBACK =
        new UpdateCallback.Default();

    private volatile Future<?> updateTask;

    /**
     * Constructor.
     * @param url The URL to connect to
     * @param principal The principal to connect as
     */
    public ProducingBinary(String url, String principal) {
        super(url, principal);
    }

    @Override
    public void onStarted(Session session) {
        final TopicControl topicControl = session.feature(TopicControl.class);

        final TopicSpecification specification =
            topicControl.newSpecification(TopicType.BINARY).withProperty(
                TopicSpecification.REMOVAL,
                "When no session has '$SessionId is \"" +
                session.getSessionId().toString() + "\"'");

        topicControl.addTopic("binary/random", specification);
    }

    @Override
    public void onConnected(Session session) {
        final TopicUpdateControl.ValueUpdater<Binary> updater = session
            .feature(TopicUpdateControl.class)
            .updater()
            .valueUpdater(Binary.class);

        updateTask = EXECUTOR.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    // Update the topic with random data
                    updater.update(
                        "binary/random",
                        RandomData.toBinary(RandomData.next()),
                        UPDATE_CALLBACK);
                }
            },
            0L,
            1L,
            SECONDS);
    }

    @Override
    public void onDisconnected() {
        // Cancel updates when disconnected
        updateTask.cancel(false);
    }

    /**
     * Entry point for the example.
     * @param args The command line arguments
     * @throws InterruptedException If the main thread was interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        final ProducingBinary client =
            new ProducingBinary("ws://diffusion.example.com:80", "auth");
        client.start("auth_secret");
        client.waitForStopped();
    }
}
