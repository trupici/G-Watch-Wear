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
import android.graphics.Bitmap;
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

import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BorderType;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.data.Trend;
import sk.trupici.gwatch.wear.util.BgUtils;
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
public class BgPanel extends BroadcastReceiver implements ComponentPanel {

    public static final String LOG_TAG = BgPanel.class.getSimpleName();

    public static final int CONFIG_ID = 13;

    public static final String PREF_BKG_COLOR = "bg_color_background";
    public static final String PREF_HYPO_COLOR = "bg_color_hypo";
    public static final String PREF_LOW_COLOR = "bg_color_low";
    public static final String PREF_IN_RANGE_COLOR = "bg_color_in_range";
    public static final String PREF_HIGH_COLOR = "bg_color_high";
    public static final String PREF_HYPER_COLOR = "bg_color_hyper";
    public static final String PREF_NO_DATA_COLOR = "bg_color_no_data";

    public static final String PREF_BORDER_COLOR = "bg_border_color";
    public static final String PREF_BORDER_TYPE = "bg_border_type";

    private static final int BG_INDICATOR_INACTIVE_MASK = 0x30FFFFFF;

    final private int refScreenWidth;
    final private int refScreenHeight;

    final private WatchfaceConfig watchfaceConfig;

    private RectF sizeFactors;
    private float topOffset;
    private float bottomOffset;

    private Rect bounds;
    private Paint bkgPaint;
    private TextPaint textPaint;
    private String bgLine1;
    private String bgLine2;

    private BgData lastBgData;
    private long lastBgUpdate;

    boolean isUnitConversion;

    private int backgroundColor;
    private int hyperColor;
    private int highColor;
    private int lowColor;
    private int hypoColor;
    private int inRangeColor;
    private int noDataColor;

    private int hyperThreshold;
    private int highThreshold;
    private int lowThreshold;
    private int hypoThreshold;
    private int noDataThreshold;

    private int borderColor;
    private BorderType borderType;

    private Paint indicatorPaint;
    private boolean showBgIndicator;
    private RectF lowIndicatorBounds;
    private RectF inRangeIndicatorBounds;
    private RectF highIndicatorBounds;

    private RectF lowIndicatorSizeFactors;
    private RectF inRangeIndicatorSizeFactors;
    private RectF highIndicatorSizeFactors;

    private Paint paint;
    private Paint erasePaint;
    private Paint ambientPaint;
    private Bitmap bkgBitmap;

    // precalculated text coordinates
    private float xLine;
    private float yLine1;
    private float yLine2;
    private float paddedHeight;

    public BgPanel(int screenWidth, int screenHeight, WatchfaceConfig watchfaceConfig) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
        this.watchfaceConfig = watchfaceConfig;
        lastBgData = new BgData(0, 0, 0 , 0, Trend.UNKNOWN);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CommonConstants.BG_RECEIVER_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                onDataUpdate(BgData.fromBundle(extras));
            }
        } else if (CommonConstants.REMOTE_CONFIG_ACTION.equals(intent.getAction())) {
            onConfigChanged(context, PreferenceManager.getDefaultSharedPreferences(context));
        } else {
            Log.e(LOG_TAG, "onReceive: unsupported intent: " + intent.getAction());
        }
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
        bottomOffset = watchfaceConfig.getBgPanelBottomOffset(context);
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Rect: " + sizeFactors + ", offset: " + topOffset + ", " + bottomOffset);
        }

        xLine = bounds.left + bounds.width() / 2f; // text will be centered around
        paddedHeight = bounds.height() - topOffset - bottomOffset;
        yLine1 = bounds.top + topOffset + paddedHeight / 2f;
        yLine2 = bounds.bottom - bottomOffset - paddedHeight / 10f;


        textPaint = UiUtils.createTextPaint();
        textPaint.setTextAlign(Paint.Align.CENTER);

        bkgPaint = UiUtils.createPaint();

        indicatorPaint = UiUtils.createPaint();
        indicatorPaint.setStyle(Paint.Style.FILL);

        paint = UiUtils.createPaint();
        erasePaint = UiUtils.createErasePaint();
        ambientPaint = UiUtils.createAmbientPaint();

        showBgIndicator = watchfaceConfig.showBgPanelIndicator(context);
        if (showBgIndicator) {
            lowIndicatorBounds = watchfaceConfig.getBgPanelLowIndicatorBounds(context);
            lowIndicatorSizeFactors = new RectF(
                    lowIndicatorBounds.left / (float) refScreenWidth,
                    lowIndicatorBounds.top / (float) refScreenHeight,
                    lowIndicatorBounds.right / (float) refScreenWidth,
                    lowIndicatorBounds.bottom / (float) refScreenHeight
            );
            inRangeIndicatorBounds = watchfaceConfig.getBgPanelInRangeIndicatorBounds(context);
            inRangeIndicatorSizeFactors = new RectF(
                    inRangeIndicatorBounds.left / (float) refScreenWidth,
                    inRangeIndicatorBounds.top / (float) refScreenHeight,
                    inRangeIndicatorBounds.right / (float) refScreenWidth,
                    inRangeIndicatorBounds.bottom / (float) refScreenHeight
            );
            highIndicatorBounds = watchfaceConfig.getBgPanelHighIndicatorBounds(context);
            highIndicatorSizeFactors = new RectF(
                    highIndicatorBounds.left / (float) refScreenWidth,
                    highIndicatorBounds.top / (float) refScreenHeight,
                    highIndicatorBounds.right / (float) refScreenWidth,
                    highIndicatorBounds.bottom / (float) refScreenHeight
            );
        }
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        bounds = new Rect(
                (int) (sizeFactors.left * width),
                (int) (sizeFactors.top * height),
                (int) (sizeFactors.right * width),
                (int) (sizeFactors.bottom * height));

        float yScale = (float) height / refScreenHeight;
        topOffset = watchfaceConfig.getBgPanelTopOffset(context) * yScale;
        bottomOffset = watchfaceConfig.getBgPanelBottomOffset(context) * yScale;

        xLine = bounds.left + bounds.width() / 2f; // text will be centered around
        paddedHeight = bounds.height() - topOffset - bottomOffset;
        yLine1 = bounds.top + topOffset + paddedHeight / 2f;
        yLine2 = bounds.bottom - bottomOffset - paddedHeight / 10f;


        bkgBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        drawBackgroundAndBorder();

        if (showBgIndicator) {
            lowIndicatorBounds = new RectF(
                    lowIndicatorSizeFactors.left * width,
                    lowIndicatorSizeFactors.top * height,
                    lowIndicatorSizeFactors.right * width,
                    lowIndicatorSizeFactors.bottom * height);

            inRangeIndicatorBounds = new RectF(
                    inRangeIndicatorSizeFactors.left * width,
                    inRangeIndicatorSizeFactors.top * height,
                    inRangeIndicatorSizeFactors.right * width,
                    inRangeIndicatorSizeFactors.bottom * height);

            highIndicatorBounds = new RectF(
                    highIndicatorSizeFactors.left * width,
                    highIndicatorSizeFactors.top * height,
                    highIndicatorSizeFactors.right * width,
                    highIndicatorSizeFactors.bottom * height);
        }
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
        backgroundColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BKG_COLOR, context.getColor(R.color.def_bg_background_color));
        hypoColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_HYPO_COLOR, context.getColor(R.color.def_bg_hypo_color));
        lowColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_LOW_COLOR, context.getColor(R.color.def_bg_low_color));
        inRangeColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_IN_RANGE_COLOR, context.getColor(R.color.def_bg_in_range_color));
        highColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_HIGH_COLOR, context.getColor(R.color.def_bg_high_color));
        hyperColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_HYPER_COLOR, context.getColor(R.color.def_bg_hyper_color));
        noDataColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_NO_DATA_COLOR, context.getColor(R.color.def_bg_no_data_color));

        // border
        borderColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_COLOR, context.getColor(R.color.def_bg_border_color));
        borderType = BorderType.getByNameOrDefault(sharedPrefs.getString(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_TYPE, context.getString(R.string.def_bg_border_type)));

        drawBackgroundAndBorder();
        onDataUpdate(lastBgData);
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {

    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
         if (isAmbientMode) {
             textPaint.setColor(getAmbientRangedColor(isNoData()));
        } else {
            if (lastBgData.getValue() > 0 && System.currentTimeMillis() - lastBgUpdate > CommonConstants.MINUTE_IN_MILLIS) {
                onDataUpdate(lastBgData);
            }
            canvas.drawBitmap(bkgBitmap, bounds.left, bounds.top, paint);
            textPaint.setColor(getRangedColor(isNoData()));
        }

        // line 1
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(paddedHeight / 2f);
        textPaint.setFakeBoldText(true);
        canvas.drawText(bgLine1 != null ? bgLine1 : ComplicationConfig.NO_DATA_TEXT, xLine, yLine1, textPaint);

        // line 2
        textPaint.setTextSize(paddedHeight / 3f);
        textPaint.setFakeBoldText(false);
        canvas.drawText(bgLine2 != null ? bgLine2 : ComplicationConfig.NO_DATA_TEXT, xLine, yLine2, textPaint);

        if (showBgIndicator) {
            Boolean isNoData = isNoData();
            Paint paint = isAmbientMode ? ambientPaint : indicatorPaint;
            drawIndicatorBar(canvas, paint, lowIndicatorBounds, getLowIndicatorColor(isNoData));
            drawIndicatorBar(canvas, paint, inRangeIndicatorBounds, getInRangeIndicatorColor(isNoData));
            drawIndicatorBar(canvas, paint, highIndicatorBounds, getHighIndicatorColor(isNoData));
        }
   }

    private void drawBackgroundAndBorder() {
        if (bkgBitmap == null || borderType == null) {
            return; // not ready yet
        }

        Rect bounds = new Rect(0, 0, bkgBitmap.getWidth(), bkgBitmap.getHeight());

        Canvas canvas = new Canvas(bkgBitmap);
        canvas.drawRect(bounds, erasePaint);

        // draw background
        bkgPaint.setColor(backgroundColor);
        bkgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        if (BorderUtils.isBorderRounded(borderType)) {
            canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, bkgPaint);
        } else if (BorderUtils.isBorderRing(borderType)) {
            canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    BORDER_RING_RADIUS, BORDER_RING_RADIUS, bkgPaint);
        } else {
            canvas.drawRect(bounds, bkgPaint);
        }

        // draw border
        if (borderType != BorderType.NONE) {
            bkgPaint.setColor(borderColor);
            bkgPaint.setStyle(Paint.Style.STROKE);
            bkgPaint.setStrokeWidth(BORDER_WIDTH);
            if (BorderUtils.getBorderDrawableStyle(borderType) == ComplicationDrawable.BORDER_STYLE_DASHED) {
                if (BorderUtils.isBorderDotted(borderType)) {
                    bkgPaint.setPathEffect(new DashPathEffect(new float[]{BORDER_DOT_LEN, BORDER_GAP_LEN}, 0f));
                } else {
                    bkgPaint.setPathEffect(new DashPathEffect(new float[]{BORDER_DASH_LEN, BORDER_GAP_LEN}, 0f));
                }
            }
            if (BorderUtils.isBorderRounded(borderType)) {
                canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, bkgPaint);
            } else if (BorderUtils.isBorderRing(borderType)) {
                canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        BORDER_RING_RADIUS, BORDER_RING_RADIUS, bkgPaint);
            } else {
                canvas.drawRect(bounds, bkgPaint);
            }
        }
    }

    private void drawIndicatorBar(Canvas canvas, Paint paint, RectF bounds, int fillColor) {
        paint.setColor(fillColor);
        canvas.drawRoundRect(
                new RectF(
                    bounds.left + 1,
                    bounds.top + 1,
                    bounds.right - 1,
                    bounds.bottom - 1
                ),
                10f,
                10f,
                paint);
    }

    private boolean isNoData() {
        return lastBgData.getValue() == 0
                || (System.currentTimeMillis() - lastBgData.getTimestamp()) > noDataThreshold * CommonConstants.SECOND_IN_MILLIS;
    }

    private int getRangedColor(boolean isNoData) {
        if (lastBgData.getValue() == 0) {
            return Color.LTGRAY;
        } else if (isNoData) {
            return noDataColor;
        }

        if (lastBgData.getValue() <= lowThreshold) {
            return lastBgData.getValue() <= hypoThreshold ? hypoColor : lowColor;
        } else if (lastBgData.getValue() >= highThreshold) {
            return lastBgData.getValue() >= hyperThreshold ? hyperColor : highColor;
        } else {
            return inRangeColor;
        }
    }

    private int getLowIndicatorColor(boolean isNoData) {
        if (isNoData) {
            return lowColor & BG_INDICATOR_INACTIVE_MASK; // inactive
        }
        if (lastBgData.getValue() <= hypoThreshold) {
            return hypoColor;
        } else if (lastBgData.getValue() <= lowThreshold) {
            return lowColor;
        } else {
            return lowColor & BG_INDICATOR_INACTIVE_MASK; // inactive
        }
    }

    private int getInRangeIndicatorColor(boolean isNoData) {
        if (isNoData) {
            return inRangeColor & BG_INDICATOR_INACTIVE_MASK; // inactive
        }
        if (lowThreshold < lastBgData.getValue() && lastBgData.getValue() < highThreshold) {
            return inRangeColor;
        } else {
            return inRangeColor & BG_INDICATOR_INACTIVE_MASK; // inactive
        }
    }

    private int getHighIndicatorColor(boolean isNoData) {
        if (isNoData) {
            return highColor & BG_INDICATOR_INACTIVE_MASK; // inactive
        }
        if (lastBgData.getValue() >= hyperThreshold) {
            return hyperColor;
        } else if (lastBgData.getValue() >= highThreshold) {
            return highColor;
        } else {
            return highColor & BG_INDICATOR_INACTIVE_MASK; // inactive
        }
    }


    private int getAmbientRangedColor(boolean isNoData) {
        return isNoData ? Color.DKGRAY : Color.LTGRAY;
    }

    private void onDataUpdate(BgData bgData) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onDataUpdate: " + bgData.toString());
        }

        if (bgData.getTimestampDiff() < 0) {
            return; // historical data
        }

        long now = System.currentTimeMillis();
        long timeDiff = now - bgData.getTimestamp();
        if (bgData.getValue() <= 0 || timeDiff > CommonConstants.DAY_IN_MILLIS) {
            // too old sample or invalid data
            bgLine1 = "--";
            bgLine2 = "--";
        } else {
            bgLine1 = BgUtils.formatBgValueString(bgData.getValue(), bgData.getTrend(), isUnitConversion);
            bgLine2 = BgUtils.formatBgDeltaString(bgData.getValueDiff(), timeDiff, isUnitConversion);
        }

        Log.d(LOG_TAG, "onDataUpdate: " + bgLine1 + " / " + bgLine2);

        this.lastBgData = bgData;
        this.lastBgUpdate = now;
    }
}
