//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2017 - 2022 Push Technology Ltd.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import Foundation
import Diffusion

/**
 This example class has a number of methods that demonstrate the creation of
 schemas for recordV2 topics, using the Diffusion Apple Client Library.
 
 @note A client session is not required in order to create a schema.
 */
class CreatingRecordV2SchemaExample {

    /**
     Example of a schema consisting of a single record with three fields, each
     of a different data type.
     */
    func createSimpleSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "Record")
            .addString(withName: "string")
            .addInteger(withName: "integer")
            .addDecimal(withName: "decimal", scale: 3)
            .build()
    }

    /**
     Example of a schema consisting of multiple records, each record with a
     single field of a specific type.
     */
    func createMultipleRecordsSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "StringRecord").addString(withName: "string")
            .addRecord(withName: "IntegerRecord").addInteger(withName: "integer")
            .addRecord(withName: "DecimalRecord").addDecimal(withName: "decimal", scale: 3)
            .build()
    }

    /**
     Example of a schema consisting of a record (with a single string field)
     repeating exactly 10 times.
     */
    func createFixedRepeatingRecordsSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "RepeatingRecord", occurs: 10)
            .addString(withName: "string")
            .build()
    }

    /**
     Example of a schema consisting of 2 record types. "FixedRecord" is a record
     that occurs 5 times. "RepeatingRecord" is an optional record that can be
     repeated as many times as required (unlimited).
     */
    func createVariableRepeatingRecordsSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "FixedRecord", occurs: 5).addString(withName: "a")
            .addRecord(withName: "RepeatingRecord", min: 0, max: -1).addString(withName: "b")
            .build()
    }

    /**
     Example of a schema consisting of a single record with a string field that
     occurs exactly 10 times.
     */
    func createFixedRepeatingFieldsSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "Record")
            .addString(withName: "RepeatingString", occurs: 10)
            .build()
    }

    /**
     Example of a schema consisting of two records. The first record (A) has a
     field, "RepeatingField", which can occur between 2 and 5 times. The second
     record (B) has a field, "RepeatingFieldUnlimited", which can occur as many
     times as required but at least once.
     */
    func createVariableRepeatingFieldsSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "A").addString(withName: "RepeatingField", min: 2, max: 5)
            .addRecord(withName: "B").addString(withName: "RepeatingFieldUnlimited", min: 1, max: -1)
            .build()
    }

    /**
     Example of a schema consisting of a single record and multiple fields
     encapsulating a person's name and address.
     */
    func createNameAndAddressSchema() -> PTDiffusionRecordV2Schema {
        return PTDiffusionRecordV2SchemaBuilder()
            .addRecord(withName: "nameAndAddress")
            .addString(withName: "firstName")
            .addString(withName: "surname")
            .addInteger(withName: "houseNumber")
            .addString(withName: "street")
            .addString(withName: "town")
            .addString(withName: "state")
            .addString(withName: "postcode")
            .build()
    }

}
