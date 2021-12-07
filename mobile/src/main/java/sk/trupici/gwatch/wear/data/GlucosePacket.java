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
import sk.trupici.gwatch.wear.util.UiUtils;

public class GlucosePacket extends GlucosePacketBase {
    public static final int PACKET_MIN_DATA_SIZE = (2 + 4 + 1);

    public enum Trend {
        UNKNOWN,
        UP_FAST,
        UP,
        UP_SLOW,
        FLAT,
        DOWN_SLOW,
        DOWN,
        DOWN_FAST
    }

    private final byte battery;
    private final Trend trend;
    private final String rawTrend;

    public GlucosePacket(short glucoseValue, long timestamp, byte battery, Trend trend, String rawTrend, String source) {
        super(PacketType.GLUCOSE, source, glucoseValue, timestamp);
        this.battery = battery;
        this.trend = (trend == null ? Trend.UNKNOWN : trend);
        this.rawTrend = rawTrend;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Packet implementation

    @Override
    public byte[] getData() {
        Integer dataSize = PACKET_MIN_DATA_SIZE;
        byte[] data = new byte[PACKET_HEADER_SIZE + dataSize];
        int idx = 0;

        data[idx++] = getType().getCodeAsByte();
        data[idx++] = dataSize.byteValue();

        idx += encodeShort(data, idx, glucoseValue);

        long ts = (timestamp < receivedAt) ? timestamp : receivedAt;
        idx += encodeLong(data, idx, ts / 1000); // time in seconds

        data[idx] = (byte)trend.ordinal();
        return data;
    }

    @Override
    public String toText(Context context, String header) {
        StringBuffer text = new StringBuffer(super.toText(context, header));
        text.append(context.getString(R.string.packet_battery, battery)).append("\n");
        text.append(context.getString(R.string.packet_trend, UiUtils.getStringOrNoData(rawTrend)));
        return text.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    public byte getBattery() {
        return battery;
    }
    public Trend getTrend() {
        return trend;
    }
}
