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

package com.pushtechnology.demo.update;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    /**
     * A session handler that maintains the diffusion session.
     */
    private class SessionHandler implements SessionFactory.OpenCallback {
        private Session session = null;

        @Override
        public void onOpened(Session session) {
            this.session = session;


            // Create a JSON topic 'foo/counter'
            session.feature(TopicControl.class).addTopic(
                "foo/counter",
                TopicType.JSON,
                new TopicControl.AddCallback.Default());


            // Get the TopicUpdateControl feature and JSON data type
            final JSONDataType jsonDataType = Diffusion.dataTypes().json();
            final TopicUpdateControl updateControl = session
                    .feature(TopicUpdateControl.class);


            final AtomicInteger i = new AtomicInteger(0);

            // Schedule a recurring task that increments the counter and updates the "counter" topic with a json value
            // every second
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    // Create the json value
                    final JSON value = jsonDataType.fromJsonString(
                        String.format("{\"count\" : %d }", i.getAndIncrement()));
            
                    // Update the topic
                    updateControl.updater().update(
                        "counter",
                        value,
                        new TopicUpdateControl.Updater.UpdateCallback.Default());
                }
            }, 1000, 1000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onError(ErrorReason errorReason) {
            Log.e("Diffusion", "Failed to open session because: " + errorReason.toString());
            session = null;
        }

        public void close() {
            if (session != null) {
                session.close();
            }
        }
    }

    private SessionHandler sessionHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (sessionHandler == null) {
            sessionHandler = new SessionHandler();

            Diffusion.sessions()
                     .principal("username")
                     .password("password")
                     .open("ws://host:port", sessionHandler);
        }
    }

    @Override
    protected void onDestroy() {
        if (sessionHandler != null) {
            sessionHandler.close();
            sessionHandler = null;
        }

        super.onDestroy();
    }
}
