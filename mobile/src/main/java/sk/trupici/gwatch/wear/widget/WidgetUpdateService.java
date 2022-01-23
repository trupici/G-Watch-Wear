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
import android.graphics.Paint;
import android.os.PersistableBundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Arrays;

import androidx.core.content.ContextCompat;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.ConfigPacket;
import sk.trupici.gwatch.wear.data.GlucosePacketBase;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.util.BgUtils;
import sk.trupici.gwatch.wear.util.DexcomUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.view.MainActivity;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

public class WidgetUpdateService extends JobService {

    public static final int WIDGET_JOB_ID = 8182;

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
    private static int[] graphData = new int[GRAPH_DATA_LEN];
    private static long lastGraphUpdateMin = 0;

    private static final int DEF_REFRESH_RATE_MIN = 5;
    private static final int HIGH_REFRESH_RATE_MIN = 1;
    private static int lastRefreshRate = DEF_REFRESH_RATE_MIN;

    private static WidgetData lastWidgetData = new WidgetData();

    @Override
    public boolean onStartJob(JobParameters params) {

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

        updateWidget(appWidgetManager, appWidgetIds, widgetData, action);

        jobFinished(params, false);
        if (action != null) {
            scheduleTimeUpdate(context, widgetData);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private void updateWidget(AppWidgetManager appWidgetManager, int[] appWidgetIds, WidgetData widgetData, String action) {
        Context context = getApplicationContext();

        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, "" + action + ": " + widgetData);
        }

        if (!"glucose".equals(action)) {
            updateRefreshRate(context);
            updateTimeDelta(widgetData);
            updateGraphData(widgetData, false);
        }

        boolean makeFontsSmaller = !UiUtils.isHighDensityDisplay(context);
        String sourcePackage = getSourceAppPackageToLaunch(context);

        for (int appWidgetId : appWidgetIds) {

            // get the layout for the App Widget and attach an on-click listener
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // update views' content
            updateRemoteViews(context, appWidgetManager, views, appWidgetId, new WidgetData(widgetData));

            // set on click handler
            views.setOnClickPendingIntent(R.id.widget_main, createLaunchPendingIntent(context, sourcePackage));

            // adjust fonts size
            if (makeFontsSmaller) {
                decreaseWidgetFontSizes(views);
            }

            // tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private void scheduleTimeUpdate(Context context, WidgetData widgetData) {
        PersistableBundle bundle = widgetData.toPersistableBundle("time");

        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(WIDGET_JOB_ID, new ComponentName(context, getClass()))
                .setExtras(bundle)
                .setMinimumLatency(MINUTE_IN_MS); // delay 1 min
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        jobScheduler.schedule(jobInfoBuilder.build());
    }

    private static boolean updateRefreshRate(Context context) {
        int refreshRate = getConfiguredRefreshRate(context);
        if (refreshRate != lastRefreshRate) {
            Arrays.fill(graphData, 0);
            lastRefreshRate = refreshRate;
            return true;
        }
        return false;
    }

    private static void updateTimeDelta(WidgetData widgetData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, "updateTimeDelta");
        }
        long now = System.currentTimeMillis();
        long timestamp = widgetData.getTimestamp();
        if (timestamp != 0 && now > timestamp) {
            int delta = (int) ((now - timestamp) / MINUTE_IN_MS); // minutes
            if (delta > 24 * 60) { // more than 1 day -> reset
                widgetData.reset();
            } else {
                widgetData.setTimeDelta(delta);
            }
        }
    }

    private static void updateRemoteViews(Context context, AppWidgetManager appWidgetManager, RemoteViews views, int widgetId, WidgetData widgetData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, "updateRemoteViews: " + views.getPackage());
        }

        if (widgetData == null) {
            Log.w(GWatchApplication.LOG_TAG, "updateRemoteViews: null data");
            widgetData = new WidgetData();
        }

        int color = PreferenceUtils.getIntValue(context, "pref_widget_background_color", ContextCompat.getColor(context, R.color.def_widget_graph_bkg));
        views.setInt(R.id.widget_background, "setBackgroundColor", color);

        int timeDelta = widgetData.getTimeDelta();
        String timeDeltaStr = timeDelta > 60 ? String.format("%d hr %d min", timeDelta/60, timeDelta%60) : String.format("%d min", timeDelta);
        views.setTextViewText(R.id.widget_time_delta, timeDeltaStr);
        views.setTextColor(R.id.widget_time_delta, getTimeDeltaColor(context, widgetData.getTimeDelta()));

        views.setTextViewText(R.id.widget_source, "LibreLink".equals(widgetData.getSource()) ? "Libre" : widgetData.getSource());
        color = PreferenceUtils.getIntValue(context, "pref_widget_text_color_source", ContextCompat.getColor(context, R.color.def_widget_text));
        views.setTextColor(R.id.widget_source, color);

//        views.setViewVisibility(R.id.widget_offline_indicator, ((GWatchApplication)context).isConnected() ? View.GONE : View.VISIBLE);

        if (widgetData.getGlucose() != 0) {

            int colorByGlucose = getColorByGlucose(context, widgetData.getGlucose());

            boolean isUnitConv = PreferenceUtils.isConfigured(context, "cfg_glucose_units_conversion", false);
            views.setTextViewText(R.id.widget_glucose, getValueStrInUnits(widgetData.getGlucose(), isUnitConv));
            views.setTextColor(R.id.widget_glucose, colorByGlucose);

            String glDeltaStr = getDeltaStrInUnits(widgetData.getGlucoseDelta(), isUnitConv, UiUtils.isHighDensityDisplay(context));
            views.setTextViewText(R.id.widget_glucose_delta, glDeltaStr);
            views.setTextColor(R.id.widget_glucose_delta, colorByGlucose);

            views.setTextViewText(R.id.widget_units, BgUtils.getGlucoseUnitsStr(isUnitConv));
            views.setTextColor(R.id.widget_units, colorByGlucose);

            views.setImageViewResource(R.id.widget_trend, getTrendIconId(widgetData.getGlucoseDelta(), widgetData.getSampleTimeDelta()));
            views.setInt(R.id.widget_trend,"setColorFilter", getTrendColorId(context, widgetData.getGlucoseDelta(), widgetData.getSampleTimeDelta()));

            views.setImageViewBitmap(R.id.widget_background, drawChart(context, appWidgetManager, widgetId));
        }
        if (BuildConfig.DEBUG) {
            Log.v(GWatchApplication.LOG_TAG, Arrays.toString(graphData));
        }
    }

    private static String getValueStrInUnits(int value, boolean isUnitConv) {
        return isUnitConv ? BgUtils.convertGlucoseToMmolLStr(value) : String.valueOf(value);
    }

    private static String getDeltaStrInUnits(int value, boolean isUnitConv, boolean highDensity) {
        StringBuilder builder = new StringBuilder();
        builder.append(value < 0 ? StringUtils.EMPTY_STRING : "+");
        if (isUnitConv) {
            if (highDensity) {
                builder.append(BgUtils.convertGlucoseToMmolL2Str(value));
            } else {
                builder.append(BgUtils.convertGlucoseToMmolLStr(value));
            }
        } else {
            builder.append(value);
        }
        return builder.toString();
    }

    private static int getColorByGlucose(Context context, int glucose) {
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

    private static int getTimeDeltaColor(Context context, int delta) {
        int color;
        int missedThreshold = Math.round(PreferenceUtils.getStringValueAsInt(context, "cfg_status_panel_no_data_time", 360)/60f);
        if (delta > missedThreshold) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_sample_time_color_missed", ContextCompat.getColor(context, R.color.def_widget_sample_expired));
        } else {
            color = PreferenceUtils.getIntValue(context, "pref_widget_sample_time_color_in_range", ContextCompat.getColor(context, R.color.def_widget_sample_time));
        }
        return color;
    }

    private static int getTrendIconId(int glucoseDelta, int sampleTimeDelta) {
        if (glucoseDelta < -2 * sampleTimeDelta) {
            return R.drawable.arrow_down;
        } else if (glucoseDelta < -sampleTimeDelta) {
            return R.drawable.arrow_down_skew;
        } else if (glucoseDelta < sampleTimeDelta) {
            return R.drawable.arrow_right;
        } else if (glucoseDelta < 2 * sampleTimeDelta) {
            return R.drawable.arrow_up_skew;
        } else {
            return R.drawable.arrow_up;
        }
    }

    private static int getTrendColorId(Context context, int glucoseDelta, int sampleTimeDelta) {
        int color;
        if (glucoseDelta < -2 * sampleTimeDelta) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_trend_color_steep", ContextCompat.getColor(context, R.color.def_red));
        } else if (glucoseDelta < -sampleTimeDelta) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_trend_color_moderate", ContextCompat.getColor(context, R.color.def_orange));
        } else if (glucoseDelta < sampleTimeDelta) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_trend_color_flat", ContextCompat.getColor(context, R.color.def_green));
        } else if (glucoseDelta < 2 * sampleTimeDelta) {
            color = PreferenceUtils.getIntValue(context, "pref_widget_trend_color_moderate", ContextCompat.getColor(context, R.color.def_orange));
        } else {
            color = PreferenceUtils.getIntValue(context, "pref_widget_trend_color_steep", ContextCompat.getColor(context, R.color.def_red));
        }
        return color;
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
        return PendingIntent.getActivity(context, 0, intent, 0);
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
        } else if (PreferenceUtils.isConfigured(context, "pref_widget_launch_dexcom", false)) {
            return DexcomUtils.getInstalledDexcomAppPackage();
        }
        return null;
    }

    private void decreaseWidgetFontSizes(RemoteViews views) {
        views.setTextViewTextSize(R.id.widget_time_delta, COMPLEX_UNIT_SP, 10);
        views.setTextViewTextSize(R.id.widget_source, COMPLEX_UNIT_SP, 10);
        views.setTextViewTextSize(R.id.widget_glucose, COMPLEX_UNIT_SP, 40);
        views.setTextViewTextSize(R.id.widget_glucose_delta, COMPLEX_UNIT_SP, 10);
        views.setTextViewTextSize(R.id.widget_units, COMPLEX_UNIT_SP, 10);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Graph implementation

    private static void updateGraphData(WidgetData widgetData, boolean isNewData) {
        if (BuildConfig.DEBUG) {
            Log.d(GWatchApplication.LOG_TAG, "updateGraphData: " + widgetData.toString());
        }

        final long now = System.currentTimeMillis() / (long)MINUTE_IN_MS; // minutes
        if (now < 0) {
            Log.e(GWatchApplication.LOG_TAG, "now is negative: " + now);
            return;
        }

        int refreshRateMin = getConfiguredRefreshRate(GWatchApplication.getAppContext());

        if (lastGraphUpdateMin != 0) {
            // shift data left
            int roll = (int) ((now - lastGraphUpdateMin) / refreshRateMin);
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
        if (isNewData) {
            long tsData = widgetData.getTimestamp() / MINUTE_IN_MS;
            int diff = (int) Math.round((now - tsData)/(double)refreshRateMin);
            if (0 <= diff && diff < GRAPH_DATA_LEN) {
                int newValue = widgetData.getGlucose();
                int idx = GRAPH_DATA_LEN - 1 - diff;
                int oldValue = graphData[idx];
                graphData[idx] = oldValue == 0 ? newValue : (oldValue + newValue)/2; // kind of average
            }
//            lastGraphUpdateMin = now;
        }
    }

    private static int getConfiguredRefreshRate(Context context) {
        boolean isHighRefreshRate = PreferenceUtils.isConfigured(context, "pref_widget_graph_1min_update", false);
        return isHighRefreshRate ? HIGH_REFRESH_RATE_MIN : DEF_REFRESH_RATE_MIN;
    }

    private static Bitmap drawChart(Context context, AppWidgetManager appWidgetManager, int widgetId) {
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
            Log.d(GWatchApplication.LOG_TAG, "Paint size: " + width + " x " + height);
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

    private static int dpToPx(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi/160f));
    }

    private static int getGraphPaintColor(Context context, int value) {
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
            if (gp.getGlucoseValue() == 0 || gp.getTimestamp() <= 0 || gp.getTimestamp() == lastWidgetData.getTimestamp()) {
                // this usually happens in case of complex source packet, e.g. from AAPS
                // the BG sample is the same but additional data is new
                // in this case no widget refresh is needed
                return;
            }

            WidgetData widgetData = new WidgetData();
            widgetData.setSource(gp.getSource());
            widgetData.setTimestamp(gp.getTimestamp());
            widgetData.setGlucose(gp.getGlucoseValue());
            widgetData.setTimeDelta(0);

            if (lastWidgetData.getTimestamp() != 0 && lastWidgetData.getTimestamp() > widgetData.getTimestamp()) {
                // in case of old value, do not update current status, just update graph data
                updateGraphData(widgetData, true);
                return;
            }
            widgetData.setGlucoseDelta(lastWidgetData.getGlucose() == 0
                    ? 0
                    : widgetData.getGlucose() - lastWidgetData.getGlucose());

            int delta = 0;
            if (lastWidgetData.getTimestamp() != 0 && widgetData.getTimestamp() > lastWidgetData.getTimestamp()) {
                delta = (int)(widgetData.getTimestamp() - lastWidgetData.getTimestamp()) / 1000; // delta in seconds
                delta = (int)Math.round((double)delta/60f);
            }
            widgetData.setSampleTimeDelta(delta == 0 ? 1 : delta); // can't be 0

            updateTimeDelta(widgetData);
            updateGraphData(widgetData, true);

            lastWidgetData = new WidgetData(widgetData);
            bundle = widgetData.toPersistableBundle("glucose");

        } else if (packet instanceof ConfigPacket) {
            bundle = lastWidgetData.toPersistableBundle("config");
            if (BuildConfig.DEBUG) {
                Log.i(GWatchApplication.LOG_TAG, "Config packet received. Sending data: " + lastWidgetData);
            }
        }

        if (bundle != null) {
            Context context = GWatchApplication.getAppContext();
            ComponentName componentName = new ComponentName(context, WidgetUpdateService.class);
            JobInfo jobInfo = new JobInfo.Builder(WidgetUpdateService.WIDGET_JOB_ID, componentName)
                    .setExtras(bundle)
                    .setOverrideDeadline(1000) // max delay 1s
                    .build();
            JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(WidgetUpdateService.WIDGET_JOB_ID);
            jobScheduler.schedule(jobInfo);
        }
    }
}
