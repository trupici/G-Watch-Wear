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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.bgchart.SimpleBgChart;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.ConfigPageData;
import sk.trupici.gwatch.wear.config.complications.BorderType;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;
import sk.trupici.gwatch.wear.config.complications.Config;
import sk.trupici.gwatch.wear.util.BgUtils;
import sk.trupici.gwatch.wear.util.DumpUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.StringUtils;

import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BKG_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BORDER_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BORDER_SHAPE;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_DATA_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_TEXT_SIZE;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_PREFIX;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_DASH_LEN;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_DOT_LEN;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_GAP_LEN;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_RING_RADIUS;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_ROUND_RECT_RADIUS;
import static sk.trupici.gwatch.wear.config.complications.ComplicationAdapter.BORDER_WIDTH;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class StandardAnalogWatchfaceService extends CanvasWatchFaceService {

    public static final String LOG_TAG = "StdAnalogGWatchface";

    // offsets for watch hands shadows - currently we use the same value for X and Y
    public static final float HOUR_HAND_SHADOW_OFFSET = 3;
    public static final float MINUTE_HAND_SHADOW_OFFSET = 5;
    public static final float SECOND_HAND_SHADOW_OFFSET = 7;

    public static final int CHART_BOTTOM_MARGIN = 10;

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 1000; //TimeUnit.SECONDS.toMillis(1);

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
        private final WeakReference<StandardAnalogWatchfaceService.Engine> wfReference;

        public EngineHandler(StandardAnalogWatchfaceService.Engine reference) {
            super(Looper.myLooper());
            wfReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            StandardAnalogWatchfaceService.Engine engine = wfReference.get();
            if (engine != null) {
                if (msg.what == MSG_UPDATE_TIME) {
                    engine.handleUpdateTimeMessage();
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        /* Handler to update the time once a second in interactive mode. */
        private final Handler updateTimeHandler = new EngineHandler(this);
        private Calendar calendar;
        private DateFormat dayOfWeekFormat;
        private DateFormat dateFormat;

        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean timeZoneRegistered = false;
        private boolean muted;

        private float centerX;
        private float centerY;

        private Paint handsPaint;

        private Bitmap hourBitmap;
        private Bitmap hourShadowBitmap;

        private Bitmap minuteBitmap;
        private Bitmap minuteShadowBitmap;

        private Bitmap secondBitmap;
        private Bitmap secondShadowBitmap;

        private Paint backgroundPaint;
        private Bitmap backgroundBitmap;
        private Bitmap ambientBackgroundBitmap;

        private SimpleBgChart chart;

        private boolean ambientMode;
        private boolean lowBitAmbient;
        private boolean burnInProtection;

        private ComplicationSettings leftComplSettings;
        private ComplicationSettings rightComplSettings;
        private ComplicationSettings centerComplSettings;
        private ComplicationSettings bottomComplSettings;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> activeComplicationDataSparseArray;

        // Used to pull user's preferences for background color, highlight color, and visual
        // indicating there are unread notifications.
        private SharedPreferences sharedPrefs;

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(LOG_TAG, "onCreate: ");

            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(StandardAnalogWatchfaceService.this)
                    .setAcceptsTapEvents(true)
                    .build());

            calendar = Calendar.getInstance();

            // Used throughout watch face to pull user's preferences.
            Context context = getApplicationContext();
            sharedPrefs = context.getSharedPreferences(
                    getString(R.string.standard_analog_complication_preferences_key),
                    Context.MODE_PRIVATE);

            loadSavedPreferences();

            initializeBackground();
            initializeComplications();
            initializeHands();

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            int left = (int) (100 * size.x / 450f);
            int top = (int) (85 * size.y / 450f);
            int width = (int) (250 * size.x / 450f);
            int height = (int) (110 * size.y / 450f);
            chart = new SimpleBgChart(
                    left, top,
                    width, height,
                    sharedPrefs);
        }


        // Pulls all user's preferences for watch face appearance.
        private void loadSavedPreferences() {
            PreferenceUtils.dumpPreferences(sharedPrefs);

            leftComplSettings = new ComplicationSettings();
            leftComplSettings.load(sharedPrefs, PREF_PREFIX + ComplicationConfig.LEFT_PREFIX);
//            Log.d(LOG_TAG, "LEFT: " + leftComplSettings.toString());

            rightComplSettings = new ComplicationSettings();
            rightComplSettings.load(sharedPrefs, PREF_PREFIX + ComplicationConfig.RIGHT_PREFIX);
//            Log.d(LOG_TAG, "RIGHT: " + rightComplSettings.toString());

            centerComplSettings = new ComplicationSettings();
            centerComplSettings.load(sharedPrefs, PREF_PREFIX + ComplicationConfig.CENTER_PREFIX);
//            Log.d(LOG_TAG, "CENTER: " + centerComplSettings.toString());

            bottomComplSettings = new ComplicationSettings();
            bottomComplSettings.load(sharedPrefs, PREF_PREFIX + ComplicationConfig.BOTTOM_PREFIX);
//            Log.d(LOG_TAG, "BOTTOM: " + bottomComplSettings.toString());
        }

        private void initializeComplications() {
            Log.d(LOG_TAG, "initializeComplications()");

            activeComplicationDataSparseArray = new SparseArray<>(Config.getComplicationCount());

            setDefaultSystemComplicationProvider (ComplicationId.LEFT_COMPLICATION_ID.ordinal(),
                    SystemProviders.WATCH_BATTERY, ComplicationData.TYPE_RANGED_VALUE);
            setDefaultSystemComplicationProvider (ComplicationId.RIGHT_COMPLICATION_ID.ordinal(),
                    SystemProviders.STEP_COUNT, ComplicationData.TYPE_SMALL_IMAGE);
            setDefaultSystemComplicationProvider(ComplicationId.CENTER_COMPLICATION_ID.ordinal(),
                    SystemProviders.DAY_AND_DATE, ComplicationData.TYPE_NOT_CONFIGURED);
            setDefaultComplicationProvider (ComplicationId.BOTTOM_COMPLICATION_ID.ordinal(),
                    null, ComplicationData.TYPE_EMPTY);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face.
            Context context = getApplicationContext();

            ComplicationDrawable complicationDrawable = new ComplicationDrawable(context);
            Config.getComplicationConfig(ComplicationId.LEFT_COMPLICATION_ID)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, leftComplSettings));

            complicationDrawable = new ComplicationDrawable(context);
            Config.getComplicationConfig(ComplicationId.RIGHT_COMPLICATION_ID)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, rightComplSettings));

            complicationDrawable = new ComplicationDrawable(context);
            Config.getComplicationConfig(ComplicationId.CENTER_COMPLICATION_ID)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, centerComplSettings));

            // to be changed
            complicationDrawable = new ComplicationDrawable(context);
            bottomComplSettings.setFontSize(80);
            ComplicationDrawable drawable = updateComplicationDrawable(complicationDrawable, bottomComplSettings);
            drawable.setTitleSizeActive(80);
            Config.getComplicationConfig(ComplicationId.BOTTOM_COMPLICATION_ID)
                    .setComplicationDrawable(drawable);


            setActiveComplications(Config.getComplicationIds());
        }

        private ComplicationDrawable updateComplicationDrawable(ComplicationDrawable drawable, ComplicationSettings settings) {
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
            if (settings.getFontSize() != -1) {
                drawable.setTextSizeActive(settings.getFontSize());
                drawable.setTitleSizeActive(settings.getFontSize());
            }

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

            DumpUtils.dumpComplicationData(getApplicationContext(), complicationData);
            if (complicationId == ComplicationId.BOTTOM_COMPLICATION_ID.ordinal()) {
                processBgValue(complicationData);
            }

            // Updates correct ComplicationDrawable with updated data.
            Config.getComplicationConfig(complicationId)
                    .getComplicationDrawable().setComplicationData(complicationData);

            invalidate();
        }

        private void processBgValue(ComplicationData complicationData) {
            if (complicationData.getShortText() == null) {
                return;
            }

            // this should be faster then regex
            CharSequence cs = complicationData.getShortText().getText(getApplicationContext(), 0).toString();
            String strValue = cs.chars()
                    .filter(c -> (Character.isDigit(c) || c == '.' || c == ','))
                    .mapToObj(x -> String.valueOf((char) x))
                    .collect(Collectors.joining());

            Double value = null;
            try {
                value = Double.valueOf(strValue.replace(',', '.'));
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
            }

            // check if in required units
            if (value != null) {
                if (value < BgUtils.MMOL_L_BOUNDARY_VALUE) { // convert to
                    value = BgUtils.convertGlucoseToMgDl(value);
                }

                chart.updateGraphData(value, System.currentTimeMillis());
            }

            Log.d(LOG_TAG, "Received BG value: " + cs.toString() + " : " + strValue + " : " + value);
        }


        private void initFormats() {
            dayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            dayOfWeekFormat.setCalendar(calendar);
            dateFormat = android.text.format.DateFormat.getDateFormat(StandardAnalogWatchfaceService.this);
            dateFormat.setCalendar(calendar);
        }


        private void initializeBackground() {
            backgroundPaint = new Paint();
            backgroundBitmap = BitmapFactory.decodeResource(getResources(),
                    watchfaceConfig.getConfigItemData(ConfigPageData.ConfigType.BACKGROUND,
                            sharedPrefs.getInt(AnalogWatchfaceConfig.PREF_BACKGROUND_IDX, AnalogWatchfaceConfig.DEF_BACKGROUND_IDX)
                    ).getResourceId());
            ambientBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.analog_bkg_ambient);
        }

        private void initializeHands() {
            ConfigPageData.HandsConfigData handsData = (ConfigPageData.HandsConfigData)
                    watchfaceConfig.getConfigItemData(ConfigPageData.ConfigType.HANDS,
                        sharedPrefs.getInt(AnalogWatchfaceConfig.PREF_HANDS_SET_IDX, AnalogWatchfaceConfig.DEF_HANDS_SET_IDX));

            handsPaint = new Paint();
            handsPaint.setAntiAlias(true);

            hourBitmap = BitmapFactory.decodeResource(getResources(), handsData.getHourHandId());
            hourShadowBitmap = handsData.getHourHandShadowId() == 0 ? null
                    : BitmapFactory.decodeResource(getResources(), handsData.getHourHandShadowId());

            minuteBitmap = BitmapFactory.decodeResource(getResources(), handsData.getMinuteHandId());
            minuteShadowBitmap = handsData.getMinuteHandShadowId() == 0 ? null
                    : BitmapFactory.decodeResource(getResources(), handsData.getMinuteHandShadowId());

            secondBitmap = BitmapFactory.decodeResource(getResources(), handsData.getSecondHandId());
            secondShadowBitmap = handsData.getSecondHandShadowId() == 0 ? null
                    : BitmapFactory.decodeResource(getResources(), handsData.getSecondHandShadowId());
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            Log.d(LOG_TAG, "onPropertiesChanged: " + properties);

        // TODO update complication drawables

            super.onPropertiesChanged(properties);
            lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            ambientMode = inAmbientMode;

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            for (ComplicationConfig complicationConfig : Config.getConfig()) {
                complicationConfig.getComplicationDrawable().setInAmbientMode(ambientMode);
            }

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
                handsPaint.setAlpha(inMuteMode ? 100 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(LOG_TAG, "onSurfaceChanged: " + width + ", " + height);

            super.onSurfaceChanged(holder, format, width, height);

            adjustSize(width, height);
        }

        private void adjustSize(int width, int height) {

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            centerX = width / 2f;
            centerY = height / 2f;

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) width) / (float) backgroundBitmap.getWidth();
            backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap,
                    (int) (backgroundBitmap.getWidth() * scale),
                    (int) (backgroundBitmap.getHeight() * scale), true);

            chart.scale(scale, scale);

            scale = ((float) width) / (float) hourBitmap.getWidth();
            int wScale = (int) (hourBitmap.getWidth() * scale);
            int hScale = (int) (hourBitmap.getHeight() * scale);

            hourBitmap = Bitmap.createScaledBitmap(hourBitmap, wScale, hScale, true);
            if (hourShadowBitmap != null) {
                hourShadowBitmap = Bitmap.createScaledBitmap(hourShadowBitmap, wScale, hScale, true);
            }

            minuteBitmap = Bitmap.createScaledBitmap(minuteBitmap, wScale, hScale, true);
            if (minuteShadowBitmap != null) {
                minuteShadowBitmap = Bitmap.createScaledBitmap(minuteShadowBitmap, wScale, hScale, true);
            }

            secondBitmap = Bitmap.createScaledBitmap(secondBitmap, wScale, hScale, true);
            if (secondShadowBitmap != null) {
                secondShadowBitmap = Bitmap.createScaledBitmap(secondShadowBitmap, wScale, hScale, true);
            }

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don"t want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren"t
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!burnInProtection && !lowBitAmbient) {
                initGrayBackgroundBitmap();
            }

            /*
             * Calculates location bounds for right and left circular complications. Please note,
             * we are not demonstrating a long text complication in this watch face.
             *
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */

            // left complication
            int left = width / 8; // 3/24
            int top = height / 2; // 12/24
            int cWidth = width * 5/24;   // 5/24
            int cHeight = height * 5/24; // 5/24

            Rect bounds = new Rect(left, top,left + cWidth,top + cHeight);
            Config.getComplicationConfig(ComplicationId.LEFT_COMPLICATION_ID)
                    .getComplicationDrawable().setBounds(bounds);

            // right complication
            left = width - cWidth - width / 8;
            bounds = new Rect(left, top,left + cWidth,top + cHeight);
            Config.getComplicationConfig(ComplicationId.RIGHT_COMPLICATION_ID)
                    .getComplicationDrawable().setBounds(bounds);

            // center complication
            left = width / 3;      // 8/24
            top = height * 13/24;  // 13/24
            cWidth = width / 3;    // 8/24
            cHeight = height / 12; // 2/24
            bounds = new Rect(left, top,left + cWidth,top + cHeight);
            Config.getComplicationConfig(ComplicationId.CENTER_COMPLICATION_ID)
                    .getComplicationDrawable().setBounds(bounds);

            // bottom complication
//            left = width / 3;     // 8/24
            top = height * 15/24; // 15/24
//            cWidth = width / 3;      // 8/24
            cHeight = height * 5/24; // 5/24
            bounds = new Rect(left, top,left + cWidth,top + cHeight);
            Config.getComplicationConfig(ComplicationId.BOTTOM_COMPLICATION_ID)
                    .getComplicationDrawable().setBounds(bounds);

//            Rect screenForBackgroundBound = new Rect(0, 0, width, height);
//            ComplicationDrawable backgroundComplicationDrawable = complicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
//            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);
        }

        private void initGrayBackgroundBitmap() {
            ambientBackgroundBitmap = Bitmap.createBitmap(
                    backgroundBitmap.getWidth(),
                    backgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(ambientBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(backgroundBitmap, 0, 0, grayPaint);
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
            calendar.setTimeInMillis(now);

            drawBackground(canvas);
            chart.draw(canvas, ambientMode);
            drawComplications(canvas, now);
            drawWatchFace(canvas);
        }

        private void drawBackground(Canvas canvas) {
            if (ambientMode) {
                if (lowBitAmbient || burnInProtection) {
                    canvas.drawColor(Color.BLACK);
                } else {
                    canvas.drawBitmap(ambientBackgroundBitmap, 0, 0, backgroundPaint);
                }
            } else {
                canvas.drawBitmap(backgroundBitmap, 0, 0, backgroundPaint);
            }
        }

        private void drawWatchFace(Canvas canvas) {

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds = (calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final int minutes = calendar.get(Calendar.MINUTE);
            final float minutesHandOffset = seconds / 10f;
            final float minutesRotation = calendar.get(Calendar.MINUTE) * 6f + minutesHandOffset;

            final float hourHandOffset = minutes / 2f;
            final float hoursRotation = calendar.get(Calendar.HOUR) * 30f + hourHandOffset;


            Matrix matrix = new Matrix();
            matrix.postRotate(hoursRotation, centerX, centerY);
            matrix.postTranslate(HOUR_HAND_SHADOW_OFFSET, HOUR_HAND_SHADOW_OFFSET);
            canvas.drawBitmap(hourShadowBitmap, matrix, handsPaint);

            matrix.postTranslate(-HOUR_HAND_SHADOW_OFFSET, -HOUR_HAND_SHADOW_OFFSET);
            canvas.drawBitmap(hourBitmap, matrix, handsPaint);

            matrix.reset();
            matrix.postRotate(minutesRotation, centerX, centerY);
            matrix.postTranslate(MINUTE_HAND_SHADOW_OFFSET, MINUTE_HAND_SHADOW_OFFSET);
            canvas.drawBitmap(minuteShadowBitmap, matrix, handsPaint);

            matrix.postTranslate(-MINUTE_HAND_SHADOW_OFFSET, -MINUTE_HAND_SHADOW_OFFSET);
            canvas.drawBitmap(minuteBitmap, matrix, handsPaint);

            if (!ambientMode) {
                matrix.reset();
                matrix.postRotate(secondsRotation, centerX, centerY);
                matrix.postTranslate(SECOND_HAND_SHADOW_OFFSET, SECOND_HAND_SHADOW_OFFSET);
                canvas.drawBitmap(secondShadowBitmap, matrix, handsPaint);

                matrix.postTranslate(-SECOND_HAND_SHADOW_OFFSET, -SECOND_HAND_SHADOW_OFFSET);
                canvas.drawBitmap(secondBitmap, matrix, handsPaint);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            for (ComplicationConfig complicationConfig : Config.getConfig()) {
                if (complicationConfig.getComplicationId() == ComplicationId.BOTTOM_COMPLICATION_ID) {
                    drawBGComplication(complicationConfig, bottomComplSettings, canvas, currentTimeMillis);
                } else {
                    complicationConfig.getComplicationDrawable().draw(canvas, currentTimeMillis);
                }
            }
        }

        private void drawBGComplication(ComplicationConfig complicationConfig, ComplicationSettings complicationSettings, Canvas canvas, long currentTimeMillis) {
            Rect bounds = complicationConfig.getComplicationDrawable().getBounds();
            ComplicationData complicationData = activeComplicationDataSparseArray.get(complicationConfig.getId());
            if (complicationData != null) {
                if (!isInAmbientMode()) {
                    Paint paint = new Paint();
                    paint.setColor(complicationSettings.getBackgroundColor());
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    if (complicationSettings.isBorderRounded()) {
                        canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
                    } else if (complicationSettings.isBorderRing()) {
                        canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, BORDER_RING_RADIUS, BORDER_RING_RADIUS, paint);
                    } else {
                        canvas.drawRect(bounds, paint);
                    }

                    if (complicationSettings.getBorderType() != BorderType.NONE) {
                        paint = new Paint();
                        paint.setColor(complicationSettings.getBorderColor());
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(BORDER_WIDTH);
                        if (complicationSettings.getBorderDrawableStyle() == ComplicationDrawable.BORDER_STYLE_DASHED) {
                            if (complicationSettings.isBorderDotted()) {
                                paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DOT_LEN, BORDER_GAP_LEN}, 0f));
                            } else {
                                paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DASH_LEN, BORDER_GAP_LEN}, 0f));
                            }
                        }
                        if (complicationSettings.isBorderRounded()) {
                            canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
                        } else if (complicationSettings.isBorderRing()) {
                            canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, BORDER_RING_RADIUS, BORDER_RING_RADIUS, paint);
                        } else {
                            canvas.drawRect(bounds, paint);
                        }
                    }
                }

                float x = bounds.left + bounds.width() / 2f; // text will be centered around
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(complicationSettings.getDataColor());
                textPaint.setAntiAlias(!isInAmbientMode());
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(bounds.height() / 2f);
                textPaint.setFakeBoldText(true);

                canvas.drawText(complicationData.getShortText() == null ? ComplicationConfig.NO_DATA_TEXT
                                : complicationData.getShortText().getText(getApplicationContext(), currentTimeMillis).toString(),
                        x, bounds.top + bounds.height() / 2f, textPaint);
                textPaint.setTextSize(bounds.height() / 3f);
                textPaint.setFakeBoldText(false);
                canvas.drawText(complicationData.getShortText() == null ? ComplicationConfig.NO_DATA_TEXT
                                : complicationData.getShortTitle().getText(getApplicationContext(), currentTimeMillis).toString(),
                        x, bounds.bottom - bounds.height() / 10f, textPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(LOG_TAG, "onVisibilityChanged: " + visible);

            super.onVisibilityChanged(visible);

            if (visible) {
                // Preferences might have changed since last time watch face was visible.
                loadSavedPreferences();

                updateComplicationDrawable(Config.getComplicationConfig(ComplicationId.LEFT_COMPLICATION_ID).getComplicationDrawable(), leftComplSettings);
                updateComplicationDrawable(Config.getComplicationConfig(ComplicationId.RIGHT_COMPLICATION_ID).getComplicationDrawable(), rightComplSettings);
                updateComplicationDrawable(Config.getComplicationConfig(ComplicationId.CENTER_COMPLICATION_ID).getComplicationDrawable(), centerComplSettings);
                updateComplicationDrawable(Config.getComplicationConfig(ComplicationId.BOTTOM_COMPLICATION_ID).getComplicationDrawable(), bottomComplSettings);

                initializeBackground();
                initializeHands();
                adjustSize((int)centerX * 2, (int)centerY * 2);

                registerReceiver();
                /* Update time zone in case it changed while we weren"t visible. */
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (timeZoneRegistered) {
                return;
            }
            timeZoneRegistered = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            StandardAnalogWatchfaceService.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!timeZoneRegistered) {
                return;
            }
            timeZoneRegistered = false;
            StandardAnalogWatchfaceService.this.unregisterReceiver(timeZoneReceiver);
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
            return isVisible() && !ambientMode;
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

        ///

        class ComplicationSettings {
            private int dataColor;
            private int backgroundColor;
            private int borderColor;
            private BorderType borderType;
            private int fontSize; // [sp]

            public ComplicationSettings() {
                init();
            }

            public void init() {
                borderType = BorderType.NONE;
                borderColor = Color.TRANSPARENT;
                dataColor = Color.WHITE;
                backgroundColor = Color.TRANSPARENT;
                fontSize = -1;
            }

            public void load(SharedPreferences prefs, String prefix) {
                borderType = BorderType.getByNameOrDefault(prefs.getString(prefix + PREF_COMPL_BORDER_SHAPE, null));
                borderColor = prefs.getInt(prefix + PREF_COMPL_BORDER_COLOR, Color.TRANSPARENT);
                dataColor = prefs.getInt(prefix + PREF_COMPL_DATA_COLOR, Color.WHITE);
                backgroundColor = prefs.getInt(prefix + PREF_COMPL_BKG_COLOR, Color.TRANSPARENT);
                fontSize = prefs.getInt(prefix + PREF_COMPL_TEXT_SIZE, -1);
            }

            public int getBorderDrawableStyle() {
                switch (borderType) {
                    case RECT:
                    case ROUNDED_RECT:
                    case RING:
                        return ComplicationDrawable.BORDER_STYLE_SOLID;
                    case DASHED_RECT:
                    case DASHED_ROUNDED_RECT:
                    case DASHED_RING:
                    case DOTTED_RECT:
                    case DOTTED_ROUNDED_RECT:
                    case DOTTED_RING:
                        return ComplicationDrawable.BORDER_STYLE_DASHED;
                    default:
                        return ComplicationDrawable.BORDER_STYLE_NONE;
                }
            }

            public boolean isBorderDotted() {
                switch (borderType) {
                    case DOTTED_RECT:
                    case DOTTED_ROUNDED_RECT:
                    case DOTTED_RING:
                        return true;
                    default:
                        return false;
                }
            }

            public boolean isBorderRounded() {
                switch (borderType) {
                    case ROUNDED_RECT:
                    case DOTTED_ROUNDED_RECT:
                    case DASHED_ROUNDED_RECT:
                        return true;
                    default:
                        return false;
                }
            }

            public boolean isBorderRing() {
                switch (borderType) {
                    case RING:
                    case DOTTED_RING:
                    case DASHED_RING:
                        return true;
                    default:
                        return false;
                }
            }

            public int getDataColor() {
                return dataColor;
            }

            public void setDataColor(int dataColor) {
                this.dataColor = dataColor;
            }

            public int getBorderColor() {
                return borderColor;
            }

            public void setBorderColor(int borderColor) {
                this.borderColor = borderColor;
            }

            public int getBackgroundColor() {
                return backgroundColor;
            }

            public void setBackgroundColor(int backgroundColor) {
                this.backgroundColor = backgroundColor;
            }

            public BorderType getBorderType() {
                return borderType;
            }

            public void setBorderType(BorderType borderType) {
                this.borderType = borderType;
            }

            public int getFontSize() {
                return fontSize;
            }

            public void setFontSize(int fontSize) {
                this.fontSize = fontSize;
            }

            @Override
            public String toString() {
                return "\nData color: " + StringUtils.formatColorStr(dataColor)
                        + "\nBackground color: " + StringUtils.formatColorStr((backgroundColor))
                        + "\nBorder type: " + (borderType == null ? "null" : borderType.name())
                        + "\nBorder color: " + StringUtils.formatColorStr(borderColor)
                        + "\nFont size: " + fontSize;
            }
        }
    }
}