/*******************************************************************************
 * Copyright (C) 2014, 2016 Push Technology Ltd.
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

import java.util.Collection;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.TopicTreeHandler;
import com.pushtechnology.diffusion.client.content.metadata.MRecord;
import com.pushtechnology.diffusion.client.content.metadata.MetadataFactory;
import com.pushtechnology.diffusion.client.content.update.PagedRecordOrderedUpdateFactory;
import com.pushtechnology.diffusion.client.content.update.PagedStringUnorderedUpdateFactory;
import com.pushtechnology.diffusion.client.content.update.Update;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.UpdateSource;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl.Updater.UpdateCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.PagedRecordTopicDetails;
import com.pushtechnology.diffusion.client.topics.details.PagedRecordTopicDetails.Attributes.PagedRecordOrderingPolicy.OrderKey;
import com.pushtechnology.diffusion.client.topics.details.TopicType;

/**
 * An example of using a control client to create and update paged topics.
 * <P>
 * This uses the 'TopicControl' feature to create a paged topic and the
 * 'TopicUpdateControl' feature to send updates to it.
 * <P>
 * This demonstrates some simple examples of paged topic updates but not all of
 * the possible ways in which they can be done.
 * <P>
 * To send updates to a topic, the client session requires the 'update_topic'
 * permission for that branch of the topic tree.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
@SuppressWarnings("deprecation")
public class ControlClientUpdatingPagedTopics {

    private static final String ORDERED_TOPIC = "Paged/Ordered";
    private static final String UNORDERED_TOPIC = "Paged/Unordered";

    private final Session session;
    private final TopicControl topicControl;
    private final TopicUpdateControl updateControl;
    private final PagedRecordOrderedUpdateFactory orderedUpdateFactory;
    private final PagedStringUnorderedUpdateFactory unorderedUpdateFactory;
    private Updater pagedUpdater = null;

    /**
     * Constructor.
     */
    public ControlClientUpdatingPagedTopics() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        topicControl = session.feature(TopicControl.class);
        updateControl = session.feature(TopicUpdateControl.class);

        orderedUpdateFactory =
            updateControl.updateFactory(
                PagedRecordOrderedUpdateFactory.class);
        unorderedUpdateFactory =
            updateControl.updateFactory(
                PagedStringUnorderedUpdateFactory.class);

        final MetadataFactory metadata = Diffusion.metadata();

        // Create an unordered paged string topic
        topicControl.addTopic(
            UNORDERED_TOPIC,
            topicControl.newDetails(TopicType.PAGED_STRING),
            new AddCallback.Default() {
                @Override
                public void onTopicAdded(String topicPath) {
                    // Request removal of topics when session closes
                    topicControl.removeTopicsWithSession(
                        "Paged",
                        new TopicTreeHandler.Default());
                }
            });

        // Create an ordered paged record topic
        final MRecord recordMetadata =
            metadata.record(
                "Record",
                metadata.string("Name"),
                metadata.string("Address"));

        topicControl.addTopic(
            ORDERED_TOPIC,
            topicControl
                .newDetailsBuilder(PagedRecordTopicDetails.Builder.class)
                .metadata(recordMetadata).order(new OrderKey("Name")).build(),
            new AddCallback.Default());

        // Register as updater for topics under the 'Paged' branch
        updateControl.registerUpdateSource(
            "Paged",
            new UpdateSource.Default() {
                @Override
                public void onActive(String topicPath, Updater updater) {
                    pagedUpdater = updater;
                }
            });
    }

    /**
     * Add a new line to the ordered topic.
     *
     * @param name the name field value
     * @param address the address field value
     * @param callback to notify result
     */
    public void addOrdered(
        String name,
        String address,
        UpdateCallback callback) {

        update(
            ORDERED_TOPIC,
            orderedUpdateFactory.add(
                Diffusion.content().newRecord(name, address)),
            callback);
    }

    /**
     * Update a line of an ordered topic.
     *
     * @param name the name of the line to update
     * @param address the new address field value
     * @param callback to notify result
     */
    public void updateOrdered(
        String name,
        String address,
        UpdateCallback callback) {

        update(
            ORDERED_TOPIC,
            orderedUpdateFactory.update(
                Diffusion.content().newRecord(name, address)),
            callback);
    }

    /**
     * Remove a line from an ordered topic.
     *
     * @param name the name of the line to remove
     * @param callback to notify result
     */
    public void removeOrdered(String name, UpdateCallback callback) {

        update(
            ORDERED_TOPIC,
            orderedUpdateFactory.remove(
                Diffusion.content().newRecord(name, "")),
            callback);
    }

    /**
     * Add a line or lines to the end of an unordered topic.
     *
     * @param values lines to add
     * @param callback to notify result
     */
    public void addUnordered(
        Collection<String> values,
        UpdateCallback callback) {

        update(
            UNORDERED_TOPIC,
            unorderedUpdateFactory.add(values),
            callback);
    }

    /**
     * Insert a line or lines at a specified index within an unordered topic.
     *
     * @param index the index to add at
     * @param values lines to insert
     * @param callback to notify result
     */
    public void insertUnordered(
        int index,
        Collection<String> values,
        UpdateCallback callback) {
        update(
            UNORDERED_TOPIC,
            unorderedUpdateFactory.insert(index, values),
            callback);
    }

    /**
     * Update a line within an unordered topic.
     *
     * @param index the index of the line to update
     * @param value the new line value
     * @param callback to notify result
     */
    public void updateUnordered(
        int index,
        String value,
        UpdateCallback callback) {

        update(
            UNORDERED_TOPIC,
            unorderedUpdateFactory.update(index, value),
            callback);
    }

    /**
     * Remove a specific line from an unordered topic.
     *
     * @param index the line to remove
     * @param callback to notify result
     */
    public void removeUnordered(int index, UpdateCallback callback) {
        update(
            UNORDERED_TOPIC,
            unorderedUpdateFactory.remove(index),
            callback);
    }

    private void update(String topic, Update update, UpdateCallback callback)
        throws IllegalStateException {
        if (pagedUpdater == null) {
            throw new IllegalStateException("No updater");
        }
        pagedUpdater.update(topic, update, callback);
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }
}
