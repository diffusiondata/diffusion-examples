/*******************************************************************************
 * Copyright (C) 2017, 2019, 2026 DiffusionData Limited.
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

package com.diffusiondata.android.example;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionAttributes;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.CompletableFuture;

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
public class PubSubExample extends AppCompatActivity {

    private static final String LOG_TAG = "diffusion";

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startSession();
    }


    private void display(String message) {
        display(message, null);
    }

    private void display(String message, Throwable ex) {
        if (ex != null) {
            Log.e(LOG_TAG, message, ex);
        } else {
            Log.i(LOG_TAG, message);
        }
        runOnUiThread(() -> {
            TextView valueView = findViewById(R.id.value);
            ScrollView scrollView = findViewById(R.id.scrollView);
            valueView.append(message + "\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    private void startSession() {
        final SessionFactory sessionFactory = Diffusion.sessions()
                .principal("admin")
                .password("password")
                // Default IP of the host machine from the Android emulator
                .serverHost("10.0.2.2")
                .serverPort(8080);

        final CompletableFuture<Session> future = sessionFactory.openAsync();

        future.whenComplete((session, ex) -> {
            if (ex != null) {
                display("Failed to connect to Diffusion server, will retry.", ex);
                executor.schedule(this::startSession, 10, TimeUnit.SECONDS);
            }
            else {
                display("Connected to Diffusion server.");
                example(session);
            }
        });
    }

    private void example(Session session) {
        final Topics topics = session.feature(Topics.class);

        topics.subscribe("counter");

        session.feature(TopicControl.class).addTopic("counter", TopicType.INT64)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        display("Failed to create topic", ex);
                    }
                    else {
                        display("Created topic 'counter' with result: " + result);
                    }
                });

        final AtomicLong i = new AtomicLong(0);

        executor.scheduleWithFixedDelay(
                () -> topics.set("counter", Long.class, i.getAndIncrement()),
                1, 1, TimeUnit.SECONDS);

        topics.addStream("counter", Long.class, new Topics.ValueStream.Default<Long>() {
            @Override
            public void onSubscription(String topicPath, TopicSpecification specification) {
                display("Subscribed to topic path: " + topicPath);
            }

            @Override
            public void onValue(
                    String topicPath,
                    TopicSpecification specification,
                    Long oldValue,
                    Long newValue) {
                display(String.format(Locale.getDefault(),
                        "Received value on '%s': %d", topicPath, newValue));
            }

        });
    }
}