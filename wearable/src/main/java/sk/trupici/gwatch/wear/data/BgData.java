/*
 * Copyright (C) 2019 Juraj Antal
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

package sk.trupici.gwatch.wear.data;

import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.UiUtils;

public class BgData {
    private final static String KEY_VALUE = "value";
    private final static String KEY_TIMESTAMP = "timestamp";
    private final static String KEY_VALUE_DIFF = "diff";
    private final static String KEY_TS_DIFF = "ts_diff";
    private final static String KEY_TREND = "trend";

    private final int value;
    private final long timestamp;
    private final int valueDiff;
    private final long timestampDiff;
    private final Trend trend;

    public BgData(int value, long timestamp, int valueDiff, long timestampDiff, Trend trend) {
        this.value = value;
        this.timestamp = timestamp;
        this.valueDiff = valueDiff;
        this.timestampDiff = timestampDiff;
        this.trend = trend;
    }

    public static BgData fromBundle(@NonNull Bundle bundle) {
        return new BgData(
                bundle.getInt(KEY_VALUE, 0),
                bundle.getLong(KEY_TIMESTAMP, 0L),
                bundle.getInt(KEY_VALUE_DIFF, 0),
                bundle.getLong(KEY_TS_DIFF, 0L),
                Trend.valueOf(bundle.getString(KEY_TREND, Trend.UNKNOWN.name()))
        );
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_VALUE, value);
        bundle.putLong(KEY_TIMESTAMP, timestamp);
        bundle.putInt(KEY_VALUE_DIFF, valueDiff);
        bundle.putLong(KEY_TS_DIFF, timestampDiff);
        if (trend != null) {
            bundle.putString(KEY_TREND, trend.name());
        }
        return bundle;
    }

    public String toString() {
        return "{ value: " + value + " (" + UiUtils.convertGlucoseToMmolL2Str(value) + ")"
                + ", ts: " + UiUtils.formatTime(new Date(timestamp))
                + ", diff: " + valueDiff + " (" + UiUtils.convertGlucoseToMmolL2Str(valueDiff) + ")"
                + ", ts diff: " + timestampDiff / CommonConstants.SECOND_IN_MILLIS
                + ", trend: " + trend
                + " }";
    }

    public int getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getValueDiff() {
        return valueDiff;
    }

    public long getTimestampDiff() {
        return timestampDiff;
    }

    public Trend getTrend() {
        return trend;
    }
}
