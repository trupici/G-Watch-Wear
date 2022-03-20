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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.components.BackgroundPanel;
import sk.trupici.gwatch.wear.components.BgAlarmController;
import sk.trupici.gwatch.wear.components.BgGraphPanel;
import sk.trupici.gwatch.wear.components.BgPanel;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;
import sk.trupici.gwatch.wear.util.CommonConstants;

import static sk.trupici.gwatch.wear.common.util.CommonConstants.MINUTE_IN_MILLIS;
import static sk.trupici.gwatch.wear.common.util.CommonConstants.SECOND_IN_MILLIS;

/**
 * Watch face parent base class with common functionality
 **/
public abstract class WatchfaceServiceBase extends CanvasWatchFaceService {

    private static final String LOG_TAG = WatchfaceServiceBase.class.getSimpleName();

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = SECOND_IN_MILLIS;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    protected WatchfaceConfig watchfaceConfig;

    protected static class EngineHandler extends Handler {
        private final WeakReference<WatchfaceServiceBase.Engine> wfReference;

        public EngineHandler(WatchfaceServiceBase.Engine reference) {
            super(Looper.myLooper());
            wfReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchfaceServiceBase.Engine engine = wfReference.get();
            if (engine != null) {
                if (msg.what == MSG_UPDATE_TIME) {
                    engine.handleUpdateTimeMessage();
                }
            }
        }
    }

    protected abstract class Engine extends CanvasWatchFaceService.Engine {

        /* Handler to update the time once a second in interactive mode. */
        private final Handler updateTimeHandler = new EngineHandler(this);

        private boolean muted;

        // Used to pull user's preferences for background color, highlight color, and visual
        // indicating there are unread notifications.
        protected SharedPreferences sharedPrefs;

        protected float refScreenWidth;
        protected float refScreenHeight;

        private BackgroundPanel bkgPanel;
        private BgGraphPanel bgGraphPanel;
        private BgPanel bgPanel;
        private BgAlarmController bgAlarmController;

        private final List<BroadcastReceiver> receivers = new ArrayList<>(8);

        private long lastMinute = 0L;

        private boolean permissionChecked = false;

        abstract void initializeCustomPanels(Context context, int screenWidth, int screenHeight);
        abstract void drawCustomPanels(Canvas canvas, boolean isAmbientMode);
        abstract void initializeComplications(Context context);

        protected Engine(boolean useHardwareCanvas) {
            super(useHardwareCanvas);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(LOG_TAG, "onCreate: ");

            super.onCreate(holder);

            // signal restart
            ((Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE))
                    .vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchfaceServiceBase.this)
                    .setAcceptsTapEvents(true)
                    .build());

            // Used throughout watch face to pull user's preferences.
            Context context = getApplicationContext();
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (BuildConfig.DEBUG) {
                PreferenceUtils.dumpPreferences(sharedPrefs);
            }

            refScreenWidth = getResources().getDimensionPixelSize(R.dimen.layout_ref_screen_width);
            refScreenHeight = getResources().getDimensionPixelSize(R.dimen.layout_ref_screen_height);

            bkgPanel = new BackgroundPanel((int) refScreenWidth, (int) refScreenHeight, watchfaceConfig);
            bkgPanel.onCreate(context, sharedPrefs);

            bgPanel = new BgPanel((int) refScreenWidth, (int) refScreenHeight, watchfaceConfig);
            bgPanel.onCreate(context, sharedPrefs);

            bgGraphPanel = new BgGraphPanel((int) refScreenWidth, (int) refScreenHeight, watchfaceConfig);
            bgGraphPanel.onCreate(context, sharedPrefs);

            bgAlarmController = new BgAlarmController();
            bgAlarmController.onCreate(context, sharedPrefs);

            initializeComplications(context);
            initializeCustomPanels(context, (int) refScreenWidth, (int) refScreenHeight);

            registerReceiver(context, bgPanel, CommonConstants.BG_RECEIVER_ACTION, CommonConstants.REMOTE_CONFIG_ACTION);
            registerReceiver(context, bgGraphPanel, CommonConstants.BG_RECEIVER_ACTION, CommonConstants.REMOTE_CONFIG_ACTION);
            registerReceiver(context, bgAlarmController, CommonConstants.BG_RECEIVER_ACTION);
        }

        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            Log.d(LOG_TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationConfig complicationConfig = watchfaceConfig.getComplicationConfig(ComplicationId.valueOf(complicationId));
            complicationConfig.getComplicationDrawable().setComplicationData(complicationData);
            invalidate();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);

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
            bgGraphPanel.onPropertiesChanged(context, properties);
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
            for (ComplicationConfig complicationConfig : watchfaceConfig.getComplicationConfigs()) {
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

        protected void adjustSize(int width, int height) {
            Context context = getApplicationContext();
            bkgPanel.onSizeChanged(context, width, height);
            bgGraphPanel.onSizeChanged(context, width, height);
            bgPanel.onSizeChanged(context, width, height);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(LOG_TAG, "OnTapCommand()");
            if (tapType == TAP_TYPE_TAP) {
                // The user has completed the tap gesture.
                // If your background complication is the first item in your array, you need
                // to walk backward through the array to make sure the tap isn't for a
                // complication above the background complication.
                for (ComplicationConfig complicationConfig : watchfaceConfig.getComplicationConfigs()) {
                    boolean successfulTap = complicationConfig.getComplicationDrawable().onTap(x, y);
                    if (successfulTap) {
                        return;
                    }
                }
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            boolean isAmbientMode = isInAmbientMode();

            bgGraphPanel.refresh(now, sharedPrefs);

            bkgPanel.onDraw(canvas, isAmbientMode);
            bgGraphPanel.onDraw(canvas, isAmbientMode);
            bgPanel.onDraw(canvas, isAmbientMode);
            drawComplications(canvas, isAmbientMode);
            drawCustomPanels(canvas, isAmbientMode);

            if (now - lastMinute > MINUTE_IN_MILLIS) {
                bgAlarmController.handleNoDataAlarm(getApplicationContext());
                lastMinute = now;
            }
        }

        protected void drawComplications(Canvas canvas, boolean isAmbientMode) {
            long currentTimeMillis = System.currentTimeMillis();
            for (ComplicationConfig complicationConfig : watchfaceConfig.getComplicationConfigs()) {
                complicationConfig.getComplicationDrawable().draw(canvas, currentTimeMillis);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(LOG_TAG, "onVisibilityChanged: " + visible);

            super.onVisibilityChanged(visible);

            if (visible) {
                Context context = getApplicationContext();
                checkAndRequestComplicationPermission(context);

                // Preferences might have changed since last time watch face was visible.
                bkgPanel.onConfigChanged(context, sharedPrefs);
                bgPanel.onConfigChanged(context, sharedPrefs);
                bgGraphPanel.onConfigChanged(context, sharedPrefs);
                bgAlarmController.onConfigChanged(context, sharedPrefs);

                Rect surfaceRect = getSurfaceHolder().getSurfaceFrame();
                adjustSize(surfaceRect.width(), surfaceRect.height());
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
         * @param actions actions to register receiver for
         */
        private void registerReceiver(Context context, BroadcastReceiver receiver, String ... actions) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.setPriority(100);
            for (String action : actions) {
                intentFilter.addAction(action);
            }
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);
            receivers.add(receiver);
        }

        protected void checkAndRequestComplicationPermission(Context context) {
            if (!permissionChecked) {
                permissionChecked = true;
                int status = ContextCompat.checkSelfPermission(context,"com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA");
                Log.d(LOG_TAG, "checkAndRequestComplicationPermission: " + status);
                if (status == PermissionChecker.PERMISSION_DENIED) {
                    Intent permissionRequestIntent = ComplicationHelperActivity.createPermissionRequestHelperIntent(context, new ComponentName(context, this.getClass()));
                    permissionRequestIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(permissionRequestIntent);
                }
            }
        }
    }
}