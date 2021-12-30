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
}
