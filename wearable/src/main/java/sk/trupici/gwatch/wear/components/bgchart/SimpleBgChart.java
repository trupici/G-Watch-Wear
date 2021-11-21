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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import java.util.Arrays;
import java.util.stream.Collectors;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.util.DumpUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

public class SimpleBgChart {
    final private static String LOG_TAG = DumpUtils.class.getSimpleName();

    private static final int GRAPH_MIN_VALUE = 40;
    private static final int GRAPH_MAX_VALUE = 400;
    private static final float GRAPH_VALUE_INT = (GRAPH_MAX_VALUE-GRAPH_MIN_VALUE + 1);

    private static final int MIN_GRAPH_WIDTH_DP = 110;
    private static final int MIN_GRAPH_HEIGHT_DP = 40;

    private static final int GRAPH_LEFT_PADDING = 1;
    private static final int GRAPH_RIGHT_PADDING = 1;
    private static final int GRAPH_TOP_PADDING = 1;
    private static final int GRAPH_BOTTOM_PADDING = 1;

    private static final int MINUTE_IN_MS = 60000; // 60 * 1000
    private static final int DAY_IN_MINUTES = 1440; // 24 * 60
    private static final int HOUR_IN_MINUTES = 60;


    private static final float DOT_RADIUS = 2f;
    private static final float DEF_DOT_PADDING = 1.5f;

    private static final float LINE_WIDTH = 3f;

    private static final int GRAPH_DYN_PADDING = 7;


    private static final int GRAPH_DATA_LEN = 48;

    private static final int DEF_REFRESH_RATE_MIN = 5;
    private static final int HIGH_REFRESH_RATE_MIN = 1;

    private static final String GRAPH_PREF_DATA_NAME = "pref_graph_data";
    private static final String GRAPH_PREF_DATA_LAST_UPD_MIN = "pref_graph_last_upd";


    private int[] graphData = new int[GRAPH_DATA_LEN];
    private long lastGraphUpdateMin = 0;
    private int refreshRateMin = DEF_REFRESH_RATE_MIN;

    private int width;
    private int height;

    private Point position;

    private int leftPadding = GRAPH_LEFT_PADDING;
    private int rightPadding = GRAPH_RIGHT_PADDING;
    private int topPadding = GRAPH_TOP_PADDING;
    private int bottomPadding = GRAPH_BOTTOM_PADDING;

    private Bitmap chartBitmap;
    private Paint chartPaint;
    private Paint ambientPaint;

    private int backgroundColor;
    private int criticalColor;
    private int warColor;
    private int inRangeColor;

    private int vertLineColor;
    private int lowLineColor;
    private int highLineColor;
    private int criticalLineColor;

    private int hypoThreshold;
    private int lowThreshold;
    private int highThreshold;
    private int hyperThreshold;

    private int minValue = 0;
    private int maxValue = 0;

    private boolean enableDynamicRange;
    private boolean enableVertLines;
    private boolean enableCriticalLines;
    private boolean enableHighLine;
    private boolean enableLowLine;

    private boolean drawChartLine;
    private boolean drawChartDots;

    private SharedPreferences prefs;

    public SimpleBgChart(int left, int top, int width, int height, SharedPreferences prefs) {
        this.prefs = prefs;

        layout(left, top, width, height);

        chartPaint = new Paint();
        chartPaint.setStyle(Paint.Style.FILL);
        chartPaint.setAntiAlias(true);

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        ambientPaint = new Paint();
        chartPaint.setAntiAlias(false);
        ambientPaint.setColorFilter(filter);

        updateConfig();

        restoreChartData();
    }

    public void layout(int left, int top, int width, int height) {
        setPosition(left, top);
        setSize(width, height);

        chartBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void scale(float scaleX, float scaleY) {
//        chartBitmap = Bitmap.createScaledBitmap(
//                chartBitmap,
//                (int) (chartBitmap.getWidth() * scaleX),
//                (int) (chartBitmap.getHeight() * scaleY),
//                true);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setPosition(int left, int top) {
        this.position = new Point(left, top);
    }

    public void setPadding(int left, int top, int right, int bottom) {
        this.leftPadding = left;
        this.topPadding = top;
        this.rightPadding = right;
        this.bottomPadding = bottom;
    }

    public void updateConfig() {

        // colors
        backgroundColor = prefs.getInt("graph_color_warn", Color.TRANSPARENT);
        criticalColor = prefs.getInt("graph_color_critical", Color.RED/*ContextCompat.getColor(context, R.color.def_red)*/);
        warColor = prefs.getInt("graph_color_warn", Color.YELLOW/*ContextCompat.getColor(context, R.color.def_orange)*/);
        inRangeColor = prefs.getInt("graph_color_in_range", Color.WHITE/*ContextCompat.getColor(context, R.color.def_green)*/);

        // lines
        vertLineColor = prefs.getInt("graph_color_vert_line", Color.TRANSPARENT);
        lowLineColor = prefs.getInt("graph_color_low_line", Color.RED);
        highLineColor = prefs.getInt("graph_color_high_line", Color.YELLOW);
        criticalLineColor = prefs.getInt("graph_color_critical_line", Color.RED);

        enableVertLines = prefs.getBoolean("graph_enable_vert_lines", true);
        enableCriticalLines = prefs.getBoolean("graph_enable_vert_lines", true);
        enableHighLine = prefs.getBoolean("graph_enable_high_line", true);
        enableLowLine = prefs.getBoolean("graph_enable_low_line", true);

        // levels
        hypoThreshold = PreferenceUtils.getStringValueAsInt(prefs, "cfg_glucose_level_hypo", 70);
        lowThreshold = PreferenceUtils.getStringValueAsInt(prefs, "cfg_glucose_level_low", 80);
        highThreshold = PreferenceUtils.getStringValueAsInt(prefs, "cfg_glucose_level_high", 170);
        hyperThreshold = PreferenceUtils.getStringValueAsInt(prefs, "cfg_glucose_level_hyper", 270);

        enableDynamicRange = prefs.getBoolean("graph_enable_dynamic_range", true);

        drawChartLine = prefs.getBoolean("graph_type_draw_line", false);
        drawChartDots = prefs.getBoolean("graph_type_draw_dots", true);
    }

    public void draw(Canvas canvas, boolean ambientMode) {
        if (ambientMode) {
            canvas.drawBitmap(chartBitmap, position.x, position.y, ambientPaint);
        } else {
            long currentMinute = System.currentTimeMillis() / MINUTE_IN_MS;
            if (lastGraphUpdateMin != currentMinute) {
                refresh();
            }
            canvas.drawBitmap(chartBitmap, position.x, position.y, chartPaint);
        }
    }

    public void refresh() {
        updateGraphData(null, System.currentTimeMillis());
    }

    public void updateGraphData(Double bgValue, long timestamp) {
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
            storeChartData();
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

        chartBitmap.eraseColor(backgroundColor);
        chartPaint.reset();
        chartPaint.setStyle(Paint.Style.FILL);
        chartPaint.setAntiAlias(true);

        Canvas canvas = new Canvas(chartBitmap);

//        int width = this.width - leftPadding - rightPadding;
//        int height = this.height - topPadding - bottomPadding;
        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "Paint size: " + width + " x " + height);
        }

        float padding = DEF_DOT_PADDING; // FIXME horizontal scale
        int count = (int)(width / (2*DOT_RADIUS + padding));
        if (count > GRAPH_DATA_LEN) {
            count = GRAPH_DATA_LEN;
            padding = (width - count * 2*DOT_RADIUS) / (float)count;
        }
        float graphPaddingX = (width - count * (2*DOT_RADIUS + padding))/2.0f;

        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "count: " + count + ", r: " + DOT_RADIUS + ", pad: " + padding);
        }

        float x, y;
        float xOffset = GRAPH_LEFT_PADDING + graphPaddingX + padding / 2 + DOT_RADIUS;
        float yOffset = GRAPH_TOP_PADDING + height;

        GraphRange graphRange = enableDynamicRange ? getDynamicRange()
                : new GraphRange(GRAPH_MIN_VALUE, GRAPH_MAX_VALUE, height / GRAPH_VALUE_INT);


        // draw lines
        chartPaint.setStrokeWidth(1f);

        // draw hour interval (vertical) lines
        if (enableVertLines) {
            chartPaint.setColor(vertLineColor);
            for (int mins = HOUR_IN_MINUTES;; mins += HOUR_IN_MINUTES) {
                float lx = xOffset + (2 * DOT_RADIUS + padding) * (count - 1 - mins / (float) refreshRateMin);
                if (lx < GRAPH_LEFT_PADDING) {
                    break;
                }
                canvas.drawLine(lx, GRAPH_TOP_PADDING, lx, height - GRAPH_BOTTOM_PADDING, chartPaint);
            }
        }

        // draw critical boundaries (horizontal) lines
        if (enableCriticalLines) {
            chartPaint.setColor(criticalLineColor);
            if (graphRange.isInRange(hyperThreshold)) {
                y = yOffset - (hyperThreshold - graphRange.min) * graphRange.scale;
                canvas.drawLine(GRAPH_LEFT_PADDING, y, width - GRAPH_RIGHT_PADDING, y, chartPaint);
            }

            if (graphRange.isInRange(hypoThreshold)) {
                y = yOffset - (hypoThreshold - graphRange.min) * graphRange.scale;
                canvas.drawLine(GRAPH_LEFT_PADDING, y, width - GRAPH_RIGHT_PADDING, y, chartPaint);
            }
        }

        // draw high boundary line (horizontal)
        if (enableHighLine && graphRange.isInRange(highThreshold)) {
            chartPaint.setColor(highLineColor);
            y = yOffset - (highThreshold - graphRange.min) * graphRange.scale;
            canvas.drawLine(GRAPH_LEFT_PADDING, y, width - GRAPH_RIGHT_PADDING, y, chartPaint);
        }

        // draw low boundary line (horizontal)
        if (enableLowLine && graphRange.isInRange(lowThreshold)) {
            chartPaint.setColor(lowLineColor);
            y = yOffset - (lowThreshold - graphRange.min) * graphRange.scale;
            canvas.drawLine(GRAPH_LEFT_PADDING, y, width - GRAPH_RIGHT_PADDING, y, chartPaint);
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
                color = warColor;
            } else if (value < highThreshold) {
                color = inRangeColor;
            } else if (value < hyperThreshold) {
                color = warColor;
            } else {
                color = criticalColor;
            }

            chartPaint.setColor(color);

            // draw value with color
            if (drawChartLine) { // line graph
                chartPaint.setStrokeWidth(LINE_WIDTH);
                if (prevX == 0 || prevY == 0) {
                    canvas.drawLine(x - LINE_WIDTH / 2f, y, x + LINE_WIDTH / 2f, y, chartPaint);
                } else {
                    canvas.drawLine(prevX, prevY, x, y, chartPaint);
                }
            }

            if (drawChartDots) { // dot graph
                canvas.drawCircle(x, y, DOT_RADIUS, chartPaint);
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
            scale = height / ((float) (max - min + 1));
            max += GRAPH_DYN_PADDING / scale; // increase upper boundary if scaled
        }
        max += GRAPH_DYN_PADDING;
        if (GRAPH_MAX_VALUE < max) {
            max = GRAPH_MAX_VALUE;
        }

        // get dynamic horizontal scale
        scale = height / ((float) (max - min + 1));
        if (BuildConfig.DEBUG) {
//          Log.d(LOG_TAG, "getDynamicRange: min=" + min + ", max=" + max + ", scale=" + scale);
        }
        return new GraphRange(min, max, scale);
    }


    private void storeChartData() {
        try {
            String serialized = Arrays.stream(graphData)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining(":"));

            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(GRAPH_PREF_DATA_NAME, serialized);
            edit.putInt(GRAPH_PREF_DATA_LAST_UPD_MIN, (int) lastGraphUpdateMin);
            edit.apply();
        } catch (Exception e) {
            Log.e(LOG_TAG, "storeChartData: failed to store data: " + e.getLocalizedMessage());
        }
    }

    private void restoreChartData() {
        lastGraphUpdateMin = prefs.getInt(GRAPH_PREF_DATA_LAST_UPD_MIN, 0);
        if (lastGraphUpdateMin == 0) {
            return;
        }

        String serialized = prefs.getString(GRAPH_PREF_DATA_NAME, null);
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
