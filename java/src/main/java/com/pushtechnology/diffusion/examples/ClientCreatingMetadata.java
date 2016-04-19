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

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.content.metadata.MContent;
import com.pushtechnology.diffusion.client.content.metadata.MDecimalString;
import com.pushtechnology.diffusion.client.content.metadata.MRecord;
import com.pushtechnology.diffusion.client.content.metadata.MetadataFactory;

/**
 * This example class has a number of methods that demonstrate the creation of
 * metadata using the Diffusion Unified API.
 * <P>
 * Metadata is normally created by a control client in order to create topics,
 * but a standard client could create metadata to interpret content (however, it
 * would make much more sense to get the details of the topic which would
 * include it's metadata).
 * <P>
 * This example shows the creation of various types of metadata in different
 * methods. Content metadata is used by record type topics. Record metadata is
 * used by paged record topics and field metadata is used by single value
 * topics.
 * <P>
 * Note that no client session is required in order to create metadata.
 *
 * @author Push Technology Limited
 * @since 5.0
 */
public final class ClientCreatingMetadata {

    private final MetadataFactory factory = Diffusion.metadata();

    /**
     * Constructor.
     */
    public ClientCreatingMetadata() {
    }

    /**
     * Example of using a decimal field builder.
     * <P>
     * This is purely to demonstrate the use of a builder as in most cases one
     * of the factory convenience methods would be much easier to use to create
     * a new field metadata definition.
     *
     * @param name field name
     * @param scale scale of the decimal number
     * @param value default value
     * @return new decimal field metadata
     */
    public MDecimalString createDecimal(String name, int scale, String value) {
        return factory.decimalBuilder(name).scale(scale).defaultValue(value)
            .build();
    }

    /**
     * Creates a simple name and address record definition with fixed name
     * single multiplicity fields.
     *
     * @return record metadata definition
     */
    public MRecord createNameAndAdressRecord() {
        return factory.record(
            "NameAndAddress",
            factory.string("FirstName"),
            factory.string("SurName"),
            factory.string("HouseNumber"),
            factory.string("Street"),
            factory.string("Town"),
            factory.string("State"),
            factory.string("PostCode"));
    }

    /**
     * This creates a record with two fields, a string called "Currency" and a
     * single decimal string called "Rate" with a default value of 1.00.
     *
     * @return record metadata definition
     */
    public MRecord createCurrencyRecord() {
        return factory.record(
            "CurrencyRecord",
            factory.string("Currency"),
            factory.decimal("Rate", "1.00"));
    }

    /**
     * This creates a record with two fields, a string called "Currency" and a
     * decimal string called "Rate" with a default value of 1.00 which repeats a
     * specified number of times.
     *
     * @param name the record name
     * @param occurs the number of occurrences of the "Rate" field
     * @return the metadata record
     */
    public MRecord createMultipleRateCurrencyRecord(String name, int occurs) {
        return factory.recordBuilder(name).
            add(factory.string("Currency")).
            add(factory.decimal("Rate", "1.00"), occurs).
            build();
    }

    /**
     * A simple example of creating content metadata with two records.
     *
     * @return content metadata
     */
    public MContent createContent() {
        return factory.content(
            "Content",
            factory.record(
                "Rec1",
                factory.string("A"),
                factory.string("B")),
            factory.record(
                "Rec2",
                factory.string("C")));
    }

    /**
     * An example of how to use a content builder to create content metadata
     * with a single record type that can occur zero to n times.
     *
     * @return content metadata
     */
    public MContent createContentRepeating() {
        return factory.contentBuilder("Content").
            add(
                factory.record(
                    "Rec1",
                    factory.string("A"),
                    factory.string("B")),
                0,
                -1).
            build();
    }

}
