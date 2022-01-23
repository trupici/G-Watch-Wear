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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.util.BgUtils;

public class LibreAlarmReceiver extends BGReceiver {
    private final static String SRC_LABEL = "Libre";
    private final static String PREFERENCE_KEY = "pref_data_source_libre_alarm";
    private final static String ACTION = "com.eveningoutpost.dexdrip.FROM_LIBRE_ALARM";

    private final static String EXTRA_DATA = "data";
    private final static String DATA_TREND = "trend";
    private final static String DATA_TIMESTAMP = "realDate";
    private final static String DATA_GLUCOSE = "glucoseLevel";
    private final static String DATA_GLUCOSE_RAW = "glucoseLevelRaw";

    private final static Double LIBRE_ALARM_DIVIDER = 8.5d;

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
            String data = extras.getString(EXTRA_DATA);
            if (data != null) {
                try {
                    JSONObject mainObj = new JSONObject(data);
                    JSONObject dataObj = mainObj.optJSONObject(EXTRA_DATA);
                    if (dataObj != null) {
                        JSONArray trendArray = dataObj.optJSONArray(DATA_TREND);
                        if (trendArray != null) {
                            // get only the latest values
                            long maxTimestamp = 0L;
                            int maxTsValue = 0;

                            for (int i = 0; i < trendArray.length(); i++) {
                                JSONObject glucoseData = trendArray.getJSONObject(i);
                                long timestamp = glucoseData.optLong(DATA_TIMESTAMP, 0L);

                                int value = glucoseData.optInt(DATA_GLUCOSE, 0);
                                if (value <= 0) {
                                    value = glucoseData.optInt(DATA_GLUCOSE_RAW, 0);
                                }
                                if (timestamp > maxTimestamp && value > 0) {
                                    maxTimestamp = timestamp;
                                    maxTsValue = value;
                                }
                            }
                            double glucoseValue = maxTsValue / LIBRE_ALARM_DIVIDER;
                            if (BuildConfig.DEBUG) {
                                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                                Log.w(GWatchApplication.LOG_TAG, "Timestamp: " + maxTimestamp);
                            }
                            short glucose = (short)Math.round(glucoseValue);
                            if (glucose > 0) {
                                return new GlucosePacket(glucose, maxTimestamp, (byte) 0, null, null, getSourceLabel());
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(GWatchApplication.LOG_TAG, "Error while parsing LibreAlarm data", e);
                }
            }
        }
        return null;
    }
}
