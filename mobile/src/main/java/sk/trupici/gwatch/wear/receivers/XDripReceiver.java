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

package sk.trupici.gwatch.wear.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Date;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.data.Trend;
import sk.trupici.gwatch.wear.util.BgUtils;

public class XDripReceiver extends BGReceiver {
    private final static String ACTION = "com.eveningoutpost.dexdrip.BgEstimate";
    private final static String EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate";
    private final static String EXTRA_BG_SLOPE_NAME = "com.eveningoutpost.dexdrip.Extras.BgSlopeName";
    private final static String EXTRA_SENSOR_BATTERY = "com.eveningoutpost.dexdrip.Extras.SensorBattery";
    private final static String EXTRA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.Time";
    private final static String SRC_LABEL = "xDrip+";
    private final static String PREFERENCE_KEY = "pref_data_source_xdrip";


    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public String getSourceLabel() {
        return SRC_LABEL;
    }

    @Override
    public String getAction() {
        return ACTION;
    }

    @Override
    protected Packet processIntent(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            double glucoseValue = extras.getDouble(EXTRA_BG_ESTIMATE);
            String trend = extras.getString(EXTRA_BG_SLOPE_NAME);
            long timestamp = extras.getLong(EXTRA_TIMESTAMP);
            int battery = extras.getInt(EXTRA_SENSOR_BATTERY);
            if (BuildConfig.DEBUG) {
                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                Log.w(GWatchApplication.LOG_TAG, "Trend: " + trend);
                Log.w(GWatchApplication.LOG_TAG, "Timestamp: " + new Date(timestamp));
                Log.w(GWatchApplication.LOG_TAG, "Battery: " + battery + "%");
            }
            short glucose = (short)Math.round(glucoseValue);
            if (glucose > 0) {
                return new GlucosePacket(glucose, timestamp, (byte) battery, toTrend(trend), trend, getSourceLabel());
            }
        }
        return null;
    }

    private static Trend toTrend(String value) {
        if (value == null) {
            return null;
        } else if ("DoubleUp".equals(value)) {
            return Trend.UP_FAST;
        } else if ("SingleUp".equals(value)) {
            return Trend.UP;
        } else if ("FortyFiveUp".equals(value)) {
            return Trend.UP_SLOW;
        } else if ("Flat".equals(value)) {
            return Trend.FLAT;
        } else if ("FortyFiveDown".equals(value)) {
            return Trend.DOWN_SLOW;
        } else if ("SingleDown".equals(value)) {
            return Trend.DOWN;
        } else if ("DoubleDown".equals(value)) {
            return Trend.DOWN_FAST;
        } else {
            return Trend.UNKNOWN;
        }
    }
}
