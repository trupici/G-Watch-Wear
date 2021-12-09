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

import android.content.Context;
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

import java.util.function.BiConsumer;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.BorderType;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.DumpUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_PREFIX;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_DASH_LEN;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_DOT_LEN;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_GAP_LEN;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_RING_RADIUS;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_ROUND_RECT_RADIUS;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_WIDTH;

/**
 * Component showing BG value and related info (trend, delta, etc...)
 */
public class BgPanel implements ComponentPanel {

    public static final String LOG_TAG = CommonConstants.LOG_TAG;

    public interface BgValueCallback extends BiConsumer<Integer, Long> { }

    public static final String PREF_IS_UNIT_CONVERSION = AnalogWatchfaceConfig.PREF_PREFIX + "is_unit_conversion";
    public static final String PREF_SAMPLE_PERIOD_MIN = AnalogWatchfaceConfig.PREF_PREFIX + "sample_period";

    // TODO more sets are available in standard unicode font
    private static final char[] TREND_SET_1 = {' ', '⇈', '↑', '↗', '→', '↘', '↓', '⇊'}; // standard arrows
    private static final char[] TREND_SET_2 = {' ', '⮅', '⭡', '⭧', '⭢', '⭨', '⭣', '⮇'}; // triangle arrows (unknown chars on watch)

    private static final boolean DEF_IS_UNIT_CONVERSION_VALUE = false; // mg/dl
    // 5 minutes is common interval to update bg value in the most of CGM apps
    private static final int DEF_SAMPLE_PERIOD_MIN = 5;

    private RectF sizeFactors;

    private Rect bounds;
    private TextPaint paint;
    private String bgLine1;
    private String bgLine2;

    private int bgValue = 0;
    private long bgTimestamp = 0;

    boolean isUnitConversion = DEF_IS_UNIT_CONVERSION_VALUE;
    int samplePeriod = DEF_SAMPLE_PERIOD_MIN;

    private ComponentsConfig bottomComplSettings;

    private BgValueCallback callback;

    final private int refScreenWidth;
    final private int refScreenHeight;

    public BgPanel(int screenWidth, int screenHeight) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        sizeFactors = new RectF(
                context.getResources().getDimension(R.dimen.layout_bottom_compl_left) / (float)refScreenWidth,
                context.getResources().getDimension(R.dimen.layout_bottom_compl_top) / (float)refScreenHeight,
                context.getResources().getDimension(R.dimen.layout_bottom_compl_right) / (float)refScreenWidth,
                context.getResources().getDimension(R.dimen.layout_bottom_compl_bottom) / (float)refScreenHeight
        );
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
        bottomComplSettings = new ComponentsConfig();
        bottomComplSettings.load(sharedPrefs, PREF_PREFIX + ComplicationConfig.BOTTOM_PREFIX);

        isUnitConversion = sharedPrefs.getBoolean(PREF_IS_UNIT_CONVERSION, DEF_IS_UNIT_CONVERSION_VALUE);
        samplePeriod = sharedPrefs.getInt(PREF_SAMPLE_PERIOD_MIN, DEF_SAMPLE_PERIOD_MIN);
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {

    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        if (isAmbientMode) {
            // draw background
            paint.setColor(bottomComplSettings.getBackgroundColor());
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            if (bottomComplSettings.isBorderRounded()) {
                canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
            } else if (bottomComplSettings.isBorderRing()) {
                canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        BORDER_RING_RADIUS, BORDER_RING_RADIUS, paint);
            } else {
                canvas.drawRect(bounds, paint);
            }

            // draw border
            if (bottomComplSettings.getBorderType() != BorderType.NONE) {
                paint.setColor(bottomComplSettings.getBorderColor());
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(BORDER_WIDTH);
                if (bottomComplSettings.getBorderDrawableStyle() == ComplicationDrawable.BORDER_STYLE_DASHED) {
                    if (bottomComplSettings.isBorderDotted()) {
                        paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DOT_LEN, BORDER_GAP_LEN}, 0f));
                    } else {
                        paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DASH_LEN, BORDER_GAP_LEN}, 0f));
                    }
                }
                if (bottomComplSettings.isBorderRounded()) {
                    canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                            BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
                } else if (bottomComplSettings.isBorderRing()) {
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
        if (isAmbientMode) {
            paint.setColor(Color.LTGRAY);
            paint.setAntiAlias(!isAmbientMode);
        } else {
            paint.setColor(bottomComplSettings.getDataColor());
            paint.setAntiAlias(!isAmbientMode);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() / 2f);
        paint.setFakeBoldText(true);

        // line 2
        canvas.drawText(bgLine1 != null ? bgLine1 : ComplicationConfig.NO_DATA_TEXT,
                x, bounds.top + bounds.height() / 2f, paint);
        paint.setTextSize(bounds.height() / 3f);
        paint.setFakeBoldText(false);
        canvas.drawText(bgLine2 != null ? bgLine2 : ComplicationConfig.NO_DATA_TEXT,
                x, bounds.bottom - bounds.height() / 10f, paint);
    }

    public void onDataUpdate(Context context, byte[] bgData) {
        Log.d(CommonConstants.LOG_TAG, DumpUtils.dumpData(bgData, bgData.length));

        GlucosePacket glucosePacket = GlucosePacket.of(bgData);
        if (glucosePacket != null) {
            Log.d(CommonConstants.LOG_TAG, glucosePacket.toText(context, ""));

            int bgDiff = bgValue == 0 ? 0 : (int)glucosePacket.getGlucoseValue() - bgValue;
            int bgTimestampDiff = bgTimestamp == 0 ? 0 : (int)(glucosePacket.getTimestamp()-bgTimestamp) / 60000; // to minutes
            if (bgTimestampDiff > 24 * 60) {
                bgTimestampDiff = -1;
            }
            GlucosePacket.Trend trend = glucosePacket.getTrend();
            if (trend == null || trend == GlucosePacket.Trend.UNKNOWN) {
                trend = bgTimestampDiff <= 0 ? GlucosePacket.Trend.FLAT : calcTrend(bgDiff, samplePeriod);
            }
            char trendArrow = TREND_SET_1[trend.ordinal()];

            if (isUnitConversion) {
                bgLine1 = UiUtils.convertGlucoseToMmolLStr(glucosePacket.getGlucoseValue()) + trendArrow;
                bgLine2 = bgTimestampDiff < 0 ? "" : "Δ " + UiUtils.convertGlucoseToMmolL2Str(bgDiff);
            } else {
                bgLine1 = "" + glucosePacket.getGlucoseValue() + trendArrow;
                bgLine2 = bgTimestampDiff < 0 ? "" : "Δ " + bgDiff;
            }

            Log.d(CommonConstants.LOG_TAG, "bgValue: " + bgLine1 + " / " + bgLine2);

            bgValue = glucosePacket.getGlucoseValue();
            bgTimestamp = glucosePacket.getTimestamp();

            if (callback != null) {
                callback.accept(bgValue, bgTimestamp);
            }
        }
    }

    public void setBgValueCallback(BgValueCallback callback) {
        this.callback = callback;
    }

    private GlucosePacket.Trend calcTrend(int glucoseDelta, int sampleTimeDelta) {
        if (glucoseDelta < -2 * sampleTimeDelta) {
            return GlucosePacket.Trend.DOWN;
        } else if (glucoseDelta < -sampleTimeDelta) {
            return GlucosePacket.Trend.DOWN_SLOW;
        } else if (glucoseDelta < sampleTimeDelta) {
            return GlucosePacket.Trend.FLAT;
        } else if (glucoseDelta < 2 * sampleTimeDelta) {
            return GlucosePacket.Trend.UP_SLOW;
        } else {
            return GlucosePacket.Trend.UP;
        }
    }
}
