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
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.data.Packet;
import sk.trupici.gwatch.wear.common.data.Trend;
import sk.trupici.gwatch.wear.common.util.BgUtils;


/**
    Description code received from Glimp author:

        Bundle bundle = new Bundle();
        bundle.putDouble(RESPONSE_SGV,       m.getGlucoseValueMgDl();
        bundle.putString(RESPONSE_TREND,     getTrend(m));
        bundle.putLong  (RESPONSE_TIMESTAMP, m.getDate().getMillis());
        bundle.putInt   (RESPONSE_BAT,       (int)(100f * level / scale));

        Intent intent = new Intent(ACTION);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
*/
public class GlimpReceiver extends BGReceiver {
    private final static String ACTION = "it.ct.glicemia.ACTION_GLUCOSE_MEASURED";
    private final static String EXTRA_GLUCOSE     = "mySGV";
    private final static String EXTRA_TREND       = "myTrend";
    private final static String EXTRA_TIMESTAMP   = "myTimestamp";
    private final static String EXTRA_BATTERY     = "myBatLvl";

    private final static String SRC_LABEL = "Glimp";
    private final static String PREFERENCE_KEY = "pref_data_source_glimp";

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

            double glucoseValue = extras.getDouble(EXTRA_GLUCOSE);
            String trend = extras.getString(EXTRA_TREND);
            long timestamp = extras.getLong(EXTRA_TIMESTAMP);
            int battery = extras.getInt(EXTRA_BATTERY);

            /* for debug only */
            if (glucoseValue < 1) {
                glucoseValue = Double.valueOf(String.valueOf(extras.getFloat(EXTRA_GLUCOSE)));
            }
            /* for debug only END */
            if (BuildConfig.DEBUG) {
                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                Log.w(GWatchApplication.LOG_TAG, "Trend: " + trend);
                Log.w(GWatchApplication.LOG_TAG, "Timestamp: " + new Date(timestamp));
                Log.w(GWatchApplication.LOG_TAG, "Battery: " + battery + "%");
            }
            short glucose = (short) Math.round(glucoseValue);
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
