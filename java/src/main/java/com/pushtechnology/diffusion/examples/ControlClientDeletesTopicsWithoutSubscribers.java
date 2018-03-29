/*******************************************************************************
 * Copyright (C) 2017 Push Technology Ltd.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import net.jcip.annotations.GuardedBy;

/**
 * This example control client deletes topic {@link #DEMO_TOPIC} when the number
 * of subscribers falls to and remains at zero for 10 seconds.
 * <P>
 * Manual interaction with {@link #DEMO_TOPIC} is required in order to trigger
 * the listener which detects the change in state caused by the interaction.
 * <P>
 * When the number of subscribers to the topic reaches 0, a {@code Runnable} is
 * scheduled which will delete the topic after 10 seconds and a ScheduledFuture
 * is stored in a map.
 * <P>
 * When the number of subscribers to {@link #DEMO_TOPIC} goes from 0 to >0 the
 * map is queried for a ScheduledFuture and if a ScheduledFuture exists for the
 * topic it is cancelled and removed from the map.
 * <P>
 * If nobody subscribes to the topic after it has been created, the
 * onNoSubscribers callback is not called and the topic is not deleted.
 *
 * @author Push Technology Limited
 */
public final class ControlClientDeletesTopicsWithoutSubscribers {

    private static final Logger LOG = LoggerFactory
        .getLogger(ControlClientDeletesTopicsWithoutSubscribers.class);

    private static final String DEMO_BRANCH = "DEMO";
    private static final String DEMO_TOPIC = DEMO_BRANCH + "/TOPIC";

    private static final int TIMEOUT = 10;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final Session session;
    private final TopicControl topicControl;

    // Maps a topic name to a scheduled future that will delete the topic
    @GuardedBy("this")
    private final Map<String, ScheduledFuture<?>> map = new HashMap<>();
    private final ScheduledExecutorService executorService =
        Executors.newScheduledThreadPool(1);

    ControlClientDeletesTopicsWithoutSubscribers()
        throws InterruptedException, IOException {

        session = Diffusion.sessions()
            .principal("control").password("password")
            .open("ws://localhost:8080");

        topicControl = session.feature(TopicControl.class);

        topicControl.addTopic(DEMO_TOPIC, TopicType.JSON);

        topicControl.addTopicEventListener(DEMO_BRANCH,
            new TopicControl.TopicEventListener.Default() {
                @Override
                public void onHasSubscribers(String topicPath) {
                    hasSubscribers(topicPath);
                }

                @Override
                public void onNoSubscribers(final String topicPath) {
                    noSubscribers(topicPath);
                }
            });

        LOG.info("Press enter to quit");
        System.in.read();
    }

    /**
     * When {@link #DEMO_TOPIC} goes from 0 to >0 subscribers, cancel any
     * scheduled removal of that topic.
     */
    private synchronized void hasSubscribers(String topicPath) {

        LOG.info("{} now has a subscriber", topicPath);

        final ScheduledFuture<?> future = map.get(topicPath);

        if (future != null) {
            LOG.info("Cancelling scheduled removal of {}", topicPath);
            future.cancel(true);
            map.remove(topicPath);
        }
    }

    /**
     * When {@link #DEMO_TOPIC} goes from >0 to 0 subscribers, schedule removal
     * of that topic.
     */
    private synchronized void noSubscribers(final String topicPath) {

        LOG.info("{} now has no subscribers", topicPath);
        LOG.info("Scheduling removal of {} in {} {}", topicPath, TIMEOUT,
            TIMEOUT_UNIT);

        final ScheduledFuture<?> future =
            executorService.schedule(
                (Runnable) () -> {
                    LOG.info("Removing {}", topicPath);
                    topicControl.remove(
                        topicPath,
                        new TopicControl.RemovalCallback.Default() {
                            @Override
                            public void onTopicsRemoved() {
                                synchronized (map) {
                                    map.remove(topicPath);
                                    LOG.info("{} removed", topicPath);
                                }
                            }
                        });
                },
                TIMEOUT,
                TIMEOUT_UNIT);

        map.put(topicPath, future);
    }
}