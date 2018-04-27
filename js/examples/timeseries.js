/*******************************************************************************
 * Copyright (C) 2018 Push Technology Ltd.
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

var diffusion = require('diffusion');

var TopicSpecification = diffusion.topics.TopicSpecification;
var TopicType = diffusion.topics.TopicType;
var dataType = diffusion.datatypes.int64();

// Connect to the server. Change these options to suit your own environment.
// Node.js does not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {
    // 1. Create a time series topic specification with events
    // of type int64
    var specification = new TopicSpecification(TopicType.TIME_SERIES, {
        TIME_SERIES_EVENT_VALUE_TYPE : "int64"
    });

    // 2. Create a time series topic
    session.topics.add('topic/timeseries', specification).then(function() {
        // 3. Register a value stream
        session.addStream('topic/timeseries', dataType).on('value', function(topic, specification, newValue, oldValue) {
            var value = newValue.toString();

            if (newValue.isEditEvent === true) {
                console.log("New value edited on topic: " + value);
            }
            else {
                console.log("New value appended to topic: " + value);
            }
        });

        // 4. Subscribe
        session.select('topic/timeseries');

        for (var i = 0; i < 10; i++) {
            // 4. Append values 0-9 to the topic
            session.timeseries.append("topic/timeseries", i, dataType.Int64);
        }

        // 5. Retrieve the last time series event and edit it
        session.timeseries.rangeQuery().as(dataType).fromLast(1).selectFrom("topic/timeseries").then(function(result) {
            session.timeseries.edit("topic/timeseries", result.events[0].sequence, 999, dataType.Int64);
        });
    });
});