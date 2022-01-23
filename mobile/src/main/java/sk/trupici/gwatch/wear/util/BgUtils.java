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

package sk.trupici.gwatch.wear.util;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Locale;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.data.Trend;

public class BgUtils {

    private static final String LOG_TAG = BgUtils.class.getSimpleName();

    public static final Double GLUCOSE_CONV_FACTOR = 18.018018;
    public static final String GLUCOSE_UNITS_MGDL = "mg/dl";
    public static final String GLUCOSE_UNITS_MMOLL = "mmol/l";

    private static final char[] TREND_SET = {' ', '⇈', '↑', '↗', '→', '↘', '↓', '⇊'}; // standard arrows
    private static final char[] TREND_SET_2 = {' ', '⮅', '⭡', '⭧', '⭢', '⭨', '⭣', '⮇'}; // triangle arrows (not all chars on watch)


    public static char getTrendChar(Trend trend) {
        int idx = trend == null ? 0 : trend.ordinal();
        return TREND_SET[idx];
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

    public static String getGlucoseUnitsStr(boolean isUnitConv) {
        return isUnitConv ? GLUCOSE_UNITS_MMOLL : GLUCOSE_UNITS_MGDL;
    }

    public static String formatBgValueString(int value, Trend trend, boolean isUnitConversion) {
        return (isUnitConversion ? convertGlucoseToMmolLStr(value) : Integer.toString(value))
                + getTrendChar(trend);
    }

    public static String formatBgDeltaString(int valueDiff, long timeDiff, boolean isUnitConversion) {
        if (timeDiff < 0) {
            return StringUtils.EMPTY_STRING;
        }

        StringBuffer str = new StringBuffer();
        if (valueDiff >= 0) {
            str.append("+");
        }
        if (isUnitConversion) {
            str.append(convertGlucoseToMmolL2Str(valueDiff));
        } else {
            str.append(valueDiff);
        }
        if (timeDiff <= CommonConstants.HOUR_IN_MILLIS) {
            str.append(" ")
                    .append(timeDiff/CommonConstants.MINUTE_IN_MILLIS)
                    .append("'");
        }
        return str.toString();
    }

    public static String formatBgDeltaForComplication(int valueDiff, long timeDiff, boolean isUnitConversion, long noDataThreshold) {
        if (timeDiff < 0 || timeDiff > CommonConstants.HOUR_IN_MILLIS) {
            return StringUtils.EMPTY_STRING;
        }
        if (timeDiff < noDataThreshold) {
            return "(!)";
        }

        StringBuffer str = new StringBuffer();
        if (valueDiff >= 0) {
            str.append("+");
        }
        if (isUnitConversion) {
            str.append(convertGlucoseToMmolL2Str(valueDiff));
        } else {
            str.append(valueDiff);
        }
        return str.toString();
    }

    public static Trend slopeArrowToTrend(String slopeArrow) {
        if (slopeArrow == null) {
            return null;
        }
        slopeArrow = slopeArrow.trim();
        if ("".equals(slopeArrow)) {
            return Trend.UNKNOWN;
        } else if ("⇈".equals(slopeArrow)) {
            return Trend.UP_FAST;
        } else if ("↑".equals(slopeArrow)) {
            return Trend.UP;
        } else if ("↗".equals(slopeArrow)) {
            return Trend.UP_SLOW;
        } else if ("→".equals(slopeArrow)) {
            return Trend.FLAT;
        } else if ("↘".equals(slopeArrow)) {
            return Trend.DOWN_SLOW;
        } else if ("↓".equals(slopeArrow)) {
            return Trend.DOWN;
        } else if ("⇊".equals(slopeArrow)) {
            return Trend.DOWN_FAST;
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Unknown slope: " + slopeArrow
                        + ": " + StringUtils.toHexString(slopeArrow.getBytes(StandardCharsets.UTF_8)));
            }
            return Trend.UNKNOWN;
        }
    }
}
