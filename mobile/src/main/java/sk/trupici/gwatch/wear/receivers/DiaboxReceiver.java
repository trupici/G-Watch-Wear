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

import org.json.JSONException;
import org.json.JSONObject;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.data.Packet;
import sk.trupici.gwatch.wear.common.util.BgUtils;

/**
 * description from LibreHack:
 *
 * Bundle bundle = new Bundle();
 *    bundle.putString("data", data.json);
 *    Intent intent = new Intent("com.outshineiot.diabox.BgEstimate");
 *    intent.putExtras(bundle);
 *    sendBroadcast(intent)
 * *
 * data:
 * {
 *   "realTimeGlucose": {
 *     "timestamp": 1599077082859,  // start of the sensor
 *     "index": 20060,
 *     "raw": 53
 *   },
 *   "historicGlucose": [{
 *     "timestamp": 1600252482859,
 *     "index": 19590,
 *     "raw": 45
 *   }, {
 *     "timestamp": 1600253382859,
 *     "index": 19605,
 *     "raw": 32
 *   }]
 * }
 */
public class DiaboxReceiver extends BGReceiver {
    private final static String SRC_LABEL = "DiaBox";
    private final static String PREFERENCE_KEY = "pref_data_source_diabox";
    private final static String ACTION = "com.outshineiot.diabox.BgEstimate";

    private final static String EXTRA_DATA = "data";
    private final static String EXTRA_REALTIME = "realTimeGlucose";
    private final static String EXTRA_HISTORICAL = "historicGlucose";
    private final static String DATA_TIMESTAMP = "timestamp";
    private final static String DATA_GLUCOSE = "raw";

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
                    JSONObject dataObj = new JSONObject(data);
                    if (dataObj != null) {
                        JSONObject realtimeObj = dataObj.optJSONObject(EXTRA_REALTIME);
                        if (realtimeObj != null) {
                            long now = System.currentTimeMillis();
                            long timestamp = realtimeObj.optLong(DATA_TIMESTAMP, 0L);
                            int glucoseValue = realtimeObj.optInt(DATA_GLUCOSE, 0);
                            if (BuildConfig.DEBUG) {
                                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                                Log.w(GWatchApplication.LOG_TAG, "Timestamp: " + timestamp + " -> " + now);
                            }
                            short glucose = (short)Math.round(glucoseValue);
                            if (glucose > 0) {
                                // since the timestamp in realtime glucose is a timestamp when sensor was started we need to use current time here...
                                return new GlucosePacket(glucose, now, (byte) 0, null, null, getSourceLabel());
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(GWatchApplication.LOG_TAG, "Error while parsing Diabox data", e);
                }
            }
        }
        return null;
    }
}
