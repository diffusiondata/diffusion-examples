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

import static com.pushtechnology.diffusion.datatype.DataTypes.INT64_DATATYPE_NAME;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.TimeSeries;
import com.pushtechnology.diffusion.client.features.TimeSeries.EventMetadata;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * This example shows a control client creating a {@link TimeSeries} topic.
 * Values can be appended to the topic using {@link #appendValue(Long)}, and
 * the last value of the topic can be edited using {@link #editLast(Long)}.
 *
 * @author Push Technology Limited
 * @since 6.0
 * @see ClientConsumingTimeSeriesTopics
 * @see TimeSeriesQueryExample
 */
public class ControlClientUpdatingTimeSeriesTopics {

    private static final String TOPIC_PATH = "foo/timeseries";
    private static final Logger LOG =
        LoggerFactory.getLogger(ControlClientUpdatingTimeSeriesTopics.class);

    private final Session session;
    private final TimeSeries timeSeries;
    private final TopicControl topicControl;

    /**
     * Constructor.
     *
     * @param serverUrl server URL to connect to example "ws://diffusion.example.com:80"
     */
    public ControlClientUpdatingTimeSeriesTopics(String serverUrl)
        throws InterruptedException, ExecutionException, TimeoutException {

        session = Diffusion.sessions().principal("control").password("password")
            .open(serverUrl);

        timeSeries = session.feature(TimeSeries.class);
        topicControl = session.feature(TopicControl.class);

        final TopicSpecification spec = topicControl.newSpecification(TopicType.TIME_SERIES)
            .withProperty(TopicSpecification.TIME_SERIES_EVENT_VALUE_TYPE, INT64_DATATYPE_NAME);

        topicControl.addTopic(TOPIC_PATH, spec)
            .thenAccept(result -> LOG.info("Add topic result: {}", result)).get(5, TimeUnit.SECONDS);
    }

    /**
     * Appends a value to the time series topic.
     *
     * @param value value to append
     * @return the event metadata from the successful append
     */
    public EventMetadata appendValue(long value)
        throws IllegalArgumentException, InterruptedException, ExecutionException, TimeoutException {
        return timeSeries.append(TOPIC_PATH, Long.class, value).get(5, TimeUnit.SECONDS);
    }

    /**
     * Close the session and remove the time series topic.
     */
    public void close()
        throws IllegalArgumentException, InterruptedException, ExecutionException, TimeoutException {
        topicControl.removeTopics("?foo//").get(5, TimeUnit.SECONDS);
        session.close();
    }

    /**
     * Edit the last value in a time series topic.
     *
     * @param value value to edit with
     */
    public void editLast(long value) {
        //Obtain the last value in the time series topic
        timeSeries.rangeQuery().fromLast(1).as(Long.class).selectFrom(TOPIC_PATH)
            .whenComplete((query, ex) -> {
                if (ex != null) {
                    LOG.error("Error obtaining the range query: {}", ex);
                    return;
                }
                //Perform the value edit
                query.stream().forEach(event -> {
                    timeSeries.edit(TOPIC_PATH, event.sequence(), Long.class, value)
                        .whenComplete((metadata, e) -> {
                            if (e != null) {
                                LOG.error("Error editing topic: {}", e);
                                return;
                            }
                            LOG.info("EventMetadata from edit: {}", metadata);
                    });
                });
            });
    }
}
