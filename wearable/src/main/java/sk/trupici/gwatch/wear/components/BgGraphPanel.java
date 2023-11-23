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
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.util.CommonConstants;

import static sk.trupici.gwatch.wear.common.util.CommonConstants.MINUTE_IN_MILLIS;

public class BgGraphPanel extends BroadcastReceiver implements ComponentPanel {
    final private static String LOG_TAG = BgGraphPanel.class.getSimpleName();

    public static final int CONFIG_ID = 12;

    public static final String PREF_BKG_COLOR = "graph_color_background";
    public static final String PREF_HYPO_COLOR = "graph_color_hypo";
    public static final String PREF_LOW_COLOR = "graph_color_low";
    public static final String PREF_IN_RANGE_COLOR = "graph_color_in_range";
    public static final String PREF_HIGH_COLOR = "graph_high_low";
    public static final String PREF_HYPER_COLOR = "graph_hyper_low";

    public static final String PREF_VERT_LINE_COLOR = "graph_color_vert_line";
    public static final String PREF_LOW_LINE_COLOR = "graph_color_low_line";
    public static final String PREF_HIGH_LINE_COLOR = "graph_color_high_line";
    public static final String PREF_CRITICAL_LINE_COLOR = "graph_color_critical_line";

    public static final String PREF_ENABLE_VERT_LINES = "graph_enable_vert_lines";
    public static final String PREF_ENABLE_CRITICAL_LINES = "graph_enable_critical_lines";
    public static final String PREF_ENABLE_HIGH_LINE = "graph_enable_high_line";
    public static final String PREF_ENABLE_LOW_LINE = "graph_enable_low_line";

    public static final String PREF_ENABLE_DYNAMIC_RANGE = "graph_enable_dynamic_range";
    public static final String PREF_SHOW_BG_VALUE = "show_bg_value";

    public static final String PREF_TYPE_LINE = "graph_type_draw_line";
    public static final String PREF_TYPE_DOTS = "graph_type_draw_dots";

    private static final String PREF_REFRESH_RATE = "graph_refresh_rate";


    private final BgGraph bgGraph;
    private final BgGraph.BgGraphParams params;

    final private int refScreenWidth;
    final private int refScreenHeight;

    final private WatchfaceConfig watchfaceConfig;

    private RectF sizeFactors;

    public BgGraphPanel(int screenWidth, int screenHeight, WatchfaceConfig watchfaceConfig) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
        this.watchfaceConfig = watchfaceConfig;

        bgGraph = new BgGraph();
        params = new BgGraph.BgGraphParams();
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {

        RectF bounds = watchfaceConfig.getBgGraphBounds(context);
        sizeFactors = new RectF(
                bounds.left / refScreenWidth,
                bounds.top / refScreenHeight,
                bounds.right / refScreenWidth,
                bounds.bottom / refScreenHeight
        );

        Rect padding = watchfaceConfig.getBgGraphPadding(context);
        params.setLeftPadding(padding.left);
        params.setTopPadding(padding.top);
        params.setRightPadding(padding.right);
        params.setBottomPadding(padding.bottom);

        bgGraph.create(sharedPrefs, params, bounds);
        onConfigChanged(context, sharedPrefs);
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        RectF bounds = new RectF(
                width * sizeFactors.left,
                height * sizeFactors.top,
                width * sizeFactors.right,
                height * sizeFactors.bottom);

        bgGraph.resize(bounds);
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {

        // colors
        params.setBackgroundColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BKG_COLOR, context.getColor(R.color.def_bg_background_color)));
        params.setHypoColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_HYPO_COLOR, context.getColor(R.color.def_bg_hypo_color)));
        params.setLowColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_LOW_COLOR, context.getColor(R.color.def_bg_low_color)));
        params.setInRangeColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_IN_RANGE_COLOR, context.getColor(R.color.def_bg_in_range_color)));
        params.setHighColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_HIGH_COLOR, context.getColor(R.color.def_bg_high_color)));
        params.setHyperColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_HYPER_COLOR, context.getColor(R.color.def_bg_hyper_color)));

        // lines
        params.setVertLineColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_VERT_LINE_COLOR, context.getColor(R.color.def_graph_color_vert_line)));
        params.setLowLineColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_LOW_LINE_COLOR, context.getColor(R.color.def_graph_color_low_line)));
        params.setHighLineColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_HIGH_LINE_COLOR, context.getColor(R.color.def_graph_color_high_line)));
        params.setCriticalLineColor(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_CRITICAL_LINE_COLOR, context.getColor(R.color.def_graph_color_critical_line)));

        params.setEnableVertLines(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_ENABLE_VERT_LINES, context.getResources().getBoolean(R.bool.def_graph_enable_vert_lines)));
        params.setEnableCriticalLines(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_ENABLE_CRITICAL_LINES, context.getResources().getBoolean(R.bool.def_graph_enable_critical_lines)));
        params.setEnableHighLine(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_ENABLE_HIGH_LINE, context.getResources().getBoolean(R.bool.def_graph_enable_high_line)));
        params.setEnableLowLine(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_ENABLE_LOW_LINE, context.getResources().getBoolean(R.bool.def_graph_enable_low_line)));

        params.setEnableDynamicRange(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_ENABLE_DYNAMIC_RANGE, context.getResources().getBoolean(R.bool.def_graph_enable_dynamic_range)));
        params.setShowBgValue(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_SHOW_BG_VALUE, context.getResources().getBoolean(R.bool.def_graph_show_bg_value)));

        params.setDrawChartLine(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_TYPE_LINE, context.getResources().getBoolean(R.bool.def_graph_type_draw_line)));
        params.setDrawChartDots(sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_TYPE_DOTS, context.getResources().getBoolean(R.bool.def_graph_type_draw_dots)));

        params.setRefreshRateMin(sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_REFRESH_RATE, context.getResources().getInteger(R.integer.def_graph_refresh_rate)));

        // levels - external BG panel settings dependency !
        params.setHypoThreshold(sharedPrefs.getInt(CommonConstants.PREF_HYPO_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hypo)));
        params.setLowThreshold(sharedPrefs.getInt(CommonConstants.PREF_LOW_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_low)));
        params.setHighThreshold(sharedPrefs.getInt(CommonConstants.PREF_HIGH_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_high)));
        params.setHyperThreshold(sharedPrefs.getInt(CommonConstants.PREF_HYPER_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hyper)));

        bgGraph.reconfigure(params);
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        bgGraph.draw(canvas, isAmbientMode);
    }

    public void refresh(long timeMs, SharedPreferences sharedPrefs) {
        long currentMinute = timeMs / MINUTE_IN_MILLIS;
        if (bgGraph.getLastGraphUpdateMin() != currentMinute) {
            bgGraph.updateGraphData(null, timeMs, null, sharedPrefs);
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (CommonConstants.BG_RECEIVER_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            BgData bgData = BgData.fromBundle(extras);
            if (bgData.getValue() == 0 || bgData.getTimestamp() == 0) {
                return;
            }
            bgGraph.updateGraphData((double)bgData.getValue(), bgData.getTimestamp(), bgData.getTrend(), sharedPrefs);
        } else if (CommonConstants.REMOTE_CONFIG_ACTION.equals(intent.getAction())) {
            onConfigChanged(context, sharedPrefs);
        } else {
            Log.e(LOG_TAG, "onReceive: unsupported intent: " + intent.getAction());
        }
    }

}
