/*
 * Copyright (C) 2021 Juraj Antal
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

package sk.trupici.gwatch.wear.util;

public class PacketUtils {

    /**
     * Encode <code>short</code> to byte array as unsigned 2-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    public static int encodeShort(byte[] data, int offset, short val) {
        int idx = offset;
        data[idx++] = (byte) (val & 0xFF);
        data[idx++] = (byte) ((val & 0xFF00) >> 8);
        return idx - offset;
    }

    /**
     * Decode <code>short</code> from byte array as unsigned 2-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @return decoded short value
     */
    public static short decodeShort(byte[] data, int offset) {
        int value = (data[offset++] & 0xFF);
        value += ((data[offset++] << 8) & 0xFF00);
        return (short) value;
    }

    /**
     * Encode <code>int</code> to byte array as unsigned 4-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    public static int encodeInt(byte[] data, int offset, long val) {
        int idx = offset;
        data[idx++] = (byte) (val & 0xFF);
        data[idx++] = (byte) ((val & 0xFF00) >> 8);
        data[idx++] = (byte) ((val & 0xFF0000) >> 16);
        data[idx++] = (byte) ((val & 0xFF000000) >> 24);
        return idx - offset;
    }

    /**
     * Decode <code>long</code> from byte array as unsigned 4-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @return decoded int value
     */
    public static int decodeInt(byte[] data, int offset) {
        int value = (data[offset++] & 0xFF);
        value += ((data[offset++] << 8) & 0xFF00);
        value += ((data[offset++] << 16) & 0xFF0000);
        value += ((data[offset] << 24) & 0xFF000000);
        return value;
    }


    /**
     * Encode <code>int</code> to byte array as unsigned 3-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    public static int encodeInt3(byte[] data, int offset, long val) {
        int idx = offset;
        data[idx++] = (byte) (val & 0xFF);
        data[idx++] = (byte) ((val & 0xFF00) >> 8);
        data[idx++] = (byte) ((val & 0xFF0000) >> 16);
        return idx - offset;
    }

    /**
     * Decode <code>int</code> from byte array as unsigned 3-byte value
     * @param data byte array
     * @param offset start offset in byte array
     * @return decoded int value
     */
    public static int decodeInt3(byte[] data, int offset) {
        int value = (data[offset++] & 0xFF);
        value += ((data[offset++] << 8) & 0xFF00);
        value += ((data[offset++] << 16) & 0xFF0000);
        return value;
    }


    /**
     * Encode <code>float</code> to byte array as unsigned 4-byte IEEE 754 value
     * @param data byte array
     * @param offset start offset in byte array
     * @param val value to encode
     * @return number of bytes written
     */
    public static int encodeFloat(byte[] data, int offset, float val) {
        int bits = Float.floatToIntBits(val);
        int idx = offset;
        data[idx++] = (byte) (bits & 0xFF);
        data[idx++] = (byte) ((bits & 0xFF00) >> 8);
        data[idx++] = (byte) ((bits & 0xFF0000) >> 16);
        data[idx++] = (byte) ((bits & 0xFF000000) >> 24);
        return idx - offset;
    }

    /**
     * Decode <code>float</code> from byte array as unsigned 4-byte IEEE 754 value
     * @param data byte array
     * @param offset start offset in byte array
     * @return decoded IEEE 754 value
     */
    public static float decodeFloat(byte[] data, int offset) {
        int bits = (data[offset++] & 0xFF);
        bits += ((data[offset++] << 8) & 0xFF00);
        bits += ((data[offset++] << 16) & 0xFF0000);
        bits += ((data[offset] << 24) & 0xFF000000);
        return Float.intBitsToFloat(bits);
    }

    /**
     * Encode normalized <code>String</code> to byte array
     * as length (1 byte) followed by string characters as bytes.
     * <br>
     * String must be normalized using {@code StringUtils#normalize}
     * @param data byte array
     * @param offset start offset in byte array
     * @param str string to encode
     * @return number of bytes written
     */
    public static int encodeString(byte[] data, int offset, String str) {
        int idx = offset;
        int len = getNullableStrLen(str);
        data[idx++] = (byte)len;
        for (int i=0; i < len; i++) {
            data[idx++] = (byte) str.charAt(i);
        }
        return idx - offset;
    }

    /**
     * Decode normalized <code>String</code> from byte array
     * as length (1 byte) followed by 1-byte encoded characters.
     * <br>
     * String must be normalized using {@code StringUtils#normalize}
     * @param data byte array
     * @param offset start offset in byte array
     * @return decoded String
     */
    public static String decodeString(byte[] data, int offset) {
        int len = (data[offset++] & 0xFF);
        if (len == 0) {
            return StringUtils.EMPTY_STRING;
        }

        StringBuffer sb = new StringBuffer(len);
        for (int i=0; i < len; i++) {
            int value = (data[offset++] & 0xFF);
            sb.append((char)value);
        }
        return sb.toString();
    }

    /**
     * Returns length of string (may be null)
     * @param str - string or null
     * @return 0 if str is null, otherwise number of characters in string
     */
    public static int getNullableStrLen(String str) {
        return (str == null ? 0 : str.length());
    }
}
