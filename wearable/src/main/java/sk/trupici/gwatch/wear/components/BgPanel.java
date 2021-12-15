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
package sk.trupici.gwatch.wear.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.text.TextPaint;
import android.util.Log;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.BorderType;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.data.Trend;
import sk.trupici.gwatch.wear.services.BgDataListenerService;
import sk.trupici.gwatch.wear.util.BorderUtils;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.UiUtils;

import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_DASH_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_DOT_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_GAP_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_RING_RADIUS;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_ROUND_RECT_RADIUS;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_WIDTH;

/**
 * Component showing BG value and related info (trend, delta, etc...)
 */
public class BgPanel extends BroadcastReceiver implements ComponentPanel{

    public static final String LOG_TAG = CommonConstants.LOG_TAG;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        onDataUpdate(
                extras.getInt(BgDataListenerService.EXTRA_BG_VALUE, 0),
                extras.getLong(BgDataListenerService.EXTRA_BG_TIMESTAMP, 0),
                (Trend)extras.get(BgDataListenerService.EXTRA_BG_TREND),
                extras.getLong(BgDataListenerService.EXTRA_BG_RECEIVEDAT, 0)
        );
    }

    public static final String PREF_IS_UNIT_CONVERSION = AnalogWatchfaceConfig.PREF_PREFIX + "bg_is_unit_conversion";
    public static final String PREF_SAMPLE_PERIOD_MIN = AnalogWatchfaceConfig.PREF_PREFIX + "bg_sample_period";

    public static final String PREF_BKG_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "bg_color_background";
    public static final String PREF_CRITICAL_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "bg_color_critical";
    public static final String PREF_WARN_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "bg_color_warn";
    public static final String PREF_IN_RANGE_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "bg_color_in_range";
    public static final String PREF_NO_DATA_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "bg_color_no_data";

    public static final String PREF_HYPER_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_hyper";
    public static final String PREF_HIGH_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_high";
    public static final String PREF_LOW_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_low";
    public static final String PREF_HYPO_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_hypo";
    public static final String PREF_NO_DATA_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_no_data";

    public static final String PREF_BORDER_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "bg_border_color";
    public static final String PREF_BORDER_TYPE = AnalogWatchfaceConfig.PREF_PREFIX + "bg_border_type";

    // TODO more sets are available in standard unicode font
    private static final char[] TREND_SET_1 = {' ', '⇈', '↑', '↗', '→', '↘', '↓', '⇊'}; // standard arrows
    private static final char[] TREND_SET_2 = {' ', '⮅', '⭡', '⭧', '⭢', '⭨', '⭣', '⮇'}; // triangle arrows (unknown chars on watch)

    private RectF sizeFactors;
    private float topOffset;

    private Rect bounds;
    private TextPaint paint;
    private String bgLine1;
    private String bgLine2;

    private int bgValue = 0;
    private long bgTimestamp = 0;

    boolean isUnitConversion;
    int samplePeriod;

    final private int refScreenWidth;
    final private int refScreenHeight;

    private int backgroundColor;
    private int criticalColor;
    private int warnColor;
    private int inRangeColor;
    private int noDataColor;

    private int hyperThreshold;
    private int highThreshold;
    private int lowThreshold;
    private int hypoThreshold;
    private int noDataThreshold;

    private int borderColor;
    private BorderType borderType;

    public BgPanel(int screenWidth, int screenHeight) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        sizeFactors = new RectF(
                context.getResources().getDimension(R.dimen.layout_bg_pnel_left) / (float)refScreenWidth,
                context.getResources().getDimension(R.dimen.layout_bg_panel_top) / (float)refScreenHeight,
                context.getResources().getDimension(R.dimen.layout_bg_panel_right) / (float)refScreenWidth,
                context.getResources().getDimension(R.dimen.layout_bg_panel_bottom) / (float)refScreenHeight
        );
        topOffset = context.getResources().getDimension(R.dimen.layout_bg_panel_top_offset);
        Log.w(CommonConstants.LOG_TAG, "Rect: " + sizeFactors);

        paint = new TextPaint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        int left = (int) (sizeFactors.left * width);
        int top = (int) (sizeFactors.top * height);
        int right = (int) (sizeFactors.right * width);
        int bottom = (int) (sizeFactors.bottom * height);
        bounds = new Rect(left, top, right, bottom);
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {

        isUnitConversion = sharedPrefs.getBoolean(PREF_IS_UNIT_CONVERSION, context.getResources().getBoolean(R.bool.def_bg_is_unit_conversion));
        samplePeriod = sharedPrefs.getInt(PREF_SAMPLE_PERIOD_MIN, context.getResources().getInteger(R.integer.def_bg_sample_period));

        // thresholds
        hyperThreshold = sharedPrefs.getInt(PREF_HYPER_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hyper));
        highThreshold = sharedPrefs.getInt(PREF_HIGH_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_high));
        lowThreshold = sharedPrefs.getInt(PREF_LOW_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_low));
        hypoThreshold = sharedPrefs.getInt(PREF_HYPO_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hypo));
        noDataThreshold = sharedPrefs.getInt(PREF_NO_DATA_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_no_data));

        // colors
        backgroundColor = sharedPrefs.getInt(PREF_BKG_COLOR, context.getColor(R.color.def_bg_background));
        criticalColor = sharedPrefs.getInt(PREF_CRITICAL_COLOR, context.getColor(R.color.def_bg_critical));
        warnColor = sharedPrefs.getInt(PREF_WARN_COLOR, context.getColor(R.color.def_bg_warn));
        inRangeColor = sharedPrefs.getInt(PREF_IN_RANGE_COLOR, context.getColor(R.color.def_bg_in_range));
        noDataColor = sharedPrefs.getInt(PREF_NO_DATA_COLOR, context.getColor(R.color.def_bg_no_data));

        // border
        borderColor = sharedPrefs.getInt(PREF_BORDER_COLOR, context.getColor(R.color.def_bg_border_color));
        borderType = BorderType.getByNameOrDefault(sharedPrefs.getString(PREF_BORDER_TYPE, context.getString(R.string.def_bg_border_type)));
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {

    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        if (!isAmbientMode) {
            // draw background
            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            if (BorderUtils.isBorderRounded(borderType)) {
                canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
            } else if (BorderUtils.isBorderRing(borderType)) {
                canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        BORDER_RING_RADIUS, BORDER_RING_RADIUS, paint);
            } else {
                canvas.drawRect(bounds, paint);
            }

            // draw border
            if (borderType != BorderType.NONE) {
                paint.setColor(borderColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(BORDER_WIDTH);
                if (BorderUtils.getBorderDrawableStyle(borderType) == ComplicationDrawable.BORDER_STYLE_DASHED) {
                    if (BorderUtils.isBorderDotted(borderType)) {
                        paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DOT_LEN, BORDER_GAP_LEN}, 0f));
                    } else {
                        paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DASH_LEN, BORDER_GAP_LEN}, 0f));
                    }
                }
                if (BorderUtils.isBorderRounded(borderType)) {
                    canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                            BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
                } else if (BorderUtils.isBorderRing(borderType)) {
                    canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                            BORDER_RING_RADIUS, BORDER_RING_RADIUS, paint);
                } else {
                    canvas.drawRect(bounds, paint);
                }
            }
        }

        // draw bg value

        // line 1
        float x = bounds.left + bounds.width() / 2f; // text will be centered around
        float top = bounds.top + topOffset;
        float height = bounds.height() - topOffset;
        long bgTimeDiff = System.currentTimeMillis() / 60000 - bgTimestamp;
        if (isAmbientMode) {
            paint.setColor(Color.LTGRAY);
//            paint.setAntiAlias(false);
        } else {
            paint.setColor(getRangedColor(bgValue, bgTimeDiff));
//            paint.setAntiAlias(true);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(height / 2f);
        paint.setFakeBoldText(true);
        canvas.drawText(bgLine1 != null ? bgLine1 : ComplicationConfig.NO_DATA_TEXT,
                x, top + height / 2f, paint);

        // line 2
        paint.setTextSize(height / 3f);
        paint.setFakeBoldText(false);
        canvas.drawText(bgLine2 != null ? bgLine2 : ComplicationConfig.NO_DATA_TEXT,
                x, bounds.bottom - height / 10f, paint);


        // range indicator - EXPERIMENTAL
        if (!isAmbientMode) {
//            Log.d(LOG_TAG, "onDraw: " + bounds);
            Paint indicatorPaint = new Paint();
            int padding = 10;
            height = (bounds.height() - padding) / 3f - padding;
            RectF indicatorBounds = new RectF();
            indicatorBounds.left = bounds.left + 6;
            indicatorBounds.right = indicatorBounds.left + 10;
            indicatorBounds.bottom = bounds.bottom - padding;
            indicatorBounds.top = indicatorBounds.bottom - height;
            boolean isCritical = isBgCritical(bgValue);
            paintIndicatorBar(canvas, indicatorPaint, indicatorBounds, isCritical ? criticalColor : criticalColor & 0x60FFFFFF);
            indicatorBounds.bottom = indicatorBounds.top - padding;
            indicatorBounds.top = indicatorBounds.bottom - height;
            boolean isInRange = !isCritical && isBgInRange(bgValue);
            paintIndicatorBar(canvas, indicatorPaint, indicatorBounds, isInRange ? inRangeColor : inRangeColor & 0x60FFFFFF);
            indicatorBounds.top = bounds.top + padding;
            indicatorBounds.bottom = indicatorBounds.top + height;
            paintIndicatorBar(canvas, indicatorPaint, indicatorBounds, !isInRange && !isCritical ? warnColor : warnColor & 0x60FFFFFF);
        }
    }

    private void paintIndicatorBar(Canvas canvas, Paint paint, RectF bounds, int fillColor) {
//        Log.d(LOG_TAG, "paintIndicatorBar: " + bounds);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        canvas.drawRoundRect(bounds, 10f, 10f, paint);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(fillColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bounds, 10f, 10f, paint);
    }

    private boolean isBgCritical(int bgValue) {
        return bgValue >= hyperThreshold || bgValue <= hypoThreshold;
    }

    private boolean isBgInRange(int bgValue) {
        return lowThreshold < bgValue && bgValue < highThreshold;
    }

    private int getRangedColor(int bgValue, long bgTimeDiff) {
        if (bgValue == 0) {
            return Color.WHITE;
        }
        if (bgValue > 0 && bgTimeDiff > noDataThreshold) {
            return noDataColor;
        }
        if (bgValue <= lowThreshold) {
            return bgValue <= hypoThreshold ? criticalColor : warnColor;
        } else if (bgValue >= highThreshold) {
            return bgValue >= hyperThreshold ? criticalColor : warnColor;
        } else {
            return inRangeColor;
        }
    }

    public void onDataUpdate(int bgValue, long bgTimestamp, Trend trend, long receivedAt) {

        if (bgTimestamp == 0) {
            bgTimestamp = receivedAt != 0 ? receivedAt : System.currentTimeMillis();
        }

        int bgDiff = this.bgValue == 0 ? 0 : bgValue - this.bgValue;
        int bgTimestampDiff = this.bgTimestamp == 0 ? 0 : (int)(bgTimestamp-this.bgTimestamp) / CommonConstants.MINUTE_IN_MILLIS; // to minutes
        if (bgTimestampDiff > CommonConstants.DAY_IN_MINUTES) {
            bgTimestampDiff = -1;
        }

        if (trend == null || trend == Trend.UNKNOWN) {
            trend = bgTimestampDiff <= 0 ? Trend.FLAT : calcTrend(bgDiff, samplePeriod);
        }
        char trendArrow = TREND_SET_1[trend.ordinal()];

        if (isUnitConversion) {
            bgLine1 = UiUtils.convertGlucoseToMmolLStr(bgValue) + trendArrow;
            bgLine2 = bgTimestampDiff < 0 ? "" : "Δ " + UiUtils.convertGlucoseToMmolL2Str(bgDiff);
        } else {
            bgLine1 = "" + bgValue + trendArrow;
            bgLine2 = bgTimestampDiff < 0 ? "" : "Δ " + bgDiff;
        }

        Log.d(CommonConstants.LOG_TAG, "onDataUpdate: " + bgLine1 + " / " + bgLine2);

        this.bgValue = bgValue;
        this.bgTimestamp = bgTimestamp;
    }

    private Trend calcTrend(int glucoseDelta, int sampleTimeDelta) {
        if (glucoseDelta < -2 * sampleTimeDelta) {
            return Trend.DOWN;
        } else if (glucoseDelta < -sampleTimeDelta) {
            return Trend.DOWN_SLOW;
        } else if (glucoseDelta < sampleTimeDelta) {
            return Trend.FLAT;
        } else if (glucoseDelta < 2 * sampleTimeDelta) {
            return Trend.UP_SLOW;
        } else {
            return Trend.UP;
        }
    }
}
