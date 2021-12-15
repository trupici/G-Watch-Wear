/*
 * Copyright (C) 2021 Juraj Antal
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

package sk.trupici.gwatch.wear.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import sk.trupici.gwatch.wear.data.AAPSPacket;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.PacketBase;
import sk.trupici.gwatch.wear.data.PacketType;
import sk.trupici.gwatch.wear.data.Trend;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.DumpUtils;

import static sk.trupici.gwatch.wear.util.CommonConstants.LOG_TAG;

public class BgDataListenerService extends WearableListenerService {

    public static final String EXTRA_BG_VALUE = "bgValue";
    public static final String EXTRA_BG_TIMESTAMP = "bgTimestamp";
    public static final String EXTRA_BG_RECEIVEDAT = "bgReceivedAt";
    public static final String EXTRA_BG_TREND = "bgTrend";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOG_TAG, "Received event with Message path: " + messageEvent.getPath());

        if (!messageEvent.getPath().equals("/bg_data")) {
            super.onMessageReceived(messageEvent);
            return;
        }

        final byte[] data = messageEvent.getData();
        Log.v("myTag", "Message received:\n" + DumpUtils.dumpData(data, data.length));
        Log.d(CommonConstants.LOG_TAG, DumpUtils.dumpData(data, data.length));

        if (data.length < PacketBase.PACKET_HEADER_SIZE) {
            return;
        }

        PacketType type = PacketType.getByCode(data[0]);
        Log.d(CommonConstants.LOG_TAG, "PACKET TYPE: " + (type == null ? "null" : type.name()));
        if (type == PacketType.GLUCOSE) {
            processGlucosePacket(data);
        } else if (type == PacketType.AAPS) {
            processAAPSPacket(data);
        } else {
            Log.d(CommonConstants.LOG_TAG, "Packet ignored" + (type == null ? "null" : type.name()));
        }
    }

    private void sendBgBroadcast(int bgValue, long bgTimestamp, Trend bgTrend, long bgReceivedAt) {
        Intent intent = new Intent();
        intent.setAction(CommonConstants.BG_RECEIVER_ACTION);
        intent.putExtra(EXTRA_BG_VALUE, bgValue);
        intent.putExtra(EXTRA_BG_TIMESTAMP, bgTimestamp);
        intent.putExtra(EXTRA_BG_RECEIVEDAT, bgReceivedAt);
        if (bgTrend != null) {
            intent.putExtra(EXTRA_BG_TREND, bgTrend);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void processGlucosePacket(byte[] data) {
        GlucosePacket packet = GlucosePacket.of(data);
        if (packet == null) {
            Log.e(CommonConstants.LOG_TAG, "processGlucosePacket: failed to parse received data");
            return;
        }

        Log.d(CommonConstants.LOG_TAG, packet.toText(getApplicationContext(), ""));

        sendBgBroadcast(packet.getGlucoseValue(), packet.getTimestamp(), packet.getTrend(), packet.getReceivedAt());
    }

    private void processAAPSPacket(byte[] data) {
        AAPSPacket packet = AAPSPacket.of(data);
        if (packet == null) {
            Log.e(CommonConstants.LOG_TAG, "processAAPSPacket: failed to parse received data");
            return;
        }

        Log.d(CommonConstants.LOG_TAG, packet.toText(getApplicationContext(), ""));

//        if (!ignoreAapsBg)
        sendBgBroadcast(packet.getGlucoseValue(), packet.getTimestamp(), null, packet.getReceivedAt());

        // TODO send AAPS data
    }

}