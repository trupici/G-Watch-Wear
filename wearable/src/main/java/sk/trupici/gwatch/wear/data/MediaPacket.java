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

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.PacketUtils;

public class MediaPacket extends PacketBase {

    public enum MediaType {
        IMAGE_BACKGROUND(1),
        IMAGE_AOD_BACKGROUND(2),
        SOUND_ALARM_CRITICAL(3),
        SOUND_ALARM_LOW(4),
        SOUND_ALARM_HIGH(5),
        SOUND_ALARM_FASTDROP(6),
        SOUND_ALARM_NODATA(7)
        ;

        private final int code;

        MediaType(int code) {
            this.code = code;
        }

        public byte getCodeAsByte() {
            return (byte) code;
        }
    }

    final private MediaType mediaType;
    final private String name;
    final private byte[] content;

    public MediaPacket(MediaType mediaType, String name, byte[] data) {
        super(PacketType.MEDIA, null);
        this.mediaType = mediaType;
        this.content = data;
        this.name = name;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Packet implementation

    @Override
    public byte[] getData() {
        byte[] data = new byte[getPacketLen()];
        int idx = 0;

        int totalLen = 1 + 1 + name.length() + 3  + content.length;
        data[idx++] = getType().getCodeAsByte();
        idx += PacketUtils.encodeInt3(data, idx, totalLen);

        data[idx++] = mediaType.getCodeAsByte();

        idx += PacketUtils.encodeString(data, idx, name);

        idx += PacketUtils.encodeInt3(data, idx, content.length);

        System.arraycopy(content, 0, data, idx, content.length);
        return data;
    }

    @Override
    public String toText(Context context, String header) {

        StringBuffer text = new StringBuffer();
        if (header != null) {
            text.append(header).append("\n");
        }
        text.append(context.getString(R.string.packet_type, getType().name())).append("\n");
        text.append(context.getString(R.string.packet_file_name, name)).append("\n");
        text.append(context.getString(R.string.packet_length, getPacketLen()));

        return text.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    private int getPacketLen() {
        return 1 // tag
                + 3 // + ext len + data len;
                + 1 // media type
                + 1 // name len
                + name.length()
                + 3 // data len
                + content.length;
    }

}
