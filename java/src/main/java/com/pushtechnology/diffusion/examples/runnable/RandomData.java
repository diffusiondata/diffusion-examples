/*******************************************************************************
 * Copyright (C) 2016, 2023 DiffusionData Ltd.
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

package com.pushtechnology.diffusion.examples.runnable;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.System.currentTimeMillis;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.datatype.binary.Binary;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * Random data for examples.
 *
 * @author DiffusionData Limited
 * @since 5.7
 */
public final class RandomData {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
    private static final Random RANDOM = new Random();
    private static final CBORFactory CBOR_FACTORY = new CBORFactory();
    private static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper(CBOR_FACTORY);

    private final int id;
    private final long timestamp;
    private final int randomInt;

    private RandomData(int id, long timestamp, int randomInt) {
        this.id = id;
        this.timestamp = timestamp;
        this.randomInt = randomInt;
    }

    /**
     * @return The ID of the data value
     */
    public int getId() {
        return id;
    }

    /**
     * @return The timestamp the data value was created
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return The random integer of the data value
     */
    public int getRandomInt() {
        return randomInt;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + id + ",timestamp=" +
            timestamp + ",randomInt=" + randomInt + '>';
    }

    /**
     * @return The next {@link RandomData}
     */
    static RandomData next() {
        synchronized (RandomData.class) {
            return new RandomData(
                ID_GENERATOR.getAndIncrement(),
                currentTimeMillis(),
                RANDOM.nextInt(MAX_VALUE));
        }
    }

    /**
     * Serialize a {@link RandomData} value as a {@link Binary} value.
     * @param randomData The {@link RandomData} value
     * @return The {@link Binary} value
     */
    static Binary toBinary(RandomData randomData) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(randomData.getId());
        buffer.putLong(randomData.getTimestamp());
        buffer.putInt(randomData.getRandomInt());
        return Diffusion.dataTypes().binary().readValue(buffer.array());
    }

    /**
     * Deserialize a {@link Binary} value as a {@link RandomData} value.
     * @param binary The {@link Binary} value
     * @return The {@link RandomData} value
     */
    static RandomData fromBinary(Binary binary) {
        final ByteBuffer buffer = ByteBuffer.wrap(binary.toByteArray());
        final int id = buffer.getInt();
        final long timestamp = buffer.getLong();
        final int randomInt = buffer.getInt();
        return new RandomData(id, timestamp, randomInt);
    }

    /**
     * Serialize a {@link RandomData} value as a {@link JSON} value.
     * @param randomData The {@link RandomData} value
     * @return The {@link JSON} value
     */
    static JSON toJSON(RandomData randomData)
        throws JsonProcessingException {

        return Diffusion
            .dataTypes()
            .json()
            .readValue(OBJECT_MAPPER.writeValueAsBytes(randomData));
    }
}
