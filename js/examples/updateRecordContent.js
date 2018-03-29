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

var diffusion = require('diffusion');

// Connect to the server. Change these options to suit your own environment.
// Node.js will not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal : 'control',
    credentials : 'password'
}).then(function(session) {
    // Record values allow topics to contain data that conforms to a particular schema. Producing a schema allows new
    // values to be easily constructed.

    var TopicSpecification = diffusion.topics.TopicSpecification;
    var TopicType = diffusion.topics.TopicType;

    var RecordV2DataType = diffusion.datatypes.recordv2();

    // 1. Create a new schema using the SchemaBuilder API
    var schema = RecordV2DataType.schemaBuilder()
        .record("Row1").decimal("Field1").integer("Field2")
        .record("Row2").string("Field3")
        .build();

    // 2. To create a RecordV2 topic, use a TopicSpecification with the defined schema
    var specification = new TopicSpecification(TopicType.RECORD_V2, {
        SCHEMA : schema.asJSONString()
    });

    session.topics.add('topic/record', specification).then(function() {
        // 3. Produce RecordV2 values from the schema by creating a mutable model
        var model = schema.createMutableModel();

        model.set("Row1.Field1", 123.456);
        model.set("Row1.Field2", 789);
        model.set("Row2.Field3", "Hello world");

        var update1 = model.asValue();

        session.topics.update('topic/record', update1);

        // 4. Subsequent updates can be produced from the same model

        model.set("Row2.Field3", "Hello everybody");

        var update2 = model.asValue();

        session.topics.update('topic/record', update2);
    });


    // RecordV2 values can be easily consumed, too
    session
        .stream('topic/record')
        .asType(RecordV2DataType)
        .on('value', function(topic, specification, newValue, oldValue) {
            // 5. The schema can be used to produce a model that allows key-based lookup of records and fields
            var model = newValue.asModel(schema);

            var f1 = model.get("Row1.Field1");
            var f2 = model.get("Row1.Field2");
            var f3 = model.get("Row2.Field3");

            console.log(
                "Field1: " + f1 +
                "Field2: " + f2 +
                "Field3: " + f3);

            // 6. If the schema is not known, it is possible to iterate across the received records and fields
            newValue.asRecords().forEach(function(record) {
                record.forEach(function(field) {
                    console.log("Field value: " + field);
                });
            });
        });

    session.subscribe('topic/record');
});
