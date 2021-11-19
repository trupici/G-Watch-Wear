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

package sk.trupici.gwatch.wear.model;

import android.os.BaseBundle;
import android.os.Bundle;
import android.os.PersistableBundle;

public class BgData {

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
    /** time delta from the previous glucose sample in minutes */
    public static final String KEY_SAMPLE_TIME_DELTA = "sampleTimeDelta";

    private String source;
    private int glucose;
    private long timestamp;
    private int glucoseDelta;
    private int timeDelta;
    private int sampleTimeDelta;


    public static BgData fromBundle(BaseBundle bundle) {
        BgData data = new BgData();
        data.setSource(bundle.getString(KEY_SOURCE));
        data.setTimestamp(bundle.getLong(KEY_TIMESTAMP, 0L));
        data.setGlucose(bundle.getInt(KEY_GLUCOSE, 0));
        data.setGlucoseDelta(bundle.getInt(KEY_GLUCOSE_DELTA, 0));
        data.setTimeDelta(bundle.getInt(KEY_TIME_DELTA, 0));
        data.setSampleTimeDelta(bundle.getInt(KEY_SAMPLE_TIME_DELTA, 0));
        return data;
    }

    public BgData() {
    }

    public BgData(BgData bgData) {
        this.source = bgData.getSource();
        this.timestamp = bgData.getTimestamp();
        this.glucose = bgData.getGlucose();
        this.glucoseDelta = bgData.getGlucoseDelta();
        this.timeDelta = bgData.getTimeDelta();
        this.sampleTimeDelta = bgData.getSampleTimeDelta();
    }

    public PersistableBundle toPersistableBundle(String action) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(KEY_SOURCE, source);
        bundle.putLong(KEY_TIMESTAMP, timestamp);
        bundle.putInt(KEY_GLUCOSE, glucose);
        bundle.putInt(KEY_GLUCOSE_DELTA, glucoseDelta);
        bundle.putInt(KEY_TIME_DELTA, timeDelta);
        bundle.putInt(KEY_SAMPLE_TIME_DELTA, sampleTimeDelta);
        bundle.putString("action", action);
        return bundle;
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
        this.sampleTimeDelta = 0;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{")
                .append("src: ").append(source).append(",")
                .append("ts: ").append(timestamp).append(",")
                .append("val: ").append(glucose).append(",")
                .append("dVal: ").append(glucoseDelta).append(",")
                .append("dTime: ").append(timeDelta).append(",")
                .append("dSample: ").append(sampleTimeDelta).append("}")
                .toString();
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

    public int getSampleTimeDelta() {
        return sampleTimeDelta;
    }

    public void setSampleTimeDelta(int sampleTimeDelta) {
        this.sampleTimeDelta = sampleTimeDelta;
    }

}
