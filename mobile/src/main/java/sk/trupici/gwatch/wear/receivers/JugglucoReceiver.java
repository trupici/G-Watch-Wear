/*
 * Copyright (C) 2022 Juraj Antal
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

/*
    from Jaap Korthals Altes:

    The new broadcast is the following:

    String ACTION = "glucodata.Minute";
    String SERIAL = "glucodata.Minute.SerialNumber";
    String MGDL = "glucodata.Minute.mgdl";
    String GLUCOSECUSTOM = "glucodata.Minute.glucose";
    String RATE = "glucodata.Minute.Rate";
    String ALARM = "glucodata.Minute.Alarm";
    String TIME = "glucodata.Minute.Time";

    private static Bundle mkGlucosebundle(String SerialNumber, int mgdl, float gl, float rate, int alarm, long timmsec) {
       Bundle extras = new Bundle();
       extras.putString(SERIAL,SerialNumber);
       extras.putInt(MGDL,mgdl);
       extras.putFloat(GLUCOSECUSTOM,gl);
       extras.putFloat(RATE,rate);
       extras.putInt(ALARM,alarm);
       extras.putLong(TIME,timmsec);
	   return extras;
	}

    Rate translates to the xdrip broadcast EXTRA_BG_SLOPE_NAME the following way:
       ##define NOT_DETERMINED ""
       std::string_view getdeltaname(float rate) {
           if(rate>=3.5f)
                return "DoubleUp";
           if(rate>=2.0f)
                return "SingleUp";
           if(rate>=1.0f)
                return "FortyFiveUp";
           if(rate>-1.0f)
                return "Flat";
           if(rate>-2.0f)
                return "FortyFiveDown";
           if(rate>-3.5f)
                return "SingleDown";
           if(isnan(rate))
                return NOT_DETERMINED;
           return "DoubleDown";
       }

    glucodata.Minute.glucose is the glucose value in the unit in settings.

    glucodata.Minute.Alarm gives an alarm indicator:
       Alarm type                                 Value of alarm
       Too high                                   6 (+8 if alarm should go off)
       Too low                                    7 (+8 if alarm should go off)
       Again a value after scanning impossible    3
       Above highest measurable e.g. 500 mg/dL    4 (+8 if alarm should go off)
       Below lowest measurable e.g. 40 mg/dL      5 (+8 if alarm should go off)

    Time is in msec
 */
public class JugglucoReceiver extends BGReceiver {

    private final static String ACTION = "glucodata.Minute";
    private final static String EXTRA_MGDL = "glucodata.Minute.mgdl";
    private final static String EXTRA_TIME = "glucodata.Minute.Time";
    private final static String EXTRA_RATE = "glucodata.Minute.Rate";

    private final static String SRC_LABEL = "Juggluco";
    private final static String PREFERENCE_KEY = "pref_data_source_juggluco";

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
            int glucoseValue = extras.getInt(EXTRA_MGDL);
            float trend = extras.getFloat(EXTRA_RATE, Float.NaN);
            long timestamp = extras.getLong(EXTRA_TIME);
            if (BuildConfig.DEBUG) {
                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                Log.w(GWatchApplication.LOG_TAG, "Trend: " + trend);
                Log.w(GWatchApplication.LOG_TAG, "Timestanp: " + new Date(timestamp));
            }
            short glucose = (short)Math.round(glucoseValue);
            if (glucose > 0) {
                return new GlucosePacket(glucose, timestamp, (byte) 0, toTrend(trend), Float.toString(trend), getSourceLabel());
            }
        }
        return null;
    }

    private static Trend toTrend(float rate) {
        if (Float.isNaN(rate)) {
            return null;
        } else if (rate >= 3.5f) {
            return Trend.UP_FAST;
        } else if (rate >= 2.0f) {
            return Trend.UP;
        } else if (rate >= 1.0f) {
            return Trend.UP_SLOW;
        } else if (rate > -1.0f) {
            return Trend.FLAT;
        } else if (rate > -2.0f) {
            return Trend.DOWN_SLOW;
        } else if (rate > -3.5f) {
            return Trend.DOWN;
        } else {
            return Trend.DOWN_FAST;
        }
    }
}
