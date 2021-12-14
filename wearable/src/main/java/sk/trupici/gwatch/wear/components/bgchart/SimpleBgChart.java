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

package sk.trupici.gwatch.wear.components.bgchart;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.stream.Collectors;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.BgPanel;
import sk.trupici.gwatch.wear.components.ComponentPanel;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.util.DumpUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

public class SimpleBgChart implements ComponentPanel {
    final private static String LOG_TAG = DumpUtils.class.getSimpleName();

    public static final String PREF_BKG_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_background";
    public static final String PREF_CRITICAL_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_critical";
    public static final String PREF_WARN_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_warn";
    public static final String PREF_IN_RANGE_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_in_range";

    public static final String PREF_VERT_LINE_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_vert_line";
    public static final String PREF_LOW_LINE_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_low_line";
    public static final String PREF_HIGH_LINE_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_high_line";
    public static final String PREF_CRITICAL_LINE_COLOR = AnalogWatchfaceConfig.PREF_PREFIX + "graph_color_critical_line";

    public static final String PREF_ENABLE_VERT_LINES = AnalogWatchfaceConfig.PREF_PREFIX + "graph_enable_vert_lines";
    public static final String PREF_ENABLE_CRITICAL_LINES = AnalogWatchfaceConfig.PREF_PREFIX + "graph_enable_critical_lines";
    public static final String PREF_ENABLE_HIGH_LINE = AnalogWatchfaceConfig.PREF_PREFIX + "graph_enable_high_line";
    public static final String PREF_ENABLE_LOW_LINE = AnalogWatchfaceConfig.PREF_PREFIX + "graph_enable_low_line";

    public static final String PREF_ENABLE_DYNAMIC_RANGE = AnalogWatchfaceConfig.PREF_PREFIX + "graph_enable_dynamic_range";

    public static final String PREF_TYPE_LINE = AnalogWatchfaceConfig.PREF_PREFIX + "graph_type_draw_line";
    public static final String PREF_TYPE_DOTS = AnalogWatchfaceConfig.PREF_PREFIX + "graph_type_draw_dots";

    private static final String PREF_DATA = AnalogWatchfaceConfig.PREF_PREFIX + "graph_data";
    private static final String PREF_DATA_LAST_UPD_MIN = AnalogWatchfaceConfig.PREF_PREFIX + "graph_last_upd";

    private static final String PREF_REFRESH_RATE = AnalogWatchfaceConfig.PREF_PREFIX + "graph_refresh_rate";


    private static final int GRAPH_MIN_VALUE = 40;
    private static final int GRAPH_MAX_VALUE = 400;
    private static final float GRAPH_VALUE_INT = (GRAPH_MAX_VALUE-GRAPH_MIN_VALUE + 1);

    private static final int MINUTE_IN_MS = 60000; // 60 * 1000
    private static final int HOUR_IN_MINUTES = 60;

    private static final float DOT_RADIUS = 2f;
    private static final float DEF_DOT_PADDING = 1.5f;

    private static final float LINE_WIDTH = 3f;

    private static final int GRAPH_DYN_PADDING = 7;

    private static final int GRAPH_DATA_LEN = 48;


    private int[] graphData = new int[GRAPH_DATA_LEN];
    private long lastGraphUpdateMin = 0;
    private int refreshRateMin;

    private int minValue = 0;
    private int maxValue = 0;

    private int leftPadding;
    private int rightPadding;
    private int topPadding;
    private int bottomPadding;

    private Bitmap bitmap;
    private Paint paint;
    private Paint ambientPaint;

    private int backgroundColor;
    private int criticalColor;
    private int warnColor;
    private int inRangeColor;

    private int vertLineColor;
    private int lowLineColor;
    private int highLineColor;
    private int criticalLineColor;

    private int hypoThreshold;
    private int lowThreshold;
    private int highThreshold;
    private int hyperThreshold;

    private boolean enableDynamicRange;
    private boolean enableVertLines;
    private boolean enableCriticalLines;
    private boolean enableHighLine;
    private boolean enableLowLine;

    private boolean drawChartLine;
    private boolean drawChartDots;

    private RectF sizeFactors;
    private RectF bounds;

    final private int refScreenWidth;
    final private int refScreenHeight;


    public SimpleBgChart(int screenWidth, int screenHeight) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {

        float left = context.getResources().getDimension(R.dimen.layout_graph_panel_left);
        float top = context.getResources().getDimension(R.dimen.layout_graph_panel_top);
        float right = context.getResources().getDimension(R.dimen.layout_graph_panel_right);
        float bottom = context.getResources().getDimension(R.dimen.layout_graph_panel_bottom);

        sizeFactors = new RectF(
                left / refScreenWidth,
                top / refScreenHeight,
                right / refScreenWidth,
                bottom / refScreenHeight
        );

        leftPadding = context.getResources().getDimensionPixelOffset(R.dimen.graph_left_padding);
        topPadding = context.getResources().getDimensionPixelOffset(R.dimen.graph_top_padding);
        rightPadding = context.getResources().getDimensionPixelOffset(R.dimen.graph_right_padding);
        bottomPadding = context.getResources().getDimensionPixelOffset(R.dimen.graph_bottom_padding);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        ambientPaint = new Paint();
        ambientPaint.setAntiAlias(false);
        ambientPaint.setColorFilter(filter);

        onConfigChanged(context, sharedPrefs);

        restoreChartData(sharedPrefs);
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        bounds = new RectF(
                width * sizeFactors.left,
                height * sizeFactors.top,
                width * sizeFactors.right,
                height * sizeFactors.bottom);

        bitmap = Bitmap.createBitmap((int) bounds.width(), (int) bounds.height(), Bitmap.Config.ARGB_8888);
        drawChart();
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {

        // colors
        backgroundColor = sharedPrefs.getInt(PREF_BKG_COLOR, context.getColor(R.color.def_graph_color_background));
        criticalColor = sharedPrefs.getInt(PREF_CRITICAL_COLOR, context.getColor(R.color.def_graph_color_critical));
        warnColor = sharedPrefs.getInt(PREF_WARN_COLOR, context.getColor(R.color.def_graph_color_warn));
        inRangeColor = sharedPrefs.getInt(PREF_IN_RANGE_COLOR, context.getColor(R.color.def_graph_color_in_range));

        // lines
        vertLineColor = sharedPrefs.getInt(PREF_VERT_LINE_COLOR, context.getColor(R.color.def_graph_color_vert_line));
        lowLineColor = sharedPrefs.getInt(PREF_LOW_LINE_COLOR, context.getColor(R.color.def_graph_color_low_line));
        highLineColor = sharedPrefs.getInt(PREF_HIGH_LINE_COLOR, context.getColor(R.color.def_graph_color_high_line));
        criticalLineColor = sharedPrefs.getInt(PREF_CRITICAL_LINE_COLOR, context.getColor(R.color.def_graph_color_critical_line));

        enableVertLines = sharedPrefs.getBoolean(PREF_ENABLE_VERT_LINES, context.getResources().getBoolean(R.bool.def_graph_enable_vert_lines));
        enableCriticalLines = sharedPrefs.getBoolean(PREF_ENABLE_CRITICAL_LINES, context.getResources().getBoolean(R.bool.def_graph_enable_critical_lines));
        enableHighLine = sharedPrefs.getBoolean(PREF_ENABLE_HIGH_LINE, context.getResources().getBoolean(R.bool.def_graph_enable_high_line));
        enableLowLine = sharedPrefs.getBoolean(PREF_ENABLE_LOW_LINE, context.getResources().getBoolean(R.bool.def_graph_enable_low_line));

        enableDynamicRange = sharedPrefs.getBoolean(PREF_ENABLE_DYNAMIC_RANGE, context.getResources().getBoolean(R.bool.def_graph_enable_dynamic_range));

        drawChartLine = sharedPrefs.getBoolean(PREF_TYPE_LINE, context.getResources().getBoolean(R.bool.def_graph_type_draw_line));
        drawChartDots = sharedPrefs.getBoolean(PREF_TYPE_DOTS, context.getResources().getBoolean(R.bool.def_graph_type_draw_dots));

        refreshRateMin = sharedPrefs.getInt(PREF_REFRESH_RATE, context.getResources().getInteger(R.integer.def_graph_refresh_rate));

        // levels - external BG panel settings dependency !
        hypoThreshold = PreferenceUtils.getStringValueAsInt(sharedPrefs, BgPanel.PREF_HYPO_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hypo));
        lowThreshold = PreferenceUtils.getStringValueAsInt(sharedPrefs, BgPanel.PREF_LOW_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_low));
        highThreshold = PreferenceUtils.getStringValueAsInt(sharedPrefs, BgPanel.PREF_HIGH_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_high));
        hyperThreshold = PreferenceUtils.getStringValueAsInt(sharedPrefs, BgPanel.PREF_HYPER_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hyper));
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        if (isAmbientMode) {
            canvas.drawBitmap(bitmap, bounds.left, bounds.top, ambientPaint);
        } else {
            canvas.drawBitmap(bitmap, bounds.left, bounds.top, paint);
        }
    }

    public void refresh(long timeMs, SharedPreferences sharedPrefs) {
        long currentMinute = timeMs / MINUTE_IN_MS;
        if (lastGraphUpdateMin != currentMinute) {
            lastGraphUpdateMin = currentMinute;
            updateGraphData(null, timeMs, sharedPrefs);
        }
    }

    public void updateGraphData(Double bgValue, long timestamp, SharedPreferences sharedPrefs) {
        if (BuildConfig.DEBUG && bgValue != null) {
            Log.d(LOG_TAG, "updateGraphData: " + bgValue);
        }

        final long now = System.currentTimeMillis() / (long)MINUTE_IN_MS; // minutes
        if (now < 0) {
            Log.e(LOG_TAG, "now is negative: " + now);
            return;
        }

        boolean dataChanged = false;

        if (lastGraphUpdateMin != 0) {
            // shift data left
            int roll = (int) ((now - lastGraphUpdateMin) / refreshRateMin);
            if (roll > 0) {
                lastGraphUpdateMin = now;
                if (roll >= GRAPH_DATA_LEN) {
                    Arrays.fill(graphData, 0);
                } else {
                    graphData = Arrays.copyOfRange(graphData, roll, roll + GRAPH_DATA_LEN);
                    recalculateDynamicRange();
                }
                dataChanged = true;
            }
        } else {
            lastGraphUpdateMin = now;
        }

        // set new data
        if (bgValue != null) {
            long tsData = timestamp / MINUTE_IN_MS;
            int diff = (int) Math.round((now - tsData)/(double)refreshRateMin);
            if (0 <= diff && diff < GRAPH_DATA_LEN) {
                int newValue = bgValue.intValue();
                int idx = GRAPH_DATA_LEN - 1 - diff;
                int oldValue = graphData[idx];
                int value = oldValue == 0 ? newValue : (oldValue + newValue)/2; // kind of average
                graphData[idx] = value;
                updateDynamicRange(value);
                dataChanged = true;
            }
//            lastGraphUpdateMin = now;
        }

        drawChart();

        if (dataChanged) {
            storeChartData(sharedPrefs);
        }
    }

    private void updateDynamicRange(int value) {
        if (value > 0) {
            minValue = minValue == 0 ? value : Math.min(minValue, value);
            maxValue = maxValue == 0 ? value : Math.max(maxValue, value);
            Log.d(LOG_TAG, "updateDynamicRange: min=" + minValue + ", max=" + maxValue);
        }
    }

    private void recalculateDynamicRange() {
        // calculate scale from visible values only
        minValue = maxValue = 0;
        int visibleDataLen = getVisibleDataLen();
        int offset = GRAPH_DATA_LEN - visibleDataLen;
        for (int i = offset; i < GRAPH_DATA_LEN; i++) {
            updateDynamicRange(graphData[i]);
        }
        Log.d(LOG_TAG, "recalculateDynamicRange: min=" + minValue + ", max=" + maxValue);
    }

    private int getVisibleDataLen() {
        return GRAPH_DATA_LEN; // TODO
    }

    private void drawChart() {
        if (bitmap == null) {
            return; // not ready yet
        }

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Bitmap size: " + bitmap.getWidth() + " x " + bitmap.getHeight());
            Log.d(LOG_TAG, "Padding: " + leftPadding + ", " + topPadding + ", " + rightPadding + ", " + bottomPadding);
        }

        bitmap.eraseColor(backgroundColor);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        Canvas canvas = new Canvas(bitmap);

        int width = (int)bounds.width() - leftPadding - rightPadding;
        int height = (int)bounds.height() - topPadding - bottomPadding;
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Paint size: " + width + " x " + height);
        }

        float padding = DEF_DOT_PADDING; // FIXME horizontal scale
        int count = (int)(width / (2*DOT_RADIUS + padding));
        if (count > GRAPH_DATA_LEN) {
            count = GRAPH_DATA_LEN;
            padding = (width - count * 2*DOT_RADIUS) / (float)count;
        }
        float graphPaddingX = (width - count * (2*DOT_RADIUS + padding))/2.0f;

        float x, y;
        float xOffset = leftPadding + graphPaddingX + padding / 2 + DOT_RADIUS;
        float yOffset = topPadding + height;

        GraphRange graphRange = enableDynamicRange ? getDynamicRange()
                : new GraphRange(GRAPH_MIN_VALUE, GRAPH_MAX_VALUE, height / GRAPH_VALUE_INT);


        // draw lines
        paint.setStrokeWidth(1f);

        // draw hour interval (vertical) lines
        if (enableVertLines) {
            paint.setColor(vertLineColor);
            for (int mins = HOUR_IN_MINUTES;; mins += HOUR_IN_MINUTES) {
                float lx = xOffset + (2 * DOT_RADIUS + padding) * (count - 1 - mins / (float) refreshRateMin);
                if (lx < leftPadding) {
                    break;
                }
                canvas.drawLine(lx, topPadding, lx, height - bottomPadding, paint);
            }
        }

        // draw critical boundaries (horizontal) lines
        if (enableCriticalLines) {
            paint.setColor(criticalLineColor);
            if (graphRange.isInRange(hyperThreshold)) {
                y = yOffset - (hyperThreshold - graphRange.min) * graphRange.scale;
                canvas.drawLine(leftPadding, y, width - rightPadding, y, paint);
            }

            if (graphRange.isInRange(hypoThreshold)) {
                y = yOffset - (hypoThreshold - graphRange.min) * graphRange.scale;
                canvas.drawLine(leftPadding, y, width - rightPadding, y, paint);
            }
        }

        // draw high boundary line (horizontal)
        if (enableHighLine && graphRange.isInRange(highThreshold)) {
            paint.setColor(highLineColor);
            y = yOffset - (highThreshold - graphRange.min) * graphRange.scale;
            canvas.drawLine(leftPadding, y, width - rightPadding, y, paint);
        }

        // draw low boundary line (horizontal)
        if (enableLowLine && graphRange.isInRange(lowThreshold)) {
            paint.setColor(lowLineColor);
            y = yOffset - (lowThreshold - graphRange.min) * graphRange.scale;
            canvas.drawLine(leftPadding, y, width - rightPadding, y, paint);
        }

        // get offset of the left most value
        int valueOffset = GRAPH_DATA_LEN - count;

        // draw values
        int color = 0, prevColor = 0;
        float prevX = 0, prevY = 0;
        for (int i = valueOffset; i < GRAPH_DATA_LEN; i++) {
            int value = graphData[i];
            if (value == 0) {
                prevX = prevY = 0;
                continue;
            } else if (value < GRAPH_MIN_VALUE) {
                value = GRAPH_MIN_VALUE;
            }
            if (value > GRAPH_MAX_VALUE) {
                value = GRAPH_MAX_VALUE;
            }

            x = xOffset + (2 * DOT_RADIUS + padding) * (i - valueOffset);
            y = yOffset - ((value - graphRange.min) * graphRange.scale);

            prevColor = color;

            if (value <= hypoThreshold) {
                color = criticalColor;
            } else if (value <= lowThreshold) {
                color = warnColor;
            } else if (value < highThreshold) {
                color = inRangeColor;
            } else if (value < hyperThreshold) {
                color = warnColor;
            } else {
                color = criticalColor;
            }

            paint.setColor(color);

            // draw value with color
            if (drawChartLine) { // line graph
                paint.setStrokeWidth(LINE_WIDTH);
                if (prevX == 0 || prevY == 0) {
                    canvas.drawLine(x - LINE_WIDTH / 2f, y, x + LINE_WIDTH / 2f, y, paint);
                } else {
                    canvas.drawLine(prevX, prevY, x, y, paint);
                }
            }

            if (drawChartDots) { // dot graph
                canvas.drawCircle(x, y, DOT_RADIUS, paint);
            }

            prevX = x;
            prevY = y;
        }
    }

    private GraphRange getDynamicRange() {
        float scale;

        // get min displayed value
        int min = minValue;
        if (min <= lowThreshold){
            min = GRAPH_MIN_VALUE;
        } else {
            min = lowThreshold;
        }
        min -= GRAPH_DYN_PADDING;
        if (min < GRAPH_MIN_VALUE) {
            min = GRAPH_MIN_VALUE;
        }

        // get max displayed value
        int max = maxValue;
        if (max < highThreshold) {
            max = highThreshold;
        } else {
            scale = bounds.width() / ((float) (max - min + 1));
            max += GRAPH_DYN_PADDING / scale; // increase upper boundary if scaled
        }
        max += GRAPH_DYN_PADDING;
        if (GRAPH_MAX_VALUE < max) {
            max = GRAPH_MAX_VALUE;
        }

        // get dynamic horizontal scale
        scale = bounds.height() / ((float) (max - min + 1));

        return new GraphRange(min, max, scale);
    }


    private void storeChartData(SharedPreferences sharedPrefs) {
        try {
            String serialized = Arrays.stream(graphData)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining(":"));

            SharedPreferences.Editor edit = sharedPrefs.edit();
            edit.putString(PREF_DATA, serialized);
            edit.putInt(PREF_DATA_LAST_UPD_MIN, (int) lastGraphUpdateMin);
            edit.apply();
        } catch (Exception e) {
            Log.e(LOG_TAG, "storeChartData: failed to store data: " + e.getLocalizedMessage());
        }
    }

    private void restoreChartData(SharedPreferences sharedPrefs) {
        lastGraphUpdateMin = sharedPrefs.getInt(PREF_DATA_LAST_UPD_MIN, 0);
        if (lastGraphUpdateMin == 0) {
            return;
        }

        String serialized = sharedPrefs.getString(PREF_DATA, null);
        if (serialized == null) {
            lastGraphUpdateMin = 0;
            return;
        }

        try {
            int data[] = Arrays.stream(serialized.split(":"))
                    .mapToInt(Integer::valueOf)
                    .toArray();
            if (data.length != GRAPH_DATA_LEN) {
                lastGraphUpdateMin = 0;
                return;
            }
            System.arraycopy(data, 0, graphData, data.length, Math.min(GRAPH_DATA_LEN, data.length));
        } catch (Exception e) {
            Log.e(LOG_TAG, "restoreChartData: failed to restore data: " + e.getLocalizedMessage());
        }
    }


    class GraphRange {
        final int min;
        final int max;
        final float scale;

        GraphRange(int min, int max, float scale) {
            this.min = min;
            this.max = max;
            this.scale = scale;
        }

        boolean isInRange(int value) {
            return min <= value && value <= max;
        }
    }

}
