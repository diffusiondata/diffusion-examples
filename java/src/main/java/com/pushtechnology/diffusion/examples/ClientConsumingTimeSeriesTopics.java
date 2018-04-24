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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TimeSeries;
import com.pushtechnology.diffusion.client.features.TimeSeries.Event;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.features.Topics.ValueStream;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * This demonstrates a client session subscribing to a
 * {@link TimeSeries} topic.
 *
 * @author Push Technology Limited
 * @since 6.0
 * @see ControlClientUpdatingTimeSeriesTopics
 * @see TimeSeriesQueryExample
 */
public class ClientConsumingTimeSeriesTopics {

    private static final String TOPIC_PATH = "foo/timeseries";

    private Session session;

    /**
     * Constructor.
     *
     * @param serverUrl for example "ws://diffusion.example.com:80"
     * @param valueStream value stream to receive time series topic events
     */
    public ClientConsumingTimeSeriesTopics(String serverUrl, ValueStream<Event<Long>> valueStream)
        throws InterruptedException, ExecutionException, TimeoutException {
        session = Diffusion.sessions().principal("client").password("password")
            .open(serverUrl);

        final Topics topics = session.feature(Topics.class);
        topics.addTimeSeriesStream(TOPIC_PATH, Long.class, valueStream);
        topics.subscribe(TOPIC_PATH).get(5, TimeUnit.SECONDS);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
