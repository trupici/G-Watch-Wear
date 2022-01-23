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

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.util.BgUtils;
import sk.trupici.gwatch.wear.util.DexcomUtils;

/**
 * Intent extras contains array of bundles
 *  "glucoseValues": [
 *      {
 *          "timestamp": 1608534091,
 *          "trendArrow": "Flat",
 *          "glucoseValue": "132"
 *      }
 *  ]
 *
 *  It contains also history data from the oldest to the newest
 *  (i.e. back-filling can be made)
 */
public class DexComReceiver extends BGReceiver {
    private final static String SRC_LABEL = "DexCom";
    private final static String PREFERENCE_KEY = "pref_data_source_dexcom";
    private final static String ACTION = "com.dexcom.cgm.EXTERNAL_BROADCAST";

    private final static String EXTRA_GLUCOSE_VALUES = "glucoseValues";
    private final static String EXTRA_TIMESTAMP = "timestamp";
    private final static String EXTRA_GLUCOSE = "glucoseValue";
    private final static String EXTRA_TREND = "trendArrow";


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
            Log.i(GWatchApplication.LOG_TAG, "Bundle: " + extras.toString());

            Bundle glucoseValues = extras.getBundle(EXTRA_GLUCOSE_VALUES);
            if (glucoseValues != null) {

                // get only the latest values
                long maxTimestamp = 0L;
                int glucoseValue = 0;
                String trendStr = null;

                for (int i = 0; i < glucoseValues.size(); i++) {
                    Bundle glucoseValueBundle = glucoseValues.getBundle(String.valueOf(i));
                    if (glucoseValueBundle != null) {
                        long timestamp = glucoseValueBundle.getLong(EXTRA_TIMESTAMP);
                        int value = glucoseValueBundle.getInt(EXTRA_GLUCOSE);
                        if (timestamp > maxTimestamp && value > 0) {
                            maxTimestamp = timestamp;
                            glucoseValue = value;
                            trendStr = glucoseValueBundle.getString(EXTRA_TREND);
                        }
                    }
                }
                if (BuildConfig.DEBUG) {
                    Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                    Log.w(GWatchApplication.LOG_TAG, "Timestamp: " + maxTimestamp);
                    Log.w(GWatchApplication.LOG_TAG, "Trend: " + trendStr);
                }

                if (maxTimestamp != 0 && glucoseValue > 0) {
                    return new GlucosePacket((short)glucoseValue, maxTimestamp * 1000, (byte) 0, DexcomUtils.toTrend(trendStr), trendStr, getSourceLabel());
                }
            }
        }
        return null;
    }
}
