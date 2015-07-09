/*******************************************************************************
 * Copyright (C) 2014, 2015 Push Technology Ltd.
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

import java.util.List;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.TopicTreeHandler;
import com.pushtechnology.diffusion.client.content.Content;
import com.pushtechnology.diffusion.client.content.RecordContentBuilder;
import com.pushtechnology.diffusion.client.content.metadata.MContent;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.AddContextCallback;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl.RemoveCallback;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.RecordTopicDetails;
import com.pushtechnology.diffusion.client.topics.details.TopicDetails;

/**
 * An example of using a control client to add topics.
 * <P>
 * This uses the 'TopicControl' feature only.
 * <P>
 * To add or remove topics, the client session must have the 'modify_topic'
 * permission for that branch of the topic tree.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public class ControlClientAddingTopics {

    private final Session session;

    private final TopicControl topicControl;

    /**
     * Constructor.
     */
    public ControlClientAddingTopics() {

        session =
            Diffusion.sessions().principal("control").password("password")
                .open("ws://diffusion.example.com:80");

        topicControl = session.feature(TopicControl.class);

    }

    /**
     * Adds a topic with type derived from value.
     * <P>
     * This uses the simple convenience method for adding topics where the topic
     * type and metadata are derived from a supplied value which can be any
     * object. For example, an Integer would result in a single value topic of
     * type integer.
     *
     * @param topicPath full topic path
     * @param initialValue an optional initial value for the topic
     * @param context this will be passed back to the callback when reporting
     *        success or failure of the topic add
     * @param callback to notify result of operation
     * @param <T> the value type
     * @return the topic details used to add the topic
     */
    public <T> TopicDetails addTopicForValue(
        String topicPath,
        T initialValue,
        String context,
        AddContextCallback<String> callback) {

        return topicControl.addTopicFromValue(
            topicPath,
            initialValue,
            context,
            callback);
    }

    /**
     * Add a record topic from a list of initial values.
     * <P>
     * This demonstrates the simplest mechanism for adding a record topic by
     * supplying values that both the metadata and the initial values are
     * derived from.
     *
     * @param topicPath full topic path
     * @param initialValues the initial values for the topic fields which will
     *        also be used to derive the metadata definition of the topic
     * @param context this will be passed back to the callback when reporting
     *        success or failure of the topic add
     * @param callback to notify result of operation
     * @return the topic details used to add the topic
     */
    public TopicDetails addRecordTopic(
        String topicPath,
        List<String> initialValues,
        String context,
        AddContextCallback<String> callback) {

        return topicControl.addTopicFromValue(
            topicPath,
            Diffusion.content().newBuilder(RecordContentBuilder.class)
                .putFields(initialValues).build(),
            context,
            callback);

    }

    /**
     * Adds a record topic with supplied metadata and optional initial content.
     * <P>
     * This example shows details being created and would be fine when creating
     * topics that are all different but if creating many record topics with the
     * same details then it is far more efficient to pre-create the details.
     *
     * @param topicPath the full topic path
     * @param metadata pre-created record metadata
     * @param initialValue optional initial value for the topic which must have
     *        been created to match the supplied metadata
     * @param context context passed back to callback when topic created
     * @param callback to notify result of operation
     */
    public void addRecordTopic(
        String topicPath,
        MContent metadata,
        Content initialValue,
        String context,
        AddContextCallback<String> callback) {

        final TopicDetails details =
            topicControl.newDetailsBuilder(RecordTopicDetails.Builder.class)
                .metadata(metadata).build();

        topicControl.addTopic(
            topicPath,
            details,
            initialValue,
            context,
            callback);
    }

    /**
     * Remove a single topic given its path.
     *
     * @param topicPath the topic path
     * @param callback notifies result of operation
     */
    public void removeTopic(String topicPath, RemoveCallback callback) {
        topicControl.removeTopics(
            ">" + topicPath, // convert to a topic path selector
            callback);
    }

    /**
     * Remove one or more topics using a topic selector expression.
     *
     * @param topicSelector the selector expression
     * @param callback notifies result of operation
     */
    public void removeTopics(String topicSelector, RemoveCallback callback) {
        topicControl.removeTopics(
            topicSelector,
            callback);
    }

    /**
     * Request that the topic {@code topicPath} and its descendants be removed
     * when the session is closed (either explicitly using {@link Session#close}
     * , or by the server). If more than one session calls this method for the
     * same {@code topicPath}, the topics will be removed when the last session
     * is closed.
     *
     * <p>
     * Different sessions may call this method for the same topic path, but not
     * for topic paths above or below existing registrations on the same branch
     * of the topic tree.
     *
     * @param topicPath the part of the topic tree to remove when the last
     *        session is closed
     */
    public void removeTopicsWithSession(String topicPath) {
        topicControl.removeTopicsWithSession(
            topicPath, new TopicTreeHandler.Default());
    }

    /**
     * Close the session.
     */
    public void close() {
        session.close();
    }

}
