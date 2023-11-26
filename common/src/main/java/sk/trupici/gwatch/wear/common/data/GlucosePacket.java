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

import android.content.Context;

import sk.trupici.gwatch.wear.common.R;
import sk.trupici.gwatch.wear.common.util.PacketUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;

public class GlucosePacket extends GlucosePacketBase {
    public static final int PACKET_MIN_DATA_SIZE = (2 + 4 + 1);

    private final byte battery;
    private final Trend trend;
    private final String rawTrend;

    public GlucosePacket(short glucoseValue, long timestamp, byte battery, Trend trend, String rawTrend, String source) {
        super(PacketType.GLUCOSE, source, glucoseValue, timestamp);
        this.battery = battery;
        this.trend = trend;
        this.rawTrend = rawTrend;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Packet implementation

    @Override
    public byte[] getData() {
        Integer dataSize = PACKET_MIN_DATA_SIZE
                + 1 + PacketUtils.getNullableStrLen(getSource());
        byte[] data = new byte[PACKET_HEADER_SIZE + dataSize];
        int idx = 0;

        data[idx++] = getType().getCodeAsByte();
        data[idx++] = dataSize.byteValue();

        idx += PacketUtils.encodeShort(data, idx, glucoseValue);

        long ts = Math.min(timestamp, receivedAt);
        idx += PacketUtils.encodeInt(data, idx, ts / 1000); // time in seconds

        data[idx++] = (byte)(trend == null ? 0 : trend.ordinal());
        PacketUtils.encodeString(data, idx, getSource());
        return data;
    }

    @Override
    public String toText(Context context, String header) {
        StringBuffer text = new StringBuffer(super.toText(context, header));
        text.append(context.getString(R.string.packet_battery, battery)).append("\n");
        text.append(context.getString(R.string.packet_trend, StringUtils.getStringOrNoData(rawTrend)));
        return text.toString();
    }

    public static GlucosePacket of(byte[] data) {
        if (data == null || data.length < PACKET_HEADER_SIZE) {
            return null;
        }

        int idx = 0;
        byte type = data[idx++];
        byte size = data[idx++];

        if (type != PacketType.GLUCOSE.getCodeAsByte() || size < PACKET_MIN_DATA_SIZE) {
            return null;
        }

        short value = PacketUtils.decodeShort(data, idx);
        long timestamp = PacketUtils.decodeInt(data, idx+2) * 1000L;
        Trend trend = Trend.valueOf(data[idx+6]);
        String source = size == PACKET_MIN_DATA_SIZE ? null :  PacketUtils.decodeString(data, idx+7);

        return new GlucosePacket(value, timestamp, (byte)0, trend, trend.name(), source);
    }

    ///////////////////////////////////////////////////////////////////////////

    public byte getBattery() {
        return battery;
    }
    public Trend getTrend() {
        return trend;
    }
}
