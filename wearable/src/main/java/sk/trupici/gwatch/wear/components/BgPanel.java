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

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BorderType;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.data.Trend;
import sk.trupici.gwatch.wear.util.BorderUtils;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.StringUtils;
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
public class BgPanel extends BroadcastReceiver implements ComponentPanel {

    public static final String LOG_TAG = BgPanel.class.getSimpleName();

    public static final int CONFIG_ID = 13;

    public static final String PREF_BKG_COLOR = "bg_color_background";
    public static final String PREF_CRITICAL_COLOR = "bg_color_critical";
    public static final String PREF_WARN_COLOR = "bg_color_warn";
    public static final String PREF_IN_RANGE_COLOR = "bg_color_in_range";
    public static final String PREF_NO_DATA_COLOR = "bg_color_no_data";

    public static final String PREF_BORDER_COLOR = "bg_border_color";
    public static final String PREF_BORDER_TYPE = "bg_border_type";

    final private int refScreenWidth;
    final private int refScreenHeight;

    final private WatchfaceConfig watchfaceConfig;

    private RectF sizeFactors;
    private float topOffset;

    private Rect bounds;
    private TextPaint paint;
    private String bgLine1;
    private String bgLine2;

    private BgData lastBgData;

    boolean isUnitConversion;

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

    public BgPanel(int screenWidth, int screenHeight, WatchfaceConfig watchfaceConfig) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
        this.watchfaceConfig = watchfaceConfig;
        lastBgData = new BgData(0, 0, 0 , 0, Trend.UNKNOWN);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        onDataUpdate(context, BgData.fromBundle(extras));
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        RectF bounds = watchfaceConfig.getBgPanelBounds(context);
        sizeFactors = new RectF(
                bounds.left / (float)refScreenWidth,
                bounds.top / (float)refScreenHeight,
                bounds.right / (float)refScreenWidth,
                bounds.bottom / (float)refScreenHeight
        );
        topOffset = watchfaceConfig.getBgPanelTopOffset(context);
        Log.w(LOG_TAG, "Rect: " + sizeFactors + ", offset: " + topOffset);

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
        Log.d(LOG_TAG, "onConfigChanged: ");

        isUnitConversion = sharedPrefs.getBoolean(CommonConstants.PREF_IS_UNIT_CONVERSION,
                context.getResources().getBoolean(R.bool.def_bg_is_unit_conversion));

        // thresholds
        hyperThreshold = sharedPrefs.getInt(CommonConstants.PREF_HYPER_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hyper));
        highThreshold = sharedPrefs.getInt(CommonConstants.PREF_HIGH_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_high));
        lowThreshold = sharedPrefs.getInt(CommonConstants.PREF_LOW_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_low));
        hypoThreshold = sharedPrefs.getInt(CommonConstants.PREF_HYPO_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hypo));
        noDataThreshold = sharedPrefs.getInt(CommonConstants.PREF_NO_DATA_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_no_data));

        // colors
        backgroundColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BKG_COLOR, context.getColor(R.color.def_bg_background));
        criticalColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_CRITICAL_COLOR, context.getColor(R.color.def_bg_critical));
        warnColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_WARN_COLOR, context.getColor(R.color.def_bg_warn));
        inRangeColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_IN_RANGE_COLOR, context.getColor(R.color.def_bg_in_range));
        noDataColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_NO_DATA_COLOR, context.getColor(R.color.def_bg_no_data));

        // border
        borderColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_COLOR, context.getColor(R.color.def_bg_border_color));
        borderType = BorderType.getByNameOrDefault(sharedPrefs.getString(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_TYPE, context.getString(R.string.def_bg_border_type)));

        onDataUpdate(context, lastBgData);
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
        long bgTimeDiff = System.currentTimeMillis() - lastBgData.getTimestamp();
        if (isAmbientMode) {
            paint.setColor(getAmbientRangedColor(lastBgData.getValue(), bgTimeDiff));
//            paint.setAntiAlias(false);
        } else {
            paint.setColor(getRangedColor(lastBgData.getValue(), bgTimeDiff));
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

//        if (!isAmbientMode) {
//            drawRangeIndicator(canvas);
//        }
    }

    // range indicator - EXPERIMENTAL
    private void drawRangeIndicator(Canvas canvas) {
        Paint indicatorPaint = new Paint();
        int padding = 10;
        float height = (bounds.height() - padding) / 3f - padding;
        RectF indicatorBounds = new RectF();
        indicatorBounds.left = bounds.left + 6;
        indicatorBounds.right = indicatorBounds.left + 10;
        indicatorBounds.bottom = bounds.bottom - padding;
        indicatorBounds.top = indicatorBounds.bottom - height;
        boolean isCritical = isBgCritical(lastBgData.getValue());
        paintIndicatorBar(canvas, indicatorPaint, indicatorBounds, isCritical ? criticalColor : criticalColor & 0x60FFFFFF);
        indicatorBounds.bottom = indicatorBounds.top - padding;
        indicatorBounds.top = indicatorBounds.bottom - height;
        boolean isInRange = !isCritical && isBgInRange(lastBgData.getValue());
        paintIndicatorBar(canvas, indicatorPaint, indicatorBounds, isInRange ? inRangeColor : inRangeColor & 0x60FFFFFF);
        indicatorBounds.top = bounds.top + padding;
        indicatorBounds.bottom = indicatorBounds.top + height;
        paintIndicatorBar(canvas, indicatorPaint, indicatorBounds, !isInRange && !isCritical ? warnColor : warnColor & 0x60FFFFFF);
    }

    private void paintIndicatorBar(Canvas canvas, Paint paint, RectF bounds, int fillColor) {
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
            return Color.LTGRAY;
        } else if (bgTimeDiff > noDataThreshold * CommonConstants.SECOND_IN_MILLIS) {
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

    private int getAmbientRangedColor(int bgValue, long bgTimeDiff) {
        return (bgTimeDiff > noDataThreshold * CommonConstants.SECOND_IN_MILLIS) ? Color.DKGRAY : Color.LTGRAY;
    }

    private void onDataUpdate(Context context, BgData bgData) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onDataUpdate: " + bgData.toString());
        }

        boolean invalidData = bgData.getValue() == 0;

        if (bgData.getTimestampDiff() < 0) {
            return; // historical data
        }

        long bgTimestampDiff = bgData.getTimestampDiff() / CommonConstants.MINUTE_IN_MILLIS; // to minutes
        if (bgTimestampDiff > CommonConstants.DAY_IN_MINUTES) {
            invalidData = true;
        }

        if (invalidData) {
            bgLine1 = "--";
            bgLine2 = "--";
        } else {
            if (isUnitConversion) {
                bgLine1 = UiUtils.convertGlucoseToMmolLStr(bgData.getValue()) + UiUtils.getTrendChar(bgData.getTrend());
                bgLine2 = bgTimestampDiff < 0 ? StringUtils.EMPTY_STRING : "Δ " + UiUtils.convertGlucoseToMmolL2Str(bgData.getValueDiff());
            } else {
                bgLine1 = Integer.toString(bgData.getValue()) + UiUtils.getTrendChar(bgData.getTrend());
                bgLine2 = bgTimestampDiff < 0 ? StringUtils.EMPTY_STRING : "Δ " + bgData.getValueDiff();
            }
        }

        Log.d(LOG_TAG, "onDataUpdate: " + bgLine1 + " / " + bgLine2);

        this.lastBgData = bgData;
    }
}
