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

package sk.trupici.gwatch.wear.providers;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.WearableDialogActivity;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.components.BgGraph;
import sk.trupici.gwatch.wear.components.BgGraphView;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.UiUtils;

public class ComplicationBgGraphActivity extends WearableDialogActivity {

    public static final String LOG_TAG = ComplicationBgGraphActivity.class.getSimpleName();

    private final static int PADDING = 3;

    private final static int TIME_TO_CLOSE_MS = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_complication_graph);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        TextView valueView = findViewById(R.id.value);
        TextView timeView = findViewById(R.id.timestamp);

        String value = sharedPrefs.getString(BgDataProviderService.PREF_TEXT, null);
        String delta = sharedPrefs.getString(BgDataProviderService.PREF_TITLE, null);
        if (value != null) {
            String valueLine = value;
            if (delta != null && delta.length() > 0 ) {
                valueLine += " / " + delta;
            }
            valueView.setText(valueLine);

            long timestamp = sharedPrefs.getLong(BgDataProviderService.PREF_LAST_UPDATE, 0);
            String timeLine = StringUtils.formatTimeOrNoData(timestamp);
            timeView.setText(timeLine);
        }


        BgGraphView graphView = findViewById(R.id.graph);
        Rect graphBounds = UiUtils.getViewBounds(graphView);

        Resources res = getResources();

        BgGraph bgGraph = new BgGraph();

        BgGraph.BgGraphParams graphParams = new BgGraph.BgGraphParams();

        // padding
        graphParams.setLeftPadding(PADDING);
        graphParams.setTopPadding(PADDING);
        graphParams.setRightPadding(PADDING);
        graphParams.setBottomPadding(PADDING);

        // colors
        graphParams.setBackgroundColor(getColor(R.color.def_bg_background_color));
        graphParams.setHypoColor(getColor(R.color.def_bg_hypo_color));
        graphParams.setLowColor(getColor(R.color.def_bg_low_color));
        graphParams.setInRangeColor(getColor(R.color.def_bg_in_range_color));
        graphParams.setHighColor(getColor(R.color.def_bg_high_color));
        graphParams.setHyperColor(getColor(R.color.def_bg_hyper_color));

        // lines
        graphParams.setVertLineColor(getColor(R.color.def_graph_color_vert_line));
        graphParams.setLowLineColor(getColor(R.color.def_graph_color_low_line));
        graphParams.setHighLineColor(getColor(R.color.def_graph_color_high_line));
        graphParams.setCriticalLineColor(getColor(R.color.def_graph_color_critical_line));

        graphParams.setEnableVertLines(res.getBoolean(R.bool.def_graph_enable_vert_lines));
        graphParams.setEnableCriticalLines(res.getBoolean(R.bool.def_graph_enable_critical_lines));
        graphParams.setEnableHighLine(res.getBoolean(R.bool.def_graph_enable_high_line));
        graphParams.setEnableLowLine(res.getBoolean(R.bool.def_graph_enable_low_line));

        graphParams.setEnableDynamicRange(res.getBoolean(R.bool.def_graph_enable_dynamic_range));

        // chart type - DOTS
        graphParams.setDrawChartLine(res.getBoolean(R.bool.def_graph_type_draw_line));
        graphParams.setDrawChartDots(res.getBoolean(R.bool.def_graph_type_draw_dots));

        graphParams.setRefreshRateMin(res.getInteger(R.integer.def_graph_refresh_rate));

        // levels - external BG panel settings dependency !
        graphParams.setHypoThreshold(sharedPrefs.getInt(CommonConstants.PREF_HYPO_THRESHOLD, res.getInteger(R.integer.def_bg_threshold_hypo)));
        graphParams.setLowThreshold(sharedPrefs.getInt(CommonConstants.PREF_LOW_THRESHOLD, res.getInteger(R.integer.def_bg_threshold_low)));
        graphParams.setHighThreshold(sharedPrefs.getInt(CommonConstants.PREF_HIGH_THRESHOLD, res.getInteger(R.integer.def_bg_threshold_high)));
        graphParams.setHyperThreshold(sharedPrefs.getInt(CommonConstants.PREF_HYPER_THRESHOLD, res.getInteger(R.integer.def_bg_threshold_hyper)));

        bgGraph.create(sharedPrefs, graphParams, new RectF(graphBounds));
        bgGraph.updateGraphData(null, System.currentTimeMillis(), sharedPrefs);
        graphView.setBgGraph(bgGraph);

        new Handler(Looper.getMainLooper()).postDelayed(this::finish, TIME_TO_CLOSE_MS);
    }
}
