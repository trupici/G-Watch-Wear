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

package sk.trupici.gwatch.wear.common.data;

import java.util.Arrays;

/**
 * Packet type enumeration.
 * Types ordinals must be equal in watch processor.
 */
public enum PacketType {
    SYNC(0),
    CONFIG(1),
    GLUCOSE(2),
    AAPS(3),
    ;

    private final int code;

    PacketType(int code) {
        this.code = code;
    }

    public byte getCodeAsByte() {
        return (byte) code;
    }

    public static PacketType getByCode(int code) {
        return Arrays.stream(values()).filter(x -> x.code == code).findFirst().orElseGet(null);
    }

}
