/*******************************************************************************
 * Copyright (C) 2015, 2016 Push Technology Ltd.
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

import com.pushtechnology.diffusion.api.message.MessageReader;
import com.pushtechnology.diffusion.api.message.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.conflation.MessageMerger;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.publisher.Publishers;

import java.util.ArrayList;
import java.util.List;

/**
 * Example MessageMerger that merges delta messages for record topics with variable records and fields.
 *
 * The fields of each record in newMessage are iterated over and compared against the corresponding field in
 * currentMessage. As long as a given field in newMessage is not empty it will take priority and get merged.
 *
 * If newMessage has more records than currentMessage, the additional records will be merged. If currentMessage has more
 * records than newMessage, the additional records will not be merged.
 *
 * If a record in newMessage has more fields than currentMessage, the additional fields will be merged. If a record in
 * currentMessage has more fields than newMessage, the additional fields will not be merged.
 *
 * Note: This MessageMerger should only be used with topics that have had an empty field value specified.
 *
 * @author Push Technology Limited
 * @since 5.9
 */
public final class MessageMergerExample implements MessageMerger {
    private static final Logger LOG =
            LoggerFactory.getLogger(MessageMergerExample.class);

    @Override
    public TopicMessage merge(TopicMessage currentMessage, TopicMessage newMessage) throws APIException {

        final MessageReader currentMessageReader = currentMessage.getReader();
        final MessageReader newMessageReader = newMessage.getReader();

        final TopicMessage result = Publishers.createDeltaMessage(currentMessage.getTopicName());

        Record cRecord = currentMessageReader.nextRecord();
        Record nRecord = newMessageReader.nextRecord();

        while (nRecord != null) {
            final List<String> mergedRecord = new ArrayList<>(nRecord.size());
            for (int i = 0; i < nRecord.size(); i++) {
                final String nField = nRecord.getField(i);
                if (!nField.isEmpty() || cRecord == null || i >= cRecord.size()) {
                    mergedRecord.add(nField);
                }
                else {
                    mergedRecord.add(cRecord.getField(i));
                }
            }

            result.putRecord(mergedRecord);

            nRecord = newMessageReader.nextRecord();
            cRecord = currentMessageReader.nextRecord();
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("MessageMerger merging - currentMessage: {}, newMessage: {}, merged: {}",
                    currentMessage.asRecords(), newMessage.asRecords(), result.asRecords());
        }

        return result;
    }
}