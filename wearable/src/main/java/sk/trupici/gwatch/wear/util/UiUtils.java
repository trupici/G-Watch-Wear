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

package sk.trupici.gwatch.wear.util;

import android.os.Handler;
import android.os.Looper;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;

import sk.trupici.gwatch.wear.data.Trend;

public class UiUtils {

    public static final Double GLUCOSE_CONV_FACTOR = 18.018018;
    public static final String GLUCOSE_UNITS_MGDL = "mg/dl";
    public static final String GLUCOSE_UNITS_MMOLL = "mmol/l";

    public static final String NO_DATA_STR = "-";

    private static final char[] TREND_SET = {' ', '⇈', '↑', '↗', '→', '↘', '↓', '⇊'}; // standard arrows
    private static final char[] TREND_SET_2 = {' ', '⮅', '⭡', '⭧', '⭢', '⭨', '⭣', '⮇'}; // triangle arrows (not all chars on watch)

    public static char getTrendChar(Trend trend) {
        int idx = trend == null ? 0 : trend.ordinal();
        return TREND_SET[idx];
    }

    public static String formatDateTime(Date date) {
        return java.text.DateFormat.getDateTimeInstance().format(date);
    }

    public static String formatTime(Date date) {
        return java.text.DateFormat.getTimeInstance().format(date);
    }

    public static String formatTimeOrNoData(long timestamp) {
        return timestamp == 0 ? NO_DATA_STR : java.text.DateFormat.getTimeInstance().format(new Date(timestamp));
    }

    public static String formatDoubleOrNoData(Double value) {
        return (value == null || value == -1) ? UiUtils.NO_DATA_STR : String.format("%.2f", value);
    }

    public static String getStringOrNoData(String str) {
        return (str == null) ? UiUtils.NO_DATA_STR : str;
    }

    public static Double convertGlucoseToMmolL(double glucoseValue) {
        return Math.round(glucoseValue / GLUCOSE_CONV_FACTOR * 10d) / 10d;
    }

    public static Double convertGlucoseToMmolL2(double glucoseValue) {
        return Math.round(glucoseValue / GLUCOSE_CONV_FACTOR * 100d) / 100d;
    }

    public static Double convertGlucoseToMgDl(double glucoseValue) {
        return (double) Math.round(glucoseValue * GLUCOSE_CONV_FACTOR);
    }

    private static char getDefaultDecimalSeparator() {
        return ((DecimalFormat)DecimalFormat.getInstance(Locale.getDefault())).getDecimalFormatSymbols().getDecimalSeparator();
    }

    public static String convertGlucoseToMmolLStr(double glucoseValue) {
        char decimalSeparator = getDefaultDecimalSeparator();
        return String.valueOf(convertGlucoseToMmolL(glucoseValue)).replace('.', decimalSeparator);
    }

    public static String convertGlucoseToMmolL2Str(double glucoseValue) {
        char decimalSeparator = getDefaultDecimalSeparator();
        double value = convertGlucoseToMmolL2(glucoseValue);
        return String.valueOf(value).replace('.', decimalSeparator);
    }

    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(runnable);
        } else {
            runnable.run();
        }
    }

    public static String getGlucoseUnitsStr(boolean isUnitConv) {
        return isUnitConv ? GLUCOSE_UNITS_MMOLL : GLUCOSE_UNITS_MGDL;
    }

}
