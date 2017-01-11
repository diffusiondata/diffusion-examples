package com.pushtechnology.demo.subscribe;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.TopicStream;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.types.UpdateContext;

public class MainActivity extends AppCompatActivity {
    /**
     * A topic stream that prints updates to logcat.
     */
    private static class TopicStreamLogcat extends TopicStream.Default {
        @Override
        public void onTopicUpdate(String topic, Content content,
                                  UpdateContext context) {

            Log.i("Topic Stream", topic + ":   " + content.asString());
        }
    }

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

            // Add a new topic stream for 'foo/counter'
            topics.addTopicStream(">foo/counter", new TopicStreamLogcat());

            // Subscribe to the topic 'foo/counter'
            topics.subscribe("foo/counter",
                    new Topics.CompletionCallback.Default());
        }

        @Override
        public void onError(ErrorReason errorReason) {
            Log.e( "Diffusion", "Failed to open session because: " + errorReason.toString() );
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

            // Connect anonymously
            // Replace 'host' with your hostname
            Diffusion.sessions().open("ws://host:80", sessionHandler );
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
