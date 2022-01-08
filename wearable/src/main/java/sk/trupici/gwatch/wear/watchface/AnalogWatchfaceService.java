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

package sk.trupici.gwatch.wear.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.BackgroundPanel;
import sk.trupici.gwatch.wear.components.BgAlarmController;
import sk.trupici.gwatch.wear.components.BgGraph;
import sk.trupici.gwatch.wear.components.BgPanel;
import sk.trupici.gwatch.wear.components.ComplicationAttrs;
import sk.trupici.gwatch.wear.components.DatePanel;
import sk.trupici.gwatch.wear.components.WatchHands;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;
import sk.trupici.gwatch.wear.config.complications.Config;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_PREFIX;
import static sk.trupici.gwatch.wear.util.CommonConstants.PREF_CONFIG_CHANGED;

/**
 * Analog watch face with a ticking second hand.
 **/
public class AnalogWatchfaceService extends CanvasWatchFaceService {

    private static final String LOG_TAG = AnalogWatchfaceService.class.getSimpleName();

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = CommonConstants.SECOND_IN_MILLIS;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private AnalogWatchfaceConfig watchfaceConfig;

    @Override
    public Engine onCreateEngine() {
        watchfaceConfig = new AnalogWatchfaceConfig();
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<AnalogWatchfaceService.Engine> wfReference;

        public EngineHandler(AnalogWatchfaceService.Engine reference) {
            super(Looper.myLooper());
            wfReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            AnalogWatchfaceService.Engine engine = wfReference.get();
            if (engine != null) {
                if (msg.what == MSG_UPDATE_TIME) {
                    engine.handleUpdateTimeMessage();
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

        /* Handler to update the time once a second in interactive mode. */
        private final Handler updateTimeHandler = new EngineHandler(this);

        private boolean muted;

        private ComplicationAttrs leftComplicationAttrs;
        private ComplicationAttrs rightComplicationAttrs;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> activeComplicationDataSparseArray;

        // Used to pull user's preferences for background color, highlight color, and visual
        // indicating there are unread notifications.
        private SharedPreferences sharedPrefs;

        private float screenWidth;
        private float screenHeight;

        private RectF leftComplCoefs;
        private RectF rightComplCoefs;

        private WatchHands watchHands;
        private BackgroundPanel bkgPanel;
        private BgGraph bgGraph;
        private BgPanel bgPanel;
        private DatePanel datePanel;
        private BgAlarmController bgAlarmController;

        private final List<BroadcastReceiver> receivers = new ArrayList<>(8);

        private long lastMinute = 0L;

        Engine() {
            //  Ask for a hardware accelerated canvas.
            super(true);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(LOG_TAG, "onCreate: ");

            super.onCreate(holder);

            // signal restart
            ((Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE))
                    .vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));

            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchfaceService.this)
                    .setAcceptsTapEvents(true)
                    .build());

            // Used throughout watch face to pull user's preferences.
            Context context = getApplicationContext();
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPrefs.registerOnSharedPreferenceChangeListener(this);
            if (BuildConfig.DEBUG) {
                PreferenceUtils.dumpPreferences(sharedPrefs);
            }

            screenWidth = getResources().getDimensionPixelSize(R.dimen.layout_ref_screen_width);
            screenHeight = getResources().getDimensionPixelSize(R.dimen.layout_ref_screen_height);

            bkgPanel = new BackgroundPanel(watchfaceConfig);
            bkgPanel.onCreate(context, sharedPrefs);

            datePanel = new DatePanel((int) screenWidth, (int) screenHeight);
            datePanel.onCreate(context, sharedPrefs);

            watchHands = new WatchHands(watchfaceConfig);
            watchHands.onCreate(context, sharedPrefs);

            bgPanel = new BgPanel((int) screenWidth, (int) screenHeight);
            bgPanel.onCreate(context, sharedPrefs);

            bgGraph = new BgGraph((int) screenWidth, (int) screenHeight);
            bgGraph.onCreate(context, sharedPrefs);

            bgAlarmController = new BgAlarmController();
            bgAlarmController.onCreate(context, sharedPrefs);

            leftComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.layout_left_compl_left) / screenWidth,
                    getResources().getDimension(R.dimen.layout_left_compl_top) / screenHeight,
                    getResources().getDimension(R.dimen.layout_left_compl_right) / screenWidth,
                    getResources().getDimension(R.dimen.layout_left_compl_bottom) / screenHeight
            );
            leftComplicationAttrs = new ComplicationAttrs();
            leftComplicationAttrs.load(context, sharedPrefs, PREF_PREFIX + ComplicationConfig.LEFT_PREFIX);

            rightComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.layout_right_compl_left) / screenWidth,
                    getResources().getDimension(R.dimen.layout_right_compl_top) / screenHeight,
                    getResources().getDimension(R.dimen.layout_right_compl_right) / screenWidth,
                    getResources().getDimension(R.dimen.layout_right_compl_bottom) / screenHeight
            );
            rightComplicationAttrs = new ComplicationAttrs();
            rightComplicationAttrs.load(context, sharedPrefs, PREF_PREFIX + ComplicationConfig.RIGHT_PREFIX);

            initializeComplications();

            registerReceiver(context, bgPanel, CommonConstants.BG_RECEIVER_ACTION);
            registerReceiver(context, bgGraph, CommonConstants.BG_RECEIVER_ACTION);
            registerReceiver(context, bgAlarmController, CommonConstants.BG_RECEIVER_ACTION);
        }

        private void initializeComplications() {
            Log.d(LOG_TAG, "initializeComplications()");

            activeComplicationDataSparseArray = new SparseArray<>(Config.getComplicationCount());

            setDefaultSystemComplicationProvider (ComplicationId.LEFT_COMPLICATION_ID.ordinal(),
                    SystemProviders.WATCH_BATTERY, ComplicationData.TYPE_RANGED_VALUE);
            setDefaultSystemComplicationProvider (ComplicationId.RIGHT_COMPLICATION_ID.ordinal(),
                    SystemProviders.STEP_COUNT, ComplicationData.TYPE_SMALL_IMAGE);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face.
            Context context = getApplicationContext();

            ComplicationDrawable complicationDrawable = new ComplicationDrawable(context);
            Config.getComplicationConfig(ComplicationId.LEFT_COMPLICATION_ID)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, leftComplicationAttrs));

            complicationDrawable = new ComplicationDrawable(context);
            Config.getComplicationConfig(ComplicationId.RIGHT_COMPLICATION_ID)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, rightComplicationAttrs));

            setActiveComplications(Config.getComplicationIds());
        }

        private ComplicationDrawable updateComplicationDrawable(ComplicationDrawable drawable, ComplicationAttrs settings) {
            /*
            More settings to do:
                app:highlightColor="@color/fuchsia"
                app:rangedValuePrimaryColor="@color/teal"
                app:rangedValueRingWidth="1dp"
                app:rangedValueSecondaryColor="@color/white"
                app:textTypeface="sans-serif-condensed"
                app:titleSize="10sp"
                app:titleTypeface="sans-serif">
             */

            drawable.setBackgroundColorActive(settings.getBackgroundColor());
            drawable.setIconColorActive(settings.getDataColor());
            drawable.setTextColorActive(settings.getDataColor());
            drawable.setTitleColorActive(settings.getDataColor());

            int borderStyle = settings.getBorderDrawableStyle();
            drawable.setBorderStyleActive(borderStyle);
            if (borderStyle != ComplicationDrawable.BORDER_STYLE_NONE) {
                Log.d(LOG_TAG, "updateComplicationDrawable: " + settings.toString());
                drawable.setBorderColorActive(settings.getBorderColor());
                drawable.setBorderWidthActive(1);
                if (borderStyle == ComplicationDrawable.BORDER_STYLE_DASHED) {
                    drawable.setBorderWidthActive(1);
                    drawable.setBorderDashGapActive(1);
                    drawable.setBorderDashWidthActive(settings.isBorderDotted() ? 1 : 2);
                }

                if (settings.isBorderRounded()) {
                    drawable.setBorderRadiusActive(15);
                } else if (settings.isBorderRing()) {
                    drawable.setBorderRadiusActive(150);
                } else {
                    drawable.setBorderRadiusActive(0);
                }
            }

            drawable.setNoDataText(ComplicationConfig.NO_DATA_TEXT);
            return drawable;
        }

        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            Log.d(LOG_TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            activeComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            Config.getComplicationConfig(complicationId)
                    .getComplicationDrawable().setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);

            // unregister all receivers
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
            receivers.forEach(r -> {
                try {
                    localBroadcastManager.unregisterReceiver(r);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to unregister receiver: " + r.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
                }
            });

            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            Log.d(LOG_TAG, "onPropertiesChanged: " + properties);

            super.onPropertiesChanged(properties);

            Context context = getApplicationContext();
            bkgPanel.onPropertiesChanged(context, properties);
            bgPanel.onPropertiesChanged(context, properties);
            bgGraph.onPropertiesChanged(context, properties);
            datePanel.onPropertiesChanged(context, properties);

            watchHands.onPropertiesChanged(context, properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            for (ComplicationConfig complicationConfig : Config.getConfig()) {
                complicationConfig.getComplicationDrawable().setInAmbientMode(inAmbientMode);
            }

//            if (lowBitAmbient) {
//                boolean antiAlias = !inAmbientMode;
//                FIXME propagate antiAlias to all components
//            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            Log.d(LOG_TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (muted != inMuteMode) {
                muted = inMuteMode;
//                int alpha = inMuteMode ? 100 : 255;
//                FIXME propagate alpha to all components (corresponding paints)
//                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(LOG_TAG, "onSurfaceChanged: " + width + ", " + height);

            super.onSurfaceChanged(holder, format, width, height);

            adjustSize(width, height);
        }

        private void adjustSize(int width, int height) {
            this.screenWidth = width;
            this.screenHeight = height;

            Context context = getApplicationContext();
            bkgPanel.onSizeChanged(context, width, height);
            bgGraph.onSizeChanged(context, width, height);
            datePanel.onSizeChanged(context, width, height);
            bgPanel.onSizeChanged(context, width, height);

            watchHands.onSizeChanged(context, width, height);

            /*
             * Calculates location bounds for right and left circular complications. Please note,
             * we are not demonstrating a long text complication in this watch face.
             *
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */

            // left complication
            int left = (int) (leftComplCoefs.left * width);
            int top = (int) (leftComplCoefs.top * height);
            int right = (int) (leftComplCoefs.right * width);
            int bottom = (int) (leftComplCoefs.bottom * height);
            Rect bounds = new Rect(left, top, right, bottom);
            Config.getComplicationConfig(ComplicationId.LEFT_COMPLICATION_ID).getComplicationDrawable().setBounds(bounds);

            // right complication
            left = (int) (rightComplCoefs.left * width);
            top = (int) (rightComplCoefs.top * height);
            right = (int) (rightComplCoefs.right * width);
            bottom = (int) (rightComplCoefs.bottom * height);
            bounds = new Rect(left, top, right, bottom);
            Config.getComplicationConfig(ComplicationId.RIGHT_COMPLICATION_ID).getComplicationDrawable().setBounds(bounds);

//            Rect screenForBackgroundBound = new Rect(0, 0, width, height);
//            ComplicationDrawable backgroundComplicationDrawable = complicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
//            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(LOG_TAG, "OnTapCommand()");
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
//                    bgAlarmController.test(getApplicationContext());
                    // The user has completed the tap gesture.
                    // If your background complication is the first item in your array, you need
                    // to walk backward through the array to make sure the tap isn't for a
                    // complication above the background complication.
                    for (ComplicationConfig complicationConfig : Config.getConfig()) {
                        boolean successfulTap = complicationConfig.getComplicationDrawable().onTap(x, y);
                        if (successfulTap) {
                            return;
                        }
                    }
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            boolean isAmbientMode = isInAmbientMode();

            bgGraph.refresh(now, sharedPrefs);

            bkgPanel.onDraw(canvas, isAmbientMode);
            bgGraph.onDraw(canvas, isAmbientMode);
            datePanel.onDraw(canvas, isAmbientMode);
            datePanel.onDraw(canvas, isAmbientMode);
            bgPanel.onDraw(canvas, isAmbientMode);
            drawComplications(canvas, now);
            watchHands.onDraw(canvas, isAmbientMode);

            if (now - lastMinute > CommonConstants.MINUTE_IN_MILLIS) {
                bgAlarmController.handleNoDataAlarm(getApplicationContext());
                lastMinute = now;
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            for (ComplicationConfig complicationConfig : Config.getConfig()) {
                complicationConfig.getComplicationDrawable().draw(canvas, currentTimeMillis);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(LOG_TAG, "onVisibilityChanged: " + visible);

            super.onVisibilityChanged(visible);

            if (visible) {
                // Preferences might have changed since last time watch face was visible.
                Context context = getApplicationContext();

                leftComplicationAttrs.load(context, sharedPrefs, PREF_PREFIX + ComplicationConfig.LEFT_PREFIX);
                updateComplicationDrawable(Config.getComplicationConfig(ComplicationId.LEFT_COMPLICATION_ID)
                        .getComplicationDrawable(), leftComplicationAttrs);

                rightComplicationAttrs.load(context, sharedPrefs, PREF_PREFIX + ComplicationConfig.RIGHT_PREFIX);
                updateComplicationDrawable(Config.getComplicationConfig(ComplicationId.RIGHT_COMPLICATION_ID)
                        .getComplicationDrawable(), rightComplicationAttrs);

                bkgPanel.onConfigChanged(context, sharedPrefs);
                datePanel.onConfigChanged(context, sharedPrefs);
                bgPanel.onConfigChanged(context, sharedPrefs);
                bgGraph.onConfigChanged(context, sharedPrefs);
                watchHands.onConfigChanged(context, sharedPrefs);
                bgAlarmController.onConfigChanged(context, sharedPrefs);

                adjustSize((int)screenWidth, (int)screenHeight);

                datePanel.registerReceiver(AnalogWatchfaceService.this);
            } else {
                datePanel.unregisterReceiver(AnalogWatchfaceService.this);
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        /**
         * Starts/stops the {@link #updateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #updateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        /**
         * Register goven <code>BroadcastReceiver</code> for specified action
         * @param receiver <code>BroadcastReceiver</code> to register
         * @param action action to register receiver for
         */
        private void registerReceiver(Context context, BroadcastReceiver receiver, String action) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(action);
            intentFilter.setPriority(100);
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);
            receivers.add(receiver);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(LOG_TAG, "onSharedPreferenceChanged: " + key);
            if (PREF_CONFIG_CHANGED.equals(key)) {
                Log.d(LOG_TAG, "onSharedPreferenceChanged: signaled config change");
                Context context = getApplicationContext();
                bgPanel.onConfigChanged(context, sharedPrefs);
                bgGraph.onConfigChanged(context, sharedPrefs);
            }
        }
    }
}