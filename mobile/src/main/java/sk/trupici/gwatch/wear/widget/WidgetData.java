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

package sk.trupici.gwatch.wear.widget;

import android.os.BaseBundle;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import javax.annotation.Nullable;

import sk.trupici.gwatch.wear.common.data.Trend;

public class WidgetData implements Serializable {

    /** glucose sample source app */
    public static final String KEY_SOURCE = "source";
    /** glucose sample timestamp (millis from epoch) */
    public static final String KEY_TIMESTAMP = "timestamp";
    /** glucose sample value in mg/dl */
    public static final String KEY_GLUCOSE = "glucose";
    /** glucose delta from the previous sample in mg/dl */
    public static final String KEY_GLUCOSE_DELTA = "glucoseDelta";
    /** time elapsed from the glucose sample timestamp in seconds */
    public static final String KEY_TIME_DELTA = "timeDelta";
    /** trend received from data source packet */
    public static final String KEY_TREND = "trend";

    private String source;  // BG source
    private int glucose;    // BG value, 0 in case of config or time update
    private long timestamp; // BG sample time; NOT DISPLAYED
    private int glucoseDelta; // calculated from glucose data
    private int timeDelta;  // [min], calculated on every update
    private Trend trend;   // calculated from glucose data if not received

    public WidgetData() {
    }

    public static WidgetData fromBundle(BaseBundle bundle) {
        WidgetData data = new WidgetData();
        data.setSource(bundle.getString(KEY_SOURCE));
        data.setTimestamp(bundle.getLong(KEY_TIMESTAMP, 0L));
        data.setGlucose(bundle.getInt(KEY_GLUCOSE, 0));
        data.setGlucoseDelta(bundle.getInt(KEY_GLUCOSE_DELTA, 0));
        data.setTimeDelta(bundle.getInt(KEY_TIME_DELTA, 0));
        data.setTrend(Trend.valueOf(bundle.getInt(KEY_TREND)));
        return data;
    }

    @NonNull
    public static WidgetData fromJsonString(String jsonString) {
        WidgetData data = new WidgetData();
        if (jsonString == null) {
            return data;
        }

        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            data.setSource(jsonObj.optString(KEY_SOURCE, null));
            data.setTimestamp(jsonObj.optLong(KEY_TIMESTAMP, 0L));
            data.setGlucose(jsonObj.optInt(KEY_GLUCOSE, 0));
            data.setGlucoseDelta(jsonObj.optInt(KEY_GLUCOSE_DELTA, 0));
            data.setTimeDelta(jsonObj.optInt(KEY_TIME_DELTA, 0));
            data.setTrend(Trend.valueOf(jsonObj.optInt(KEY_TREND, Trend.UNKNOWN.ordinal())));
            return data;
        } catch (JSONException e) {
            return new WidgetData();
        }
    }

    public WidgetData(WidgetData widgetData) {
        this.source = widgetData.getSource();
        this.timestamp = widgetData.getTimestamp();
        this.glucose = widgetData.getGlucose();
        this.glucoseDelta = widgetData.getGlucoseDelta();
        this.timeDelta = widgetData.getTimeDelta();
        this.trend = widgetData.getTrend();
    }

    public PersistableBundle toPersistableBundle(String action) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(KEY_SOURCE, source);
        bundle.putLong(KEY_TIMESTAMP, timestamp);
        bundle.putInt(KEY_GLUCOSE, glucose);
        bundle.putInt(KEY_GLUCOSE_DELTA, glucoseDelta);
        bundle.putInt(KEY_TIME_DELTA, timeDelta);
        bundle.putInt(KEY_TREND, (trend == null ? 0 : trend.ordinal()));
        bundle.putString("action", action);
        return bundle;
    }

    @Nullable
    public String toJsonString() {
        try {
            return new JSONObject()
                    .put(KEY_SOURCE, source)
                    .put(KEY_TIMESTAMP, timestamp)
                    .put(KEY_GLUCOSE, glucose)
                    .put(KEY_GLUCOSE_DELTA, glucoseDelta)
                    .put(KEY_TIME_DELTA, timeDelta)
                    .put(KEY_TREND, (trend == null ? 0 : trend.ordinal()))
                    .toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public Bundle toBundle(String action) {
        return new Bundle(toPersistableBundle(action));
    }

    public void reset() {
        this.source = null;
        this.timestamp = 0L;
        this.glucose = 0;
        this.glucoseDelta = 0;
        this.timeDelta = 0;
        this.trend = null;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                "src: " + source + "," +
                "ts: " + timestamp + "," +
                "val: " + glucose + "," +
                "dVal: " + glucoseDelta + "," +
                "dTime: " + timeDelta + "," +
                "trend: " + trend + "}";
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getGlucose() {
        return glucose;
    }

    public void setGlucose(int glucose) {
        this.glucose = glucose;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getGlucoseDelta() {
        return glucoseDelta;
    }

    public void setGlucoseDelta(int glucoseDelta) {
        this.glucoseDelta = glucoseDelta;
    }

    public int getTimeDelta() {
        return timeDelta;
    }

    public void setTimeDelta(int timeDelta) {
        this.timeDelta = timeDelta;
    }

    public Trend getTrend() {
        return trend;
    }

    public void setTrend(Trend trend) {
        this.trend = trend;
    }
}
