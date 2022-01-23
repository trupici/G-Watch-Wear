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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.data.AAPSPacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.StringUtils;

/**
 */
public class AAPSReceiver extends BGReceiver {

    private final static String ACTION = "info.nightscout.androidaps.status";

    private final static String SRC_LABEL = "AAPS";
    private final static String PREFERENCE_KEY = "pref_data_source_aaps";


    private final static String BG_VALUE = "glucoseMgdl";           // double
    private final static String BG_TIMESTAMP = "glucoseTimeStamp";  // long (ms)
    private final static String BG_UNITS = "units";                 // string: "mg/dl" or "mmol"
    private final static String BG_SLOPE = "slopeArrow";            // string: direction arrow as string
    private final static String BG_DELTA = "deltaMgdl";             // double
    private final static String BG_AVG_DELTA = "avgDeltaMgdl";      // double
    private final static String BG_LOW_LINE = "high";               // double
    private final static String BG_HIGH_LINE = "low";               // double

    private final static String IOB_BOLUS = "bolusIob";             // double
    private final static String IOB_BASAL = "basalIob";             // double
    private final static String IOB_TOTAL = "iob";                  // double

    private final static String COB_VALUE = "cob";                  // double: COB [g] or -1 if N/A
    private final static String COB_FUTURE = "futureCarbs";         // double: future scheduled carbs

    private final static String LOOP_PHONE_BATT = "phoneBattery";         // int (%)
    private final static String LOOP_RIG_BATT = "rigBattery";             // int (%)
    private final static String LOOP_SUGGESTED_TS = "suggestedTimeStamp"; // long (ms) / -1 if N/A
    private final static String LOOP_SUGGESTED = "suggested";             // string
    private final static String LOOP_ENACTED_TS = "enactedTimeStamp";     // long (ms) / -1 if N/A
    private final static String LOOP_ENACTED = "enacted";                 // string

    private final static String BASAL_TIMESTAMP = "basalTimeStamp";             // long (ms)
    private final static String BASAL_BASE = "baseBasal";                       // double (U/h)
    private final static String BASAL_PROFILE = "profile";                      // string
    private final static String TMP_BASAL_START = "tempBasalStart";                 // long (ms)
    private final static String TMP_BASAL_DURATION = "tempBasalDurationInMinutes";  // int (mins)
    private final static String TMP_BASAL_ABSOLUTE = "tempBasalAbsolute";           // double: (U/h)
    private final static String TMP_BASAL_PERCENT = "tempBasalPercent";             // int (%)
    private final static String TMP_BASAL_STRING = "tempBasalString";               // string: user friendly string

    private final static String PUMP_TIMESTAMP = "pumpTimeStamp";   // long: (ms)
    private final static String PUMP_BATTERY = "pumpBattery";       // int (%)
    private final static String PUMP_RESERVOIR = "pumpReservoir";   // double
    private final static String PUMP_STATUS = "pumpStatus";         // string (JSON)

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
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return null;
        }

        if (BuildConfig.DEBUG) {
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Log.i(GWatchApplication.LOG_TAG, key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
                }
            }
        }

        Double bg = bundle.getDouble(BG_VALUE);
        Long bgTimestamp = bundle.getLong(BG_TIMESTAMP);

        boolean bgInvalid = (bg == null || bgTimestamp == null
                || PreferenceUtils.isConfigured(context, "pref_data_source_aaps_ignore_bg", false));
        AAPSPacket packet = new AAPSPacket(
                bgInvalid ? 0 : (short)Math.round(bg),
                bgInvalid ? 0 : bgTimestamp);

        try {
            packet.setIob(bundle.getDouble(IOB_TOTAL));
            packet.setIobBolus(bundle.getDouble(IOB_BOLUS));
            packet.setIobBasal(bundle.getDouble(IOB_BASAL));

            packet.setCob(bundle.getDouble(COB_VALUE));
            packet.setCobFuture(bundle.getDouble(COB_FUTURE));

            packet.setBasalTimestamp(bundle.getLong(BASAL_TIMESTAMP));
            packet.setBasalProfile(StringUtils.normalize(bundle.getString(BASAL_PROFILE)));
            packet.setTempBasalString(StringUtils.normalize(bundle.getString(TMP_BASAL_STRING)));

            packet.setPumpTimestamp(bundle.getLong(PUMP_TIMESTAMP));
            packet.setPumpBattery(bundle.getInt(PUMP_BATTERY));
            packet.setPumpReservoir(bundle.getDouble(PUMP_RESERVOIR));
            packet.setPumpStatus(StringUtils.normalize(bundle.getString(PUMP_STATUS)));
            packet.setSlopeArrow(bundle.getString(BG_SLOPE, null));
        } catch (Exception e) {
            Log.e(GWatchApplication.LOG_TAG, e.getLocalizedMessage(), e);
        }

        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, packet.toText(context, null));
        }
        return packet;
    }
}
