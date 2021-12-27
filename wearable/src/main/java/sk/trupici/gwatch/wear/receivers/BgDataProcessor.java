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
package sk.trupici.gwatch.wear.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.PacketBase;
import sk.trupici.gwatch.wear.data.PacketType;
import sk.trupici.gwatch.wear.data.Trend;
import sk.trupici.gwatch.wear.util.CommonConstants;

import static sk.trupici.gwatch.wear.util.CommonConstants.LOG_TAG;

public class BgDataProcessor extends BroadcastReceiver {
    public final static String BG_PROCESSOR_ACTION = "sk.trupici.gwatch.wear.receivers.PROCESS_BG_DATA";
    public final static String EXTRA_DATA = "BG_DATA";

    private final static String PREF_LAST_BG_VALUE = AnalogWatchfaceConfig.PREF_PREFIX + "last_bg_value";
    private final static String PREF_LAST_BG_TIMESTAMP = AnalogWatchfaceConfig.PREF_PREFIX + "last_bg_ts";
    private final static String PREF_SAMPLE_PERIOD_MIN = AnalogWatchfaceConfig.PREF_PREFIX + "bg_sample_period";


    private static final String WAKE_LOCK_TAG = "gwatch.wear:" + BgDataProcessor.class.getSimpleName() + ".wake_lock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 60000; // 60s

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "Received event: " + intent.getAction());

        PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        try {
            // check and decode packet
            final byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
            if (data.length < PacketBase.PACKET_HEADER_SIZE) {
                return;
            }

            PacketType type = PacketType.getByCode(data[0]);
            Log.d(CommonConstants.LOG_TAG, "PACKET TYPE: " + (type == null ? "null" : type.name()));
            if (type != PacketType.GLUCOSE) {
                Log.d(CommonConstants.LOG_TAG, "Packet ignored" + (type == null ? "null" : type.name()));
                return;
            }

            GlucosePacket packet = GlucosePacket.of(data);
            if (packet == null) {
                Log.e(CommonConstants.LOG_TAG, "processGlucosePacket: failed to parse received data");
                return;
            }

            Log.d(CommonConstants.LOG_TAG, packet.toText(context, ""));

            // get last stored values
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int lastBgValue = prefs.getInt(PREF_LAST_BG_VALUE, 0);
            long lastBgTimestamp = prefs.getLong(PREF_LAST_BG_TIMESTAMP, 0L);
            int samplePeriod = prefs.getInt(PREF_SAMPLE_PERIOD_MIN, context.getResources().getInteger(R.integer.def_bg_sample_period));

            // evaluate received values
            int bgValue = packet.getGlucoseValue();
            long bgTimestamp = packet.getTimestamp();
            if (bgTimestamp == 0) {
                bgTimestamp = packet.getReceivedAt();
                if (bgTimestamp == 0L) {
                    bgTimestamp = System.currentTimeMillis();
                }
            }

            int valueDiff = lastBgValue == 0 ? 0 : bgValue - lastBgValue;
            long timestampDiff = lastBgTimestamp == 0 ? 0 : bgTimestamp - lastBgTimestamp;

            Trend trend = packet.getTrend();
            if (trend == null || trend == Trend.UNKNOWN) {
                trend = calcTrend(valueDiff, samplePeriod);
            };

            // store received values
            if (timestampDiff > 0) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(PREF_LAST_BG_VALUE, bgValue);
                editor.putLong(PREF_LAST_BG_TIMESTAMP, bgTimestamp);
                editor.commit();
            }

            // broadcast received values to all registered values
            BgData bgData = new BgData(bgValue, bgTimestamp, valueDiff, timestampDiff, trend);

            Intent bgIntent = new Intent(CommonConstants.BG_RECEIVER_ACTION);
            bgIntent.putExtras(bgData.toBundle());
            LocalBroadcastManager.getInstance(context).sendBroadcast(bgIntent);
        } finally {
            wakeLock.release();
        }
    }

    private Trend calcTrend(int glucoseDelta, int sampleTimeDelta) {
        if (glucoseDelta < -2 * sampleTimeDelta) {
            return Trend.DOWN;
        } else if (glucoseDelta < -sampleTimeDelta) {
            return Trend.DOWN_SLOW;
        } else if (glucoseDelta < sampleTimeDelta) {
            return Trend.FLAT;
        } else if (glucoseDelta < 2 * sampleTimeDelta) {
            return Trend.UP_SLOW;
        } else {
            return Trend.UP;
        }
    }
}
