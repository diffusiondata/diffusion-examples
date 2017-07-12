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

package com.pushtechnology.demo.subscribe;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.TopicStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;

public class MainActivity extends AppCompatActivity {
    /**
     * A session handler that maintains the diffusion session.
     */
    private class SessionHandler implements SessionFactory.OpenCallback {
        private Session session = null;

        @Override
        public void onOpened(Session session) {
            this.session = session;

            // Get the Topics feature to subscribe to topics
            final Topics topics = session.feature( Topics.class );

            // Subscribe to the "counter" topic and establish a JSON value stream
            topics.addStream("foo/counter", JSON.class, new Topics.ValueStream.Default<JSON>() {
                @Override
                public void onSubscription(String topicPath, TopicSpecification specification) {
                    Log.i("diffusion", "Subscribed to: " + topicPath);
                }
                    
                @Override
                public void onValue(
                    String topicPath,
                    TopicSpecification specification,
                    JSON oldValue,
                    JSON newValue) {               
                    	Log.i("diffusion", topicPath + ": " + newValue.toJsonString());
                	}
          });

            topics.subscribe("foo/counter", new Topics.CompletionCallback.Default());

        }

        @Override
        public void onError(ErrorReason errorReason) {
            Log.e( "Diffusion", "Failed to open session because: " + errorReason.toString() );
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
        if (sessionHandler != null ) {
            sessionHandler.close();
            sessionHandler = null;
        }

        super.onDestroy();
    }
}
