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

import android.content.Context;

import java.util.List;

import sk.trupici.gwatch.wear.R;

public abstract class TLVPacket extends PacketBase {
    private List<TLV> tlvList;
    private int totalLen; // in bytes

    abstract protected String getPacketName();

    protected TLVPacket(List<TLV> tlvList, int totalLen, PacketType type) {
        super(type, null);
        this.tlvList = tlvList;
        this.totalLen = totalLen;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Packet implementation

    @Override
    public byte[] getData() {
        byte[] data = new byte[getPacketLen()];
        int idx = 0;

        data[idx++] = getType().getCodeAsByte();
        if (totalLen > 255) {
            data[idx++] = (byte) ((totalLen & 0xFF00) >> 8);
        } else {
            data[idx++] = 0;
        }
        data[idx++] = (byte)(totalLen & 0xFF);

        for (TLV tlv : tlvList) {
            if (tlv.getTotalLen() + idx > data.length) {
                throw new RuntimeException("Buffer too small: " + (tlv.getTotalLen() + idx) + " > " + data.length);
            }
            data[idx++] = tlv.getTag();
            data[idx++] = (byte)(tlv.getLen() & 0xFF);
            System.arraycopy(tlv.getValue(), 0, data, idx, tlv.getLen());
            idx += tlv.getLen();
        }

        return data;
    }

    @Override
    public String toText(Context context, String header) {

        StringBuffer text = new StringBuffer();
        if (header != null) {
            text.append(header).append("\n");
        }
        text.append(context.getString(R.string.packet_type, getPacketName())).append("\n");
        text.append(context.getString(R.string.packet_source, getSource())).append("\n");
        text.append(context.getString(R.string.tlv_packet_items, tlvList.size())).append("\n");
        text.append(context.getString(R.string.packet_length, getPacketLen()));

        return text.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    private int getPacketLen() {
        return 1 + 2 + totalLen;
    }
}
