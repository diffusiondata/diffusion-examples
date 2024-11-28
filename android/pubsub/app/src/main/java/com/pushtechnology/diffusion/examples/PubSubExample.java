/*******************************************************************************
 * Copyright (C) 2017, 2019 Push Technology Ltd.
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java8.util.concurrent.CompletableFuture;

/**
 * Android example showing basic use of the Diffusion API.
 *
 * <p>
 * Start a Diffusion server and the Android emulator on the same machine, then
 * build and deploy this example to the emulator.
 *
 * @author DiffusionData Limited
 * @since 6.2
 */
public class PubSubExample extends Activity {

    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        startSession();
    }

    private void startSession() {

        // Configure a session factory that connects to Diffusion using the "admin" account.
        // The principal name, password, and server port match the sample configuration
        // in the standard Diffusion installation.
        // The 10.0.2.2 IP address provided as the server host is the address of the machine
        // running the Diffusion Android emulator.

        final SessionFactory sessionFactory = Diffusion.sessions()
                .principal("admin")
                .password("password")
                .serverHost("10.0.2.2")
                .serverPort(8080);

        // For compatibility with Android SDK's earlier than API level 24, Diffusion uses a backport
        // of JDK 8 classes such as CompletableFuture. Note the java8 package name in the import
        // statement above.
        final CompletableFuture<Session> future =
            // Use the asynchronous openAsync method() to connect in a background thread.
            // Android prohibits blocking network operations in the main UI thread.
            sessionFactory.openAsync();

        future.whenComplete((session, ex) -> {
            if (ex != null) {
                Log.e("diffusion", "Failed to connect to Diffusion server, is it running? Will retry.", ex);
                executor.schedule(this::startSession, 10, TimeUnit.SECONDS);
            } else {
                example(session);
            }
        });
    }

    private void example(Session session) {

        final Topics topics = session.feature(Topics.class);

        // Ask the server to subscribe to a topic called "counter". It doesn't matter that this
        // is done before a value stream is added, nor that the topic may not yet exist. The
        // server remembers the session has selected the counter topic, and re-evaluates
        // subscriptions as topics are added and removed.
        topics.subscribe("counter");

        // Add the int64 "counter" topic.
        session.feature(TopicControl.class).addTopic("counter", TopicType.INT64)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    // This can fail because there is an incompatible topic called "counter";
                    // if the session has insufficient permissions to create the topic; or if
                    // the session is closed.
                    Log.w("diffusion", "Failed to create topic", ex);
                }
                else {
                    // Result will be either AddTopicResult.CREATED or AddTopicResult.EXISTS,
                    // depending on whether the topic already exists on the server.
                    Log.i("diffusion", "Created topic with result " + result);
                }
            });

        final AtomicLong i = new AtomicLong(0);

        // Schedule a recurring task that increments the counter and updates the topic.
        executor.scheduleAtFixedRate(
            () -> topics.set("counter", Long.class, i.getAndIncrement()),
            1, 1, TimeUnit.SECONDS);

        /// Add a value stream to dispatch the topic events locally.
        topics.addStream("counter", Long.class, new Topics.ValueStream.Default<Long>() {
            @Override
            public void onSubscription(String topicPath, TopicSpecification specification) {
                Log.i("diffusion", "Subscribed to: " + topicPath);
            }

            @Override
            public void onValue(
                    String topicPath,
                    TopicSpecification specification,
                    Long oldValue,
                    Long newValue) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView valueView = (TextView)findViewById(R.id.value);
                        valueView.setText(String.format(Locale.getDefault(),
                            "Subscribed to 'counter' topic: %d", newValue));
                    }
                });
            }
        });
    }
}
