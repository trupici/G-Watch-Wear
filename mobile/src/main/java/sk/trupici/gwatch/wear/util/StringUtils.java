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

package sk.trupici.gwatch.wear.util;

import java.text.Normalizer;

/**
 * String helper methods
 */
public class StringUtils {

    public static final String EMPTY_STRING = "";

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            builder.append(HEX_DIGITS[(b >> 4) & 0x0F]).append(HEX_DIGITS[b & 0x0F]);
        }
        return builder.toString();
    }

    public static String populateString(String str, int count) {
        if (str == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(str.length() * count);
        for (int i=0; i < count; i++) {
            builder.append(str);
        }
        return builder.toString();
    }

    public static String normalize(String str) {
        return str == null ? null : Normalizer
                .normalize(str, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", EMPTY_STRING);
    }
}
