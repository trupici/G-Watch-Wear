/*
 * Copyright (C) 2022 Juraj Antal
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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.util.Arrays;
import java.util.stream.Collectors;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.util.UiUtils;

import static sk.trupici.gwatch.wear.common.util.CommonConstants.HOUR_IN_MINUTES;
import static sk.trupici.gwatch.wear.common.util.CommonConstants.MINUTE_IN_MILLIS;

public class BgGraph {
    final private static String LOG_TAG = BgGraph.class.getSimpleName();

    private static final String PREF_DATA = "graph_data";
    private static final String PREF_DATA_LAST_UPD_MIN = "graph_last_upd";

    private static final int GRAPH_MIN_VALUE = 40;
    private static final int GRAPH_MAX_VALUE = 400;
    private static final float GRAPH_VALUE_INT = (GRAPH_MAX_VALUE-GRAPH_MIN_VALUE + 1);

    private static final float DOT_RADIUS = 2.75f;
    private static final float DEF_DOT_PADDING = 0f;

    private static final float LINE_WIDTH = 3f;

    private static final int GRAPH_DYN_PADDING = 7;

    public static final int GRAPH_DATA_LEN = 48;

    private int[] graphData = new int[GRAPH_DATA_LEN];
    private long lastGraphUpdateMin = 0;

    private int minValue = 0;
    private int maxValue = 0;

    private Bitmap bitmap;
    private Paint paint;
    private Paint ambientPaint;

    private BgGraphParams params;

    private RectF bounds;

    public void create(SharedPreferences sharedPrefs, BgGraphParams params, RectF bounds) {
        this.params = params;

        this.bounds = bounds;

        paint = UiUtils.createPaint();
        paint.setStyle(Paint.Style.FILL);

        ambientPaint = UiUtils.createAmbientPaint();

        lastGraphUpdateMin = restoreChartData(sharedPrefs, graphData);
        if (lastGraphUpdateMin > 0) {
            recalculateDynamicRange();
        }
        reconfigure(params);
    }

    public void resize(RectF bounds) {
        this.bounds = bounds;

        bitmap = Bitmap.createBitmap((int) bounds.width(), (int) bounds.height(), Bitmap.Config.ARGB_8888);
        drawChart();
    }

    public void reconfigure(BgGraphParams params) {
        this.params = params;

        drawChart();
    }

    public void draw(Canvas canvas, boolean isAmbientMode) {
        if (bitmap == null) {
            Log.e(LOG_TAG, "Failed to draw graph, bitmap is not ready yet");
            return;
        }
        if (isAmbientMode) {
            canvas.drawBitmap(bitmap, bounds.left, bounds.top, ambientPaint);
        } else {
            canvas.drawBitmap(bitmap, bounds.left, bounds.top, paint);
        }
    }

    public void updateGraphData(Double bgValue, long timestamp, SharedPreferences sharedPrefs) {
        // update or reload
        if (bgValue == null) { // graph data has been already updated by BgData Provider, just reload
            lastGraphUpdateMin = restoreChartData(sharedPrefs, graphData);
        } else { // refresh data (scroll left chart)
            lastGraphUpdateMin = updateGraphData(bgValue, timestamp, sharedPrefs, graphData, lastGraphUpdateMin, params.refreshRateMin);
        }

        // redraw chart
        if (lastGraphUpdateMin > 0) {
            recalculateDynamicRange();
            drawChart();
        }
    }

    private void updateDynamicRange(int value) {
        if (value > 0) {
            minValue = minValue == 0 ? value : Math.min(minValue, value);
            maxValue = maxValue == 0 ? value : Math.max(maxValue, value);
            Log.d(LOG_TAG, "graph: updateDynamicRange: min=" + minValue + ", max=" + maxValue);
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
        Log.d(LOG_TAG, "graph: recalculateDynamicRange: min=" + minValue + ", max=" + maxValue);
    }

    private int getVisibleDataLen() {
        return GRAPH_DATA_LEN; // TODO
    }

    private void drawChart() {
        if (bitmap == null) {
            return; // not ready yet
        }

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "graph: Bitmap size: " + bitmap.getWidth() + " x " + bitmap.getHeight());
            Log.d(LOG_TAG, "graph: Padding: " + params.leftPadding + ", " + params.topPadding + ", " + params.rightPadding + ", " + params.bottomPadding);
        }

        bitmap.eraseColor(params.backgroundColor);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        Canvas canvas = new Canvas(bitmap);

        int width = (int)bounds.width() - params.leftPadding - params.rightPadding;
        int height = (int)bounds.height() - params.topPadding - params.bottomPadding;
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "graph: Paint size: " + width + " x " + height);
        }

        float padding = DEF_DOT_PADDING; // FIXME horizontal scale
        int count = (int)(width / (2*DOT_RADIUS + padding));
        if (count > GRAPH_DATA_LEN) {
            count = GRAPH_DATA_LEN;
            padding = (width - count * 2*DOT_RADIUS) / (float)count;
        }
        float graphPaddingX = (width - count * (2*DOT_RADIUS + padding))/2.0f;

        float x, y;
        float xOffset = params.leftPadding + graphPaddingX + padding / 2 + DOT_RADIUS;
        float yOffset = params.topPadding + height;

        GraphRange graphRange = params.enableDynamicRange ? getDynamicRange()
                : new GraphRange(GRAPH_MIN_VALUE, GRAPH_MAX_VALUE, height / GRAPH_VALUE_INT);


        // draw lines
        paint.setStrokeWidth(1f);

        float xMax = bounds.width() - 1 - params.rightPadding;
        float yMax = bounds.height() - 1 - params.bottomPadding;

        // draw hour interval (vertical) lines
        if (params.enableVertLines) {
            paint.setColor(params.vertLineColor);
            for (int mins = HOUR_IN_MINUTES;; mins += HOUR_IN_MINUTES) {
                float lx = xOffset + (2 * DOT_RADIUS + padding) * (count - 1 - mins / (float) params.refreshRateMin);
                if (lx < params.leftPadding) {
                    break;
                }
                canvas.drawLine(lx, params.topPadding, lx, yMax, paint);
            }
        }

        // draw critical boundaries (horizontal) lines
        if (params.enableCriticalLines) {
            paint.setColor(params.criticalLineColor);
            if (graphRange.isInRange(params.hyperThreshold)) {
                y = yOffset - (params.hyperThreshold - graphRange.min) * graphRange.scale;
                canvas.drawLine(params.leftPadding, y, xMax, y, paint);
            }

            if (graphRange.isInRange(params.hypoThreshold)) {
                y = yOffset - (params.hypoThreshold - graphRange.min) * graphRange.scale;
                canvas.drawLine(params.leftPadding, y, xMax, y, paint);
            }
        }

        // draw high boundary line (horizontal)
        if (params.enableHighLine && graphRange.isInRange(params.highThreshold)) {
            paint.setColor(params.highLineColor);
            y = yOffset - (params.highThreshold - graphRange.min) * graphRange.scale;
            canvas.drawLine(params.leftPadding, y, xMax, y, paint);
        }

        // draw low boundary line (horizontal)
        if (params.enableLowLine && graphRange.isInRange(params.lowThreshold)) {
            paint.setColor(params.lowLineColor);
            y = yOffset - (params.lowThreshold - graphRange.min) * graphRange.scale;
            canvas.drawLine(params.leftPadding, y, xMax, y, paint);
        }

        // get offset of the left most value
        int valueOffset = GRAPH_DATA_LEN - count;

        // draw values
        int color;
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

            if (value <= params.hypoThreshold) {
                color = params.hypoColor;
            } else if (value <= params.lowThreshold) {
                color = params.lowColor;
            } else if (value < params.highThreshold) {
                color = params.inRangeColor;
            } else if (value < params.hyperThreshold) {
                color = params.highColor;
            } else {
                color = params.hyperColor;
            }

            paint.setColor(color);

            // draw value with color
            if (params.drawChartLine) { // line graph
                paint.setStrokeWidth(LINE_WIDTH);
                if (prevX == 0 || prevY == 0) {
                    canvas.drawLine(x - LINE_WIDTH / 2f, y, x + LINE_WIDTH / 2f, y, paint);
                } else {
                    canvas.drawLine(prevX, prevY, x, y, paint);
                }
            }

            if (params.drawChartDots) { // dot graph
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
        if (min <= params.lowThreshold){
            min = GRAPH_MIN_VALUE;
        } else {
            min = params.lowThreshold;
        }
        min -= GRAPH_DYN_PADDING;
        if (min < GRAPH_MIN_VALUE) {
            min = GRAPH_MIN_VALUE;
        }

        // get max displayed value
        float height = bounds.height() - params.topPadding - params.bottomPadding;
        int max = maxValue;
        if (max < params.highThreshold) {
            max = params.highThreshold;
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

        return new GraphRange(min, max, scale);
    }


    /**
     * Store graph data to its persistent storage
     */
    public static void storeChartData(SharedPreferences sharedPrefs, int[] graphData, long lastGraphUpdateMin) {
        Log.w(LOG_TAG, "storeChartData: ");
        try {
            String serialized = Arrays.stream(graphData)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining(":"));

            SharedPreferences.Editor edit = sharedPrefs.edit();
            edit.putString(PREF_DATA, serialized);
            edit.putInt(PREF_DATA_LAST_UPD_MIN, (int) lastGraphUpdateMin);
            edit.commit();
        } catch (Exception e) {
            Log.e(LOG_TAG, "graph: storeChartData: failed to store data: " + e.getLocalizedMessage());
        }
    }

    /**
     * Restore graph data from persistent storage and return the time of the last update
     */
    public static long restoreChartData(SharedPreferences sharedPrefs, int[] graphData) {
        Log.w(LOG_TAG, "restoreChartData: ");
        int lastGraphUpdateMin = sharedPrefs.getInt(PREF_DATA_LAST_UPD_MIN, -1);
        if (lastGraphUpdateMin == -1) {
            Log.w(LOG_TAG, "graph: no data timestamp to restore");
            return 0L;
        }

        String serialized = sharedPrefs.getString(PREF_DATA, null);
        if (serialized == null) {
            Log.w(LOG_TAG, "graph: no data to restore");
            return 0L;
        }

        try {
            int[] data = Arrays.stream(serialized.split(":"))
                    .mapToInt(Integer::valueOf)
                    .toArray();
            if (data.length != GRAPH_DATA_LEN) {
                Log.w(LOG_TAG, "graph: failed to deserialize data");
                return 0L;
            }
            System.arraycopy(data, 0, graphData, 0, Math.min(GRAPH_DATA_LEN, data.length));
            return lastGraphUpdateMin;
        } catch (Exception e) {
            Log.e(LOG_TAG, "graph: failed to restore data: " + e.getLocalizedMessage());
            return 0L;
        }
    }

    /**
     *  Update persisted graph data and redraw graph
     */
    public static void updateAndRedraw(Double bgValue, long bgTimestamp, SharedPreferences sharedPrefs, int refreshRateMin) {
        Log.w(LOG_TAG, "updateAndRedraw: ");
        int[] graphData = new int[BgGraph.GRAPH_DATA_LEN];
        long lastGraphUpdateMin = BgGraph.restoreChartData(sharedPrefs, graphData);
        BgGraph.updateGraphData((double)bgValue, bgTimestamp, sharedPrefs, graphData, lastGraphUpdateMin, refreshRateMin);
    }

    /**
     * Update chart data and store data to persistent storage
     *
     * @see #storeChartData(SharedPreferences, int[], long)
     */
    public static long updateGraphData(Double bgValue, long timestamp, SharedPreferences sharedPrefs, int[] graphData, long lastGraphUpdateMin, int refreshRateMin) {
        Log.w(LOG_TAG, "updateGraphData: " + bgValue + ", " + timestamp + ", " + lastGraphUpdateMin);
        if (BuildConfig.DEBUG && bgValue != null) {
            Log.d(LOG_TAG, "graph: updateGraphData: " + bgValue);
        }

        final long now = System.currentTimeMillis() / MINUTE_IN_MILLIS; // minutes
        if (now < 0) {
            Log.e(LOG_TAG, "graph: now is negative: " + now);
            return 0L;
        }

        boolean dataChanged = false;

        if (lastGraphUpdateMin != 0) {
            // shift data left
            int roll = (int) ((now - lastGraphUpdateMin) / refreshRateMin);
            if (roll > 0) {
                lastGraphUpdateMin = now;
                if (BuildConfig.DEBUG) {
                    Log.w(LOG_TAG, "graph: clearing data buffer: " + roll);
                }
                if (roll >= GRAPH_DATA_LEN) {
                    Arrays.fill(graphData, 0);
                } else {
                    graphData = Arrays.copyOfRange(graphData, roll, roll + GRAPH_DATA_LEN);
                }
                dataChanged = true;
            }
        } else {
            lastGraphUpdateMin = now;
        }

        // set new data
        if (bgValue != null) {
            long tsData = timestamp / MINUTE_IN_MILLIS;
            int diff = (int) Math.round((now - tsData)/(double)refreshRateMin);
            if (0 <= diff && diff < GRAPH_DATA_LEN) {
                int newValue = bgValue.intValue();
                int idx = GRAPH_DATA_LEN - 1 - diff;
                int oldValue = graphData[idx];
                int value = oldValue == 0 ? newValue : (oldValue + newValue)/2; // kind of average
                graphData[idx] = value;
                dataChanged = true;
            }
//            lastGraphUpdateMin = now;
        }

        if (dataChanged) {
            storeChartData(sharedPrefs, graphData, (int) lastGraphUpdateMin);
        }

        return lastGraphUpdateMin;
    }

    public long getLastGraphUpdateMin() {
        return lastGraphUpdateMin;
    }

    static class GraphRange {
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

    public static class BgGraphParams {

        private int leftPadding;
        private int rightPadding;
        private int topPadding;
        private int bottomPadding;

        private int backgroundColor;
        private int hypoColor;
        private int lowColor;
        private int inRangeColor;
        private int highColor;
        private int hyperColor;

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

        private int refreshRateMin;

        public int getLeftPadding() {
            return leftPadding;
        }

        public void setLeftPadding(int leftPadding) {
            this.leftPadding = leftPadding;
        }

        public int getRightPadding() {
            return rightPadding;
        }

        public void setRightPadding(int rightPadding) {
            this.rightPadding = rightPadding;
        }

        public int getTopPadding() {
            return topPadding;
        }

        public void setTopPadding(int topPadding) {
            this.topPadding = topPadding;
        }

        public int getBottomPadding() {
            return bottomPadding;
        }

        public void setBottomPadding(int bottomPadding) {
            this.bottomPadding = bottomPadding;
        }

        public int getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public int getHypoColor() {
            return hypoColor;
        }

        public void setHypoColor(int hypoColor) {
            this.hypoColor = hypoColor;
        }

        public int getLowColor() {
            return lowColor;
        }

        public void setLowColor(int lowColor) {
            this.lowColor = lowColor;
        }

        public int getInRangeColor() {
            return inRangeColor;
        }

        public void setInRangeColor(int inRangeColor) {
            this.inRangeColor = inRangeColor;
        }

        public int getHighColor() {
            return highColor;
        }

        public void setHighColor(int highColor) {
            this.highColor = highColor;
        }

        public int getHyperColor() {
            return hyperColor;
        }

        public void setHyperColor(int hyperColor) {
            this.hyperColor = hyperColor;
        }

        public int getVertLineColor() {
            return vertLineColor;
        }

        public void setVertLineColor(int vertLineColor) {
            this.vertLineColor = vertLineColor;
        }

        public int getLowLineColor() {
            return lowLineColor;
        }

        public void setLowLineColor(int lowLineColor) {
            this.lowLineColor = lowLineColor;
        }

        public int getHighLineColor() {
            return highLineColor;
        }

        public void setHighLineColor(int highLineColor) {
            this.highLineColor = highLineColor;
        }

        public int getCriticalLineColor() {
            return criticalLineColor;
        }

        public void setCriticalLineColor(int criticalLineColor) {
            this.criticalLineColor = criticalLineColor;
        }

        public int getHypoThreshold() {
            return hypoThreshold;
        }

        public void setHypoThreshold(int hypoThreshold) {
            this.hypoThreshold = hypoThreshold;
        }

        public int getLowThreshold() {
            return lowThreshold;
        }

        public void setLowThreshold(int lowThreshold) {
            this.lowThreshold = lowThreshold;
        }

        public int getHighThreshold() {
            return highThreshold;
        }

        public void setHighThreshold(int highThreshold) {
            this.highThreshold = highThreshold;
        }

        public int getHyperThreshold() {
            return hyperThreshold;
        }

        public void setHyperThreshold(int hyperThreshold) {
            this.hyperThreshold = hyperThreshold;
        }

        public boolean isEnableDynamicRange() {
            return enableDynamicRange;
        }

        public void setEnableDynamicRange(boolean enableDynamicRange) {
            this.enableDynamicRange = enableDynamicRange;
        }

        public boolean isEnableVertLines() {
            return enableVertLines;
        }

        public void setEnableVertLines(boolean enableVertLines) {
            this.enableVertLines = enableVertLines;
        }

        public boolean isEnableCriticalLines() {
            return enableCriticalLines;
        }

        public void setEnableCriticalLines(boolean enableCriticalLines) {
            this.enableCriticalLines = enableCriticalLines;
        }

        public boolean isEnableHighLine() {
            return enableHighLine;
        }

        public void setEnableHighLine(boolean enableHighLine) {
            this.enableHighLine = enableHighLine;
        }

        public boolean isEnableLowLine() {
            return enableLowLine;
        }

        public void setEnableLowLine(boolean enableLowLine) {
            this.enableLowLine = enableLowLine;
        }

        public boolean isDrawChartLine() {
            return drawChartLine;
        }

        public void setDrawChartLine(boolean drawChartLine) {
            this.drawChartLine = drawChartLine;
        }

        public boolean isDrawChartDots() {
            return drawChartDots;
        }

        public void setDrawChartDots(boolean drawChartDots) {
            this.drawChartDots = drawChartDots;
        }

        public int getRefreshRateMin() {
            return refreshRateMin;
        }

        public void setRefreshRateMin(int refreshRateMin) {
            this.refreshRateMin = refreshRateMin;
        }
    }
}
