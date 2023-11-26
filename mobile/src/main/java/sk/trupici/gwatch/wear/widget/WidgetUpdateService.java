/*
 * Copyright (C) 2019 Juraj Antal
 *
 * Originally created in G-Watch App
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

package sk.trupici.gwatch.wear.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.PersistableBundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import java.util.Arrays;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.data.AAPSPacket;
import sk.trupici.gwatch.wear.common.data.ConfigPacket;
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.data.GlucosePacketBase;
import sk.trupici.gwatch.wear.common.data.Packet;
import sk.trupici.gwatch.wear.common.data.Trend;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.util.AndroidUtils;
import sk.trupici.gwatch.wear.util.DelayedWorker;
import sk.trupici.gwatch.wear.util.DexcomUtils;
import sk.trupici.gwatch.wear.view.MainActivity;

@SuppressLint("SpecifyJobSchedulerIdRange")
public class WidgetUpdateService extends JobService {

    private static final String LOG_TAG = WidgetUpdateService.class.getSimpleName();

    private static final String KEY_GRAPH_DATA = sk.trupici.gwatch.wear.util.PreferenceUtils.SESSION_DATA_PREFIX + "widget_graph_data";
    private static final String KEY_GRAPH_LAST_UPDATE = sk.trupici.gwatch.wear.util.PreferenceUtils.SESSION_DATA_PREFIX + "widget_graph_last_update";
    private static final String KEY_GRAPH_REFRESH_RATE = sk.trupici.gwatch.wear.util.PreferenceUtils.SESSION_DATA_PREFIX + "widget_graph_refresh_date";
    private static final String KEY_LAST_WIDGET_DATA = sk.trupici.gwatch.wear.util.PreferenceUtils.SESSION_DATA_PREFIX + "widget_last_data";

    public static final int WIDGET_JOB_ID = 8182;

    private static final String NO_DATA_TEXT = "--";

    private static final int GRAPH_MIN_VALUE = 40;
    private static final int GRAPH_MAX_VALUE = 400;
    private static final float GRAPH_VALUE_INT = (GRAPH_MAX_VALUE-GRAPH_MIN_VALUE + 1);

    private static final int MIN_GRAPH_WIDTH_DP = 110; // see widget_layout.xml
    private static final int MIN_GRAPH_HEIGHT_DP = 40; // see widget_layout.xml

    private static final int GRAPH_LEFT_PADDING = 1; // see widget_layout.xml
    private static final int GRAPH_RIGHT_PADDING = 1; // see widget_layout.xml
    private static final int GRAPH_TOP_PADDING = 1; // see widget_layout.xml
    private static final int GRAPH_BOTTOM_PADDING = 1; // see widget_layout.xml

    private static final int MINUTE_IN_MS = (1000 * 60);

    private static final float DOT_RADIUS = 2.0f;
    private static final float DEF_DOT_PADDING = 1.5f;

    private static final int GRAPH_DATA_LEN = 48;
    private int[] graphData = new int[GRAPH_DATA_LEN];
    private long lastGraphUpdateMin = 0;

    private final int DEF_REFRESH_RATE_MIN = 5;
    private final int HIGH_REFRESH_RATE_MIN = 1;
    private int lastRefreshRate = DEF_REFRESH_RATE_MIN;

    private WidgetData lastWidgetData = new WidgetData();


    /** Initialize service, schedule the first update */
    public static void init(Context context) {
        Log.i(LOG_TAG, WidgetUpdateService.class.getSimpleName() + " initialization");
        DelayedWorker.schedule(context);
        WidgetUpdateService.scheduleTimeUpdate(context, 100);
    }

    @Override
    public void onCreate() {
        Log.i(LOG_TAG, getClass().getSimpleName() + " onCreate");
        super.onCreate();

        // restore state to recover from forcible process sleep
        Context context = getApplicationContext();
        graphData = PreferenceUtils.getIntArrayValue(context, KEY_GRAPH_DATA);
        if (graphData == null || graphData.length != GRAPH_DATA_LEN) {
            Log.i(LOG_TAG, getClass().getSimpleName() + " initializing graph data");
            graphData = new int[GRAPH_DATA_LEN];
        }
        lastGraphUpdateMin = PreferenceUtils.getLongValue(context, KEY_GRAPH_LAST_UPDATE, 0);
        lastRefreshRate = PreferenceUtils.getIntValue(context, KEY_GRAPH_REFRESH_RATE, DEF_REFRESH_RATE_MIN);
        lastWidgetData = WidgetData.fromJsonString(PreferenceUtils.getStringValue(context, KEY_LAST_WIDGET_DATA, null));
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, getClass().getSimpleName() + " onDestroy");

        // save state to be able to recover from forcible process sleep
        Context context = getApplicationContext();
        PreferenceUtils.setIntArrayValue(context, KEY_GRAPH_DATA, graphData);
        PreferenceUtils.setLongValue(context, KEY_GRAPH_LAST_UPDATE, lastGraphUpdateMin);
        PreferenceUtils.setIntValue(context, KEY_GRAPH_REFRESH_RATE, lastRefreshRate);
        PreferenceUtils.setStringValue(context, KEY_LAST_WIDGET_DATA, lastWidgetData == null ? null : lastWidgetData.toJsonString());

        super.onDestroy();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(LOG_TAG, getClass().getSimpleName() + " onStartJob");

        Context context = getApplicationContext();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, WidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widget);

        // abort paint if no widget is visible
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return false;
        }

        WidgetData widgetData = WidgetData.fromBundle(params.getExtras());
        String action = params.getExtras().getString("action");
        Log.i(LOG_TAG, getClass().getSimpleName() + " action requested: " + action);

        updateRefreshRate(context);

        setGlucoseDelta(widgetData);
        setTimeDelta(widgetData);
        updateGraphData(widgetData);

        lastWidgetData = getDataToDraw(widgetData);
        updateWidget(appWidgetManager, appWidgetIds, lastWidgetData);

        scheduleTimeUpdate(context, MINUTE_IN_MS); // delay 1 minute

        return true; // keep service running as long as possible
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(LOG_TAG, getClass().getSimpleName() + " onStopJob");

        return true;
    }

    private void updateWidget(AppWidgetManager appWidgetManager, int[] appWidgetIds, WidgetData widgetData) {
            Log.i(GWatchApplication.LOG_TAG, getClass().getSimpleName() + " updateWidget: " + widgetData);

        Context context = getApplicationContext();
        String sourcePackage = getSourceAppPackageToLaunch(context);

        for (int appWidgetId : appWidgetIds) {

            // get the layout for the App Widget and attach an on-click listener
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // update views' content
            updateRemoteViews(context, appWidgetManager, views, appWidgetId, new WidgetData(widgetData));

            // set on click handler
            views.setOnClickPendingIntent(R.id.widget_main, createLaunchPendingIntent(context, sourcePackage));

            // tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private boolean updateRefreshRate(Context context) {
        int refreshRate = getConfiguredRefreshRate(context);
        if (refreshRate != lastRefreshRate) {
            Arrays.fill(graphData, 0);
            lastRefreshRate = refreshRate;
            return true;
        }
        return false;
    }

    private void setTimeDelta(WidgetData widgetData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " updateTimeDelta");
        }

        long now = System.currentTimeMillis();

        if (widgetData.getTimestamp() == 0L) {
            if (lastWidgetData.getTimestamp() == 0L) {
                // in case of no timestamp info display 0
                widgetData.setTimeDelta(0);
            } else {
                // in case of no timestamp use current time
                int delta = (int) ((now - lastWidgetData.getTimestamp()) / MINUTE_IN_MS); // minutes
                widgetData.setTimeDelta(delta);
            }
        } else {
            // calculate current delta
            int delta = (int) ((now - widgetData.getTimestamp()) / MINUTE_IN_MS); // minutes
            widgetData.setTimeDelta(delta);
        }

        if (widgetData.getTimeDelta() > 24 * 60) { // more than 1 day -> reset
            widgetData.reset();
        }
    }

    private void setGlucoseDelta(WidgetData widgetData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " setGlucoseData");
        }

        // set glucose delta to be displayed
        if (lastWidgetData.getGlucose() == 0) {
            // in case of no previous data display 0
            widgetData.setGlucoseDelta(0);
        } else if (widgetData.getGlucose() == 0 || widgetData.getTimestamp() < lastWidgetData.getTimestamp()) {
            // in case of no BG data or old data use the previous value
            widgetData.setGlucoseDelta(lastWidgetData.getGlucoseDelta());
        } else {
            // calculate current delta
            widgetData.setGlucoseDelta(widgetData.getGlucose() - lastWidgetData.getGlucose());
        }
    }

    private WidgetData getDataToDraw(WidgetData widgetData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " getDataToDraw");
        }
        if (widgetData.getGlucose() == 0 || widgetData.getTimestamp() < lastWidgetData.getTimestamp()) {
            // in case of no BG data or old data use the previous value
            WidgetData dataToDraw = new WidgetData(lastWidgetData);
            dataToDraw.setTimeDelta(widgetData.getTimeDelta());
            dataToDraw.setGlucoseDelta(widgetData.getGlucoseDelta());
            return dataToDraw;
        }
        return widgetData;
    }

    private void updateRemoteViews(Context context, AppWidgetManager appWidgetManager, RemoteViews views, int widgetId, WidgetData widgetData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " updateRemoteViews: " + views.getPackage());
        }

        int color = PreferenceUtils.getIntValue(context, "pref_widget_background_color", ContextCompat.getColor(context, R.color.def_widget_graph_bkg));
        views.setInt(R.id.widget_background, "setBackgroundColor", color);

        int timeDelta = widgetData.getTimeDelta();
        final String timeDeltaStr = widgetData.getTimeDelta() > 60 ? String.format("%d hr %d min", timeDelta/60, timeDelta%60) : String.format("%d min", timeDelta);
        views.setTextViewText(R.id.widget_time_delta, timeDeltaStr);
        views.setTextColor(R.id.widget_time_delta, getTimeDeltaColor(context, widgetData.getTimeDelta()));

        views.setTextViewText(R.id.widget_source, widgetData.getSource());
        color = PreferenceUtils.getIntValue(context, "pref_widget_text_color_source", ContextCompat.getColor(context, R.color.def_widget_text));
        views.setTextColor(R.id.widget_source, color);

        if (widgetData.getGlucose() == 0) {
            int textColor = ContextCompat.getColor(context, R.color.def_widget_text);
            views.setTextViewText(R.id.widget_glucose, NO_DATA_TEXT);
            views.setTextColor(R.id.widget_glucose, textColor);

            views.setTextViewText(R.id.widget_glucose_delta, null);
            views.setTextColor(R.id.widget_glucose_delta, textColor);

            views.setTextViewText(R.id.widget_units, null);
            views.setTextColor(R.id.widget_units, textColor);

            views.setTextViewText(R.id.widget_units, null);
            views.setTextColor(R.id.widget_units, textColor);

            views.setTextViewText(R.id.widget_trend, null);
            views.setTextColor(R.id.widget_trend, Color.TRANSPARENT);
        } else {
            int colorByGlucose = getColorByGlucose(context, widgetData.getGlucose());

            boolean isUnitConv = PreferenceUtils.isConfigured(context, "cfg_glucose_units_conversion", false);
            views.setTextViewText(R.id.widget_glucose, getValueStrInUnits(widgetData.getGlucose(), isUnitConv));
            views.setTextColor(R.id.widget_glucose, colorByGlucose);

            String glDeltaStr = getDeltaStrInUnits(widgetData.getGlucoseDelta(), isUnitConv);
            views.setTextViewText(R.id.widget_glucose_delta, glDeltaStr);
            views.setTextColor(R.id.widget_glucose_delta, colorByGlucose);

            views.setTextViewText(R.id.widget_units, BgUtils.getGlucoseUnitsStr(isUnitConv));
            views.setTextColor(R.id.widget_units, colorByGlucose);

            Trend trend = widgetData.getTrend();
            if (trend == Trend.UNKNOWN) {
                trend = BgUtils.calcTrend(widgetData.getGlucoseDelta(), widgetData.getTimeDelta());
                if (BuildConfig.DEBUG) {
                    Log.i(LOG_TAG, WidgetUpdateService.class.getSimpleName() + " getTrendArrow: calculated trend: (bgd: "
                            + widgetData.getGlucoseDelta() + ", std: " + widgetData.getTimeDelta() + ") "
                            + trend);
                }
            }
            char arrow = BgUtils.getTrendChar(trend);
            views.setTextViewText(R.id.widget_trend, ""+arrow);
            views.setTextColor(R.id.widget_trend, getTrendColorId(context, trend));
        }
        views.setImageViewBitmap(R.id.widget_background, drawChart(context, appWidgetManager, widgetId));

        if (BuildConfig.DEBUG) {
            Log.v(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " " + Arrays.toString(graphData));
        }
    }

    private String getValueStrInUnits(int value, boolean isUnitConv) {
        return isUnitConv ? BgUtils.convertGlucoseToMmolLStr(value) : String.valueOf(value);
    }

    private String getDeltaStrInUnits(int value, boolean isUnitConv) {
        StringBuilder builder = new StringBuilder();
        builder.append(value < 0 ? StringUtils.EMPTY_STRING : "+");
        if (isUnitConv) {
            builder.append(BgUtils.convertGlucoseToMmolL2Str(value));
        } else {
            builder.append(value);
        }
        return builder.toString();
    }

    private int getColorByGlucose(Context context, int glucose) {
        int color;
        if (glucose <= PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_hypo", 70)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_text_color_hypo", ContextCompat.getColor(context, R.color.def_bg_hypo_color));
        } else if (glucose <= PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_low", 80)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_text_color_low", ContextCompat.getColor(context, R.color.def_bg_low_color));
        } else if (glucose < PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_high", 170)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_text_color_in_range", ContextCompat.getColor(context, R.color.def_widget_text));
        } else if (glucose < PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_hyper", 270)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_text_color_high", ContextCompat.getColor(context, R.color.def_bg_high_color));
        } else {
            color = PreferenceUtils.getIntValue(context, "pref_widget_text_color_hyper", ContextCompat.getColor(context, R.color.def_bg_hyper_color));
        }
        return color;
    }

    private int getTimeDeltaColor(Context context, int delta) {
        int color;
        int missedThreshold = Math.round(PreferenceUtils.getStringValueAsInt(context, "cfg_status_panel_no_data_time", 360)/60f);
        if (delta > missedThreshold) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_sample_time_color_missed", ContextCompat.getColor(context, R.color.def_widget_sample_expired));
        } else {
            color = PreferenceUtils.getIntValue(context, "pref_widget_sample_time_color_in_range", ContextCompat.getColor(context, R.color.def_widget_sample_time));
        }
        return color;
    }

    private int getTrendColorId(Context context, Trend trend) {
        switch (trend) {
            case UP_FAST:
            case UP:
            case DOWN:
            case DOWN_FAST:
                return PreferenceUtils.getIntValue(context, "pref_widget_trend_color_steep", ContextCompat.getColor(context, R.color.def_red));
            case UP_SLOW:
            case DOWN_SLOW:
                return PreferenceUtils.getIntValue(context, "pref_widget_trend_color_moderate", ContextCompat.getColor(context, R.color.def_orange));
            case FLAT:
                return PreferenceUtils.getIntValue(context, "pref_widget_trend_color_flat", ContextCompat.getColor(context, R.color.def_green));
            default:
                if (BuildConfig.DEBUG) {
                    Log.w(LOG_TAG, "getTrendColorId: returning NO COLOR");
                }
                return Color.TRANSPARENT;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // onclick handler and widget fonts

    private PendingIntent createLaunchPendingIntent(Context context, String sourcePackage) {
        Intent intent = null;
        if (sourcePackage != null) {
            try {
                intent = context.getPackageManager().getLaunchIntentForPackage(sourcePackage);
            } catch (Throwable t) {
                Log.w(GWatchApplication.LOG_TAG, t);
            }
        }
        if (intent == null) { // backup - launch this app
            intent = new Intent(context, MainActivity.class);
        }
        return PendingIntent.getActivity(context, 0, intent, AndroidUtils.getMutableFlag(true));
    }

    private String getSourceAppPackageToLaunch(Context context) {
        if (PreferenceUtils.isConfigured(context, "pref_widget_launch_glimp", false)) {
            return "it.ct.glicemia";
        } else if (PreferenceUtils.isConfigured(context, "pref_widget_launch_xdrip", false)) {
            return "com.eveningoutpost.dexdrip";
        } else if (PreferenceUtils.isConfigured(context, "pref_widget_launch_aaps", false)) {
            return "info.nightscout.androidaps";
        } else if (PreferenceUtils.isConfigured(context, "pref_widget_launch_diabox", false)) {
            return "com.outshineiot.diabox";
        } else if (PreferenceUtils.isConfigured(context, "pref_widget_launch_juggluco", false)) {
            return "tk.glucodata";
        } else if (PreferenceUtils.isConfigured(context, "pref_widget_launch_dexcom", false)) {
            return DexcomUtils.getInstalledDexcomAppPackage();
        } else if (PreferenceUtils.isConfigured(context, "pref_widget_launch_dexcom_follow", false)) {
            return DexcomUtils.getInstalledDexcomFollowAppPackage();
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Graph implementation

    private void updateGraphData(WidgetData widgetData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " updateGraphData: " + widgetData.toString());
        }

        final long nowMs = System.currentTimeMillis();
        final long now = nowMs / (long)MINUTE_IN_MS; // minutes
        if (now < 0) {
            Log.e(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " now is negative: " + now);
            return;
        }

        // shift graph data in time if needed
        if (lastGraphUpdateMin != 0) {
            // shift data left
            int roll = (int) ((now - lastGraphUpdateMin) / lastRefreshRate);
            if (roll > 0) {
                lastGraphUpdateMin = now;
                if (roll >= GRAPH_DATA_LEN) {
                    Arrays.fill(graphData, 0);
                } else {
                    graphData = Arrays.copyOfRange(graphData, roll, roll + GRAPH_DATA_LEN);
                }
            }
        } else {
            lastGraphUpdateMin = now;
        }

        // set new data
        if (widgetData.getGlucose() > 0) {
            int diff = (int) Math.round((nowMs - widgetData.getTimestamp())/(double)(lastRefreshRate * MINUTE_IN_MS));
            if (0 <= diff && diff < GRAPH_DATA_LEN) {
                int newValue = widgetData.getGlucose();
                int idx = GRAPH_DATA_LEN - 1 - diff;
                int oldValue = graphData[idx];
                graphData[idx] = oldValue == 0 ? newValue : (oldValue + newValue)/2; // kind of average
            }
//            lastGraphUpdateMin = now;
        }
    }

    private int getConfiguredRefreshRate(Context context) {
        boolean isHighRefreshRate = PreferenceUtils.isConfigured(context, "pref_widget_graph_1min_update", false);
        return isHighRefreshRate ? HIGH_REFRESH_RATE_MIN : DEF_REFRESH_RATE_MIN;
    }

    private Bitmap drawChart(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        int widgetWidth = appWidgetManager.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int widgetHeight = appWidgetManager.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, "Widget size: " + widgetWidth + " x " + widgetHeight);
        }

        int widgetLeftPadding = dpToPx(context, GRAPH_LEFT_PADDING);
        int widgetRightPadding = dpToPx(context, GRAPH_RIGHT_PADDING);
        int widgetTopPadding = dpToPx(context, GRAPH_TOP_PADDING);
        int widgetBottomPadding = dpToPx(context, GRAPH_BOTTOM_PADDING);

        int width = widgetWidth != 0 ? widgetWidth : dpToPx(context, MIN_GRAPH_WIDTH_DP);
        int height = widgetHeight != 0 ? widgetHeight : dpToPx(context, MIN_GRAPH_HEIGHT_DP);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        width -= widgetLeftPadding + widgetRightPadding;
        height -= widgetTopPadding + widgetBottomPadding;
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, "paint size: " + width + " x " + height);
        }
        float heightScale = height / GRAPH_VALUE_INT;

        float padding = DEF_DOT_PADDING;
        int count = (int)(width / (2*DOT_RADIUS + padding));
        if (count > GRAPH_DATA_LEN) {
            count = GRAPH_DATA_LEN;
            padding = (width - count * 2*DOT_RADIUS) / (float)count;
        }
        float graph_padding = (width - count * (2*DOT_RADIUS + padding))/2.0f;

        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, "count: " + count + ", r: " + DOT_RADIUS + ", pad: " + padding);
        }

        int offset = GRAPH_DATA_LEN - count;
        for (int i = offset; i < GRAPH_DATA_LEN; i++) {
            int value = graphData[i];
            if (value == 0) {
                continue;
            } else if (value < GRAPH_MIN_VALUE) {
                value = GRAPH_MIN_VALUE;
            }
            if (value > GRAPH_MAX_VALUE) {
                value = GRAPH_MAX_VALUE;
            }
            paint.setColor(getGraphPaintColor(context, value));

            value = value - GRAPH_MIN_VALUE;
            float x = widgetLeftPadding + graph_padding  + padding/2 + DOT_RADIUS + (2*DOT_RADIUS + padding) * (i - offset);
            float y = height + widgetTopPadding - (value * heightScale);

            if (BuildConfig.DEBUG) {
                Log.d(GWatchApplication.LOG_TAG, "draw: i = " + i + " [" + x + ", " + y + "]");
            }
            canvas.drawCircle(x, y, DOT_RADIUS, paint);
        }
        return bitmap;
    }

    private int dpToPx(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi/160f));
    }

    private int getGraphPaintColor(Context context, int value) {
        int color;
        if (value <= PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_hypo", 70)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_graph_color_hypo", ContextCompat.getColor(context, R.color.def_bg_hypo_color));
        } else if (value <= PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_low", 80)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_graph_color_low", ContextCompat.getColor(context, R.color.def_bg_low_color));
        } else if (value < PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_high", 170)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_graph_color_in_range", ContextCompat.getColor(context, R.color.def_bg_in_range_color));
        } else if (value < PreferenceUtils.getStringValueAsInt(context, "cfg_glucose_level_hyper", 270)) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_graph_color_high", ContextCompat.getColor(context, R.color.def_bg_high_color));
        } else {
            color = PreferenceUtils.getIntValue(context, "pref_widget_graph_color_hyper", ContextCompat.getColor(context, R.color.def_bg_hyper_color));
        }
        return color;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Data / layout changed notification
    // data update must be done on dispatcher thread
    // since on OREO+ jobs are not executed in doze mode

    public static void updateWidget(Packet packet) {
        PersistableBundle bundle = null;

        if (packet instanceof GlucosePacketBase) {
            GlucosePacketBase gp = (GlucosePacketBase) packet;
            if (gp.getGlucoseValue() == 0 || gp.getTimestamp() <= 0 /*|| gp.getTimestamp() == lastWidgetData.getTimestamp()*/) {
                // this usually happens in case of complex source packet, e.g. from AAPS
                // the BG sample is the same but additional data is new
                // in this case no widget refresh is needed
                return;
            }

            WidgetData widgetData = new WidgetData();
            widgetData.setSource(gp.getSource());
            widgetData.setTimestamp(gp.getTimestamp());
            widgetData.setGlucose(gp.getGlucoseValue());
            widgetData.setTrend(packet instanceof AAPSPacket
                    ? BgUtils.slopeArrowToTrend(((AAPSPacket)packet).getSlopeArrow())
                    : ((GlucosePacket)packet).getTrend());

            bundle = widgetData.toPersistableBundle("glucose");

        } else if (packet instanceof ConfigPacket) {
            bundle = new WidgetData().toPersistableBundle("config");
            if (BuildConfig.DEBUG) {
                Log.i(GWatchApplication.LOG_TAG, WidgetUpdateService.class.getSimpleName() + " config packet received");
            }
        }

        if (bundle != null) {
            Context context = GWatchApplication.getAppContext();
            ComponentName componentName = new ComponentName(context, WidgetUpdateService.class);
            JobInfo jobInfo = new JobInfo.Builder(WidgetUpdateService.WIDGET_JOB_ID, componentName)
                    .setExtras(bundle)
                    .setOverrideDeadline(1000) // max delay 1s
                    .setPersisted(true)
                    .build();
            JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(jobInfo);
        }
    }

    public static void scheduleTimeUpdate(Context context, long delayMs) {
        PersistableBundle bundle = new WidgetData().toPersistableBundle("time");

        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(WIDGET_JOB_ID, new ComponentName(context, WidgetUpdateService.class))
                .setExtras(bundle)
                .setMinimumLatency(delayMs);
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        jobScheduler.schedule(jobInfoBuilder.build());
    }
}
