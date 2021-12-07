/*
 * Copyright (C) 2019 Juraj Antal
 *
 * Originally created in G-Watch App
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.trupici.gwatch.wear.data;

import sk.trupici.gwatch.wear.util.StringUtils;

public abstract class PacketBase implements Packet {
    private static final String LOCAL_SOURCE = "G-Watch Service";

    private final PacketType type;
    private final String source;

    public PacketBase(PacketType type, String source) {
        this.type = type;
        this.source = source == null ? LOCAL_SOURCE : source;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Packet implementation

    @Override
    public PacketType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper methods

    /**
     * Encode <code>short</code> to byte array as unsigned 2-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    int encodeShort(byte[] data, int offset, short val) {
        int idx = offset;
        data[idx++] = (byte) (val & 0xFF);
        data[idx++] = (byte) ((val & 0xFF00) >> 8);
        return idx - offset;
    }

    /**
     * Encode <code>long</code> to byte array as unsigned 4-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    int encodeLong(byte[] data, int offset, long val) {
        int idx = offset;
        data[idx++] = (byte) (val & 0xFF);
        data[idx++] = (byte) ((val & 0xFF00) >> 8);
        data[idx++] = (byte) ((val & 0xFF0000) >> 16);
        data[idx++] = (byte) ((val & 0xFF000000) >> 24);
        return idx - offset;
    }

    /**
     * Encode <code>int</code> to byte array as unsigned 3-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    int encodeInt3(byte[] data, int offset, long val) {
        int idx = offset;
        data[idx++] = (byte) (val & 0xFF);
        data[idx++] = (byte) ((val & 0xFF00) >> 8);
        data[idx++] = (byte) ((val & 0xFF0000) >> 16);
        return idx - offset;
    }


    /**
     * Encode <code>float</code> to byte array as unsigned 4-byte IEEE 754 value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    int encodeFloat(byte[] data, int offset, float val) {
        int bits = Float.floatToIntBits(val);
        int idx = offset;
        data[idx++] = (byte) (bits & 0xFF);
        data[idx++] = (byte) ((bits & 0xFF00) >> 8);
        data[idx++] = (byte) ((bits & 0xFF0000) >> 16);
        data[idx++] = (byte) ((bits & 0xFF000000) >> 24);
        return idx - offset;
    }

    /**
     * Encode normalized <code>String</code> to byte array
     * as length (1 byte) followed by string characters as bytes.
     * <br>
     * String must be normalized using {@link StringUtils#normalize}
     * @param data byte array
     * @param offset start offset in byte array
     * @param str string to encode
     * @return number of bytes written
     */
    int encodeString(byte[] data, int offset, String str) {
        int idx = offset;
        int len = getNullableStrLen(str);
        data[idx++] = (byte)len;
        for (int i=0; i < len; i++) {
            data[idx++] = (byte) str.charAt(i);
        }
        return idx - offset;
    }

    /**
     * Returns length of string (may be null)
     * @param str - string or null
     * @return 0 if str is null, otherwise number of characters in string
     */
    int getNullableStrLen(String str) {
        return (str == null ? 0 : str.length());
    }

}
