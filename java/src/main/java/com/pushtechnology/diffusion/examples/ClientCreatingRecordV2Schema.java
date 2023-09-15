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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.datatype.recordv2.RecordV2DataType;
import com.pushtechnology.diffusion.datatype.recordv2.schema.Schema;
import com.pushtechnology.diffusion.datatype.recordv2.schema.SchemaBuilder;

/**
 * This example class has a number of methods that demonstrate the creation of
 * schemas for RECORD_V2 topics, using the Diffusion Client API.
 * <P>
 * Note that no client session is required in order to create a schema.
 *
 * @author DiffusionData Limited
 * @since 6.0
 */
public final class ClientCreatingRecordV2Schema {

    private final RecordV2DataType dataType = Diffusion.dataTypes().recordV2();

    /**
     * Constructor.
     */
    public ClientCreatingRecordV2Schema() {
    }

    /**
     * Example of a schema consisting of a single record with three fields each
     * of s different data type.
     *
     * @return a schema
     */
    public Schema createSimpleSchema() {
        final SchemaBuilder builder = dataType.schemaBuilder();
        return builder
            .record("Record")
            .string("string").integer("integer").decimal("decimal", 3)
            .build();
    }

    /**
     * Example of a schema consisting of multiple records, each record with a
     * single field of a specific type.
     *
     * @return a schema
     */
    public Schema createMultipleRecordsSchema() {
        final SchemaBuilder builder = dataType.schemaBuilder();
        return builder
            .record("StringRecord").string("string")
            .record("IntegerRecord").integer("integer")
            .record("DecimalRecord").decimal("decimal", 3)
            .build();
    }

    /**
     * Example of a schema consisting of a record (with a single string field)
     * repeating exactly 10 times.
     *
     * @return a schema
     */
    public Schema createFixedRepeatingRecordsSchema() {
        final SchemaBuilder builder = dataType.schemaBuilder();
        return builder
            .record("RepeatingRecord", 10).string("string")
            .build();
    }

    /**
     * Example of a schema consisting of 2 record types. "FixedRecord" is a
     * record that occurs 5 times. "RepeatingRecord" is an optional record that
     * can be repeated as many times as required (unlimited).
     *
     * @return a schema
     */
    public Schema createVariableRepeatingRecordsSchema() {
        final SchemaBuilder builder = dataType.schemaBuilder();
        return builder
            .record("FixedRecord", 5).string("a")
            .record("RepeatingRecord", 0, -1).string("b")
            .build();
    }

    /**
     * Example of a schema consisting of a single record with a string field
     * that occurs exactly 10 times.
     *
     * @return a schema
     */
    public Schema createFixedRepeatingFieldsSchema() {
        final SchemaBuilder builder = dataType.schemaBuilder();
        return builder
            .record("Record").string("repeatingString", 10)
            .build();
    }

    /**
     * Example of a schema consisting of two records. The first record (A) has a
     * field, "repeatingField", which can occur between 2 and 5 times. The
     * second record (B) has a field, "repeatingFieldUnlimited", which can occur
     * as many times as required but at least once.
     *
     * @return a schema
     */
    public Schema createVariableRepeatingFieldsSchema() {
        final SchemaBuilder builder = dataType.schemaBuilder();
        return builder
            .record("A").string("repeatingField", 2, 5)
            .record("B").string("repeatingFieldUnlimited", 1, -1)
            .build();
    }

    /**
     * Example of a schema consisting of a single record and multiple fields
     * encapsulating a person's name and address.
     *
     * @return a schema
     */
    public Schema createNameAndAddressSchema() {
        final SchemaBuilder builder = dataType.schemaBuilder();
        return builder
            .record("nameAndAddress")
            .string("firstName")
            .string("surname")
            .integer("houseNumber")
            .string("street")
            .string("town")
            .string("state")
            .string("postCode")
            .build();
    }

}
