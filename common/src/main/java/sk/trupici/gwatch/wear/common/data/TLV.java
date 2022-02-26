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

public class TLV {
    private byte tag;
    private byte len;
    private byte[] value;

    public TLV(byte tag, byte len, byte[] value) {
        this.tag = tag;
        this.len = len;
        this.value = value;
    }

    public int getTotalLen() {
        return 2 + len;
    }

    public byte getTag() {
        return tag;
    }

    public byte getLen() {
        return len;
    }

    public byte[] getValue() {
        return value;
    }
}
