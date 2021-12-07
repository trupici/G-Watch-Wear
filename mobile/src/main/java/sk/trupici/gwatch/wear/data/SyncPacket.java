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

public class SyncPacket extends PacketBase {
    private static final int PKT_LEN = 2;

    public SyncPacket() {
        super(PacketType.SYNC, null);
    }

    @Override
    public byte[] getData() {
        byte[] data = new byte[PKT_LEN];
        data[0] = getType().getCodeAsByte();
        data[1] = 0;
        return data;
    }

    @Override
    public String toText(Context context, String header) {

        StringBuffer text = new StringBuffer();
        if (header != null) {
            text.append(header).append("\n");
        }
        text.append(context.getString(R.string.packet_type, getType().name()));
        return text.toString();
    }
}
