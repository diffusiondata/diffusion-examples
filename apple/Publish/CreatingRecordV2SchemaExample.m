//  Diffusion Client Library for iOS, tvOS and OS X / macOS - Examples
//
//  Copyright (C) 2017 - 2023 DiffusionData Ltd.
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

@import Foundation;
@import Diffusion;

/**
 This example class has a number of methods that demonstrate the creation of
 schemas for recordV2 topics, using the Diffusion Apple Client Library.

 @note A client session is not required in order to create a schema.
 */
@interface CreatingRecordV2SchemaExample : NSObject
@end

@implementation CreatingRecordV2SchemaExample

/**
 Example of a schema consisting of a single record with three fields, each
 of a different data type.
 */
+(PTDiffusionRecordV2Schema *)createSimpleSchema {
    PTDiffusionRecordV2SchemaBuilder *const builder =
        [PTDiffusionRecordV2SchemaBuilder new];

    [builder addRecordWithName:@"Record"];
    [builder addStringWithName:@"string"];
    [builder addIntegerWithName:@"integer"];
    [builder addDecimalWithName:@"decimal" scale:3];

    return [builder build];
}

/**
 Example of a schema consisting of multiple records, each record with a
 single field of a specific type.
 */
+(PTDiffusionRecordV2Schema *)createMultipleRecordsSchema {
    PTDiffusionRecordV2SchemaBuilder *const builder =
        [PTDiffusionRecordV2SchemaBuilder new];

    [builder addRecordWithName:@"StringRecord"];
    [builder addStringWithName:@"string"];

    [builder addRecordWithName:@"IntegerRecord"];
    [builder addIntegerWithName:@"integer"];

    [builder addRecordWithName:@"DecimalRecord"];
    [builder addDecimalWithName:@"decimal" scale:3];

    return [builder build];
}

/**
 Example of a schema consisting of a record (with a single string field)
 repeating exactly 10 times.
 */
+(PTDiffusionRecordV2Schema *)createFixedRepeatingRecordsSchema {
    PTDiffusionRecordV2SchemaBuilder *const builder =
        [PTDiffusionRecordV2SchemaBuilder new];

    [builder addRecordWithName:@"RepeatingRecord" occurs:10];
    [builder addStringWithName:@"string"];

    return [builder build];
}

/**
 Example of a schema consisting of 2 record types. "FixedRecord" is a record
 that occurs 5 times. "RepeatingRecord" is an optional record that can be
 repeated as many times as required (unlimited).
 */
+(PTDiffusionRecordV2Schema *)createVariableRepeatingRecordsSchema {
    PTDiffusionRecordV2SchemaBuilder *const builder =
        [PTDiffusionRecordV2SchemaBuilder new];

    [builder addRecordWithName:@"FixedRecord" occurs:5];
    [builder addStringWithName:@"a"];

    [builder addRecordWithName:@"RepeatingRecord" min:0 max:-1];
    [builder addStringWithName:@"b"];

    return [builder build];
}

/**
 Example of a schema consisting of a single record with a string field that
 occurs exactly 10 times.
 */
+(PTDiffusionRecordV2Schema *)createFixedRepeatingFieldsSchema {
    PTDiffusionRecordV2SchemaBuilder *const builder =
        [PTDiffusionRecordV2SchemaBuilder new];

    [builder addRecordWithName:@"Record"];
    [builder addStringWithName:@"a" occurs:10];

    return [builder build];
}

/**
 Example of a schema consisting of two records. The first record (A) has a
 field, "RepeatingField", which can occur between 2 and 5 times. The second
 record (B) has a field, "RepeatingFieldUnlimited", which can occur as many
 times as required but at least once.
 */
+(PTDiffusionRecordV2Schema *)createVariableRepeatingFieldsSchema {
    PTDiffusionRecordV2SchemaBuilder *const builder =
        [PTDiffusionRecordV2SchemaBuilder new];

    [builder addRecordWithName:@"A"];
    [builder addStringWithName:@"RepeatingField" min:2 max:5];

    [builder addRecordWithName:@"B"];
    [builder addStringWithName:@"RepeatingField" min:1 max:-1];

    return [builder build];
}

/**
 Example of a schema consisting of a single record and multiple fields
 encapsulating a person's name and address.
 */
+(PTDiffusionRecordV2Schema *)createNameAndAddressSchema {
    PTDiffusionRecordV2SchemaBuilder *const builder =
        [PTDiffusionRecordV2SchemaBuilder new];

    [builder addRecordWithName:@"nameAndAddress"];
    [builder addStringWithName:@"firstName"];
    [builder addStringWithName:@"surname"];
    [builder addIntegerWithName:@"houseNumber"];
    [builder addStringWithName:@"street"];
    [builder addStringWithName:@"town"];
    [builder addStringWithName:@"state"];
    [builder addStringWithName:@"postcode"];

    return [builder build];
}

@end
