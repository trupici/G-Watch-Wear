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
package sk.trupici.gwatch.wear.settings;

public class ConfigData {

    public static final byte TAG_GL_THRESHOLD_HYPO =  0x01;
    public static final byte TAG_GL_THRESHOLD_LOW =   0x02;
    public static final byte TAG_GL_THRESHOLD_HIGH =  0x03;
    public static final byte TAG_GL_THRESHOLD_HYPER = 0x04;
    public static final byte TAG_GL_UNIT_CONVERSION = 0x05;


    final private byte tag;
    final private ConfigType type;
    final private String prefName;

    public ConfigData(byte tag, ConfigType type) {
        this(tag, type, null);
    }

    public ConfigData(byte tag, ConfigType type, String prefName) {
        this.tag = tag;
        this.type = type;
        this.prefName = prefName;
    }

    public byte getTag() {
        return tag;
    }

    public ConfigType getType() {
        return type;
    }

    public String getPrefName() {
        return prefName;
    }
}
