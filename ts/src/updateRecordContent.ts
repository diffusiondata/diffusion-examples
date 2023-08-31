/*******************************************************************************
 * Copyright (C) 2019 - 2023 DiffusionData Ltd.
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

import { connect, datatypes, topics, Session } from 'diffusion';

// example showcasing how to update a RecordV2 topic
export async function updateRecordContentExample(): Promise<void> {
    // Connect to the server. Change these options to suit your own environment.
    // Node.js will not accept self-signed certificates by default. If you have
    // one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
    // before running this example.
    const session: Session = await connect({
        host: 'diffusion.example.com',
        port: 443,
        secure: true,
        principal: 'control',
        credentials: 'password'
    });

    // Record values allow topics to contain data that conforms to a particular schema. Producing a schema allows new
    // values to be easily constructed.

    const TopicSpecification = topics.TopicSpecification;
    const TopicType = topics.TopicType;

    const RecordV2DataType = datatypes.recordv2();

    // 1. Create a new schema using the SchemaBuilder API
    const schema = RecordV2DataType.schemaBuilder()
        .record("Row1").decimal("Field1", 3).integer("Field2")
        .record("Row2").string("Field3")
        .build();

    // 2. To create a RecordV2 topic, use a TopicSpecification with the defined schema
    const specification = new TopicSpecification(TopicType.RECORD_V2, {
        SCHEMA: schema.asJSON()
    });

    await session.topics.add('topic/record', specification);

    // 3. Produce RecordV2 values from the schema by creating a mutable model
    const model = schema.createMutableModel();

    model.set("Row1.Field1", "123.456");
    model.set("Row1.Field2", "789");
    model.set("Row2.Field3", "Hello world");

    session.topicUpdate.set('topic/record', RecordV2DataType, model.asValue());

    // 4. Subsequent updates can be produced from the same model

    model.set("Row2.Field3", "Hello everybody");

    session.topicUpdate.set('topic/record', RecordV2DataType, model.asValue());


    // RecordV2 values can be easily consumed, too
    session
        .addStream('topic/record', RecordV2DataType)
        .on('value', (topic, specification, newValue, oldValue) => {
            // 5. The schema can be used to produce a model that allows key-based lookup of records and fields
            const model = newValue.asModel(schema);

            const f1 = model.get("Row1.Field1");
            const f2 = model.get("Row1.Field2");
            const f3 = model.get("Row2.Field3");

            console.log(`Field1: ${f1} Field2: ${f2} Field3: ${f3}`);

            // 6. If the schema is not known, it is possible to iterate across the received records and fields
            for (const record of newValue.asRecords()) {
                for (const field of record) {
                    console.log("Field value: " + field);
                }
            }
        });

    session.select('topic/record');
}
