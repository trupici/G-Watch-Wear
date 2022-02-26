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

import java.util.List;

import sk.trupici.gwatch.wear.common.util.PacketUtils;

public class ConfigPacket extends TLVPacket {
    public ConfigPacket(List<TLV> tlvList, int totalLen) {
        super(tlvList, totalLen, PacketType.CONFIG);
    }

    @Override
    protected String getPacketName() {
        return "CONFIG";
    }

    public static ConfigPacket of(byte[] data) {
        if (data.length < PACKET_HEADER_SIZE) {
            return null;
        }

        byte type = data[0];

        if (type != PacketType.CONFIG.getCodeAsByte()) {
            return null;
        }

        short shortLen = PacketUtils.decodeShort(data, 1);
        int totalLen = (shortLen & 0xFFFF);

        try {
            List<TLV> tlvList = decodeTLVs(data, 3);
            return new ConfigPacket(tlvList, totalLen);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

}
