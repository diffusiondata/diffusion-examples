package com.pushtechnology.demo.update;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    /**
     * A session handler that maintains the diffusion session.
     */
    private class SessionHandler implements SessionFactory.OpenCallback {
        private Session session = null;

        @Override
        public void onOpened(Session session) {
            this.session = session;

            // Get the TopicControl and TopicUpdateControl feature
            final TopicControl topicControl = session.feature(TopicControl.class);

            final TopicUpdateControl updateControl = session
                    .feature(TopicUpdateControl.class);

            final CountDownLatch waitForStart = new CountDownLatch(1);

            // Create a single value topic 'foo/counter'
            topicControl.addTopic("foo/counter", TopicType.SINGLE_VALUE,
                    new AddCallback.Default() {
                        @Override
                        public void onTopicAdded(String topicPath) {
                            waitForStart.countDown();
                        }
                    });

            // Wait for the onTopicAdded() callback.
            try {
                waitForStart.await();
            } catch (InterruptedException e) {
                Log.e("Diffusion", e.getStackTrace().toString());
            }

            // Update the topic
            for (int i = 0; i < 1000; ++i) {

                // Use the non-exclusive updater to update the topic without locking
                // it
                updateControl.updater().update("foo/counter", Integer.toString(i),
                        new UpdateCallback.Default());

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("Diffusion", e.getStackTrace().toString());
                }
            }
        }

        @Override
        public void onError(ErrorReason errorReason) {
            Log.e("Diffusion", "Failed to open session because: " + errorReason.toString());
            session = null;
        }

        public void close() {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private SessionHandler sessionHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if ( sessionHandler == null ) {
            sessionHandler = new SessionHandler();

            // Connect using a principal with 'modify_topic' and 'update_topic'
            // permissions
            Diffusion.sessions().principal("principal")
                                .password("password")
                                .open("ws://host:80", sessionHandler );
        }
    }

    @Override
    protected void onDestroy() {
        if ( sessionHandler != null ) {
            sessionHandler.close();
            sessionHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
