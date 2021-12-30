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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.PacketUtils;

public abstract class TLVPacket extends PacketBase {
    private static final String LOG_TAG = TLVPacket.class.getSimpleName();

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
        idx += PacketUtils.encodeShort(data, idx, (short)totalLen);

        for (TLV tlv : tlvList) {
            if (tlv.getTotalLen() + idx > data.length) {
                throw new RuntimeException("Buffer too small: " + (tlv.getTotalLen() + idx) + " > " + data.length);
            }
            data[idx++] = tlv.getTag();
            data[idx++] = tlv.getLen();
            System.arraycopy(tlv.getValue(), 0, data, idx, tlv.getLen() & 0xFF);
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

    protected static List<TLV> decodeTLVs(byte[] data, int offset) {
        if (data.length < PACKET_HEADER_SIZE) {
            return null;
        }

        int idx = offset;
        List<TLV> tlvList = new ArrayList<>();

        while (idx < data.length) {
            byte tag = data[idx++];
            byte len = data[idx++];
            int intLen = (len & 0xFF);
            byte[] value = new byte[intLen];
            if (data.length < idx + intLen) {
                Log.e(LOG_TAG, "decodeTLVs: invalid TLV data");
                throw new ArrayIndexOutOfBoundsException("data buffer size exceeded: " + (idx + intLen) + " of " + data.length);
            }
            System.arraycopy(data, idx, value, 0, value.length);
            idx += intLen;
            tlvList.add(new TLV(tag, len, value));
        }
        return tlvList;
    }

    ///////////////////////////////////////////////////////////////////////////

    private int getPacketLen() {
        return 1 + 2 + totalLen;
    }

    public List<TLV> getTlvList() {
        return tlvList;
    }
}
