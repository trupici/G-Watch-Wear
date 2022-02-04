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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.ComplicationAttrs;
import sk.trupici.gwatch.wear.components.DigitalTimePanel;
import sk.trupici.gwatch.wear.config.DigitalWatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;
import sk.trupici.gwatch.wear.util.UiUtils;

/**
 * Analog watch face with a ticking second hand.
 **/
public class DigitalWatchfaceService extends WatchfaceServiceBase {

    private static final String LOG_TAG = DigitalWatchfaceService.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        watchfaceConfig = new DigitalWatchfaceConfig();
        return new Engine();
    }

    protected class Engine extends WatchfaceServiceBase.Engine {

        private DigitalTimePanel timePanel;

        private Canvas canvas;
        private Bitmap bitmap;
        private Paint paint;
        private Paint clearPaint;
        private int backgroundPathColor;

        Paint ambientPaint;

        private int pbPadding;
        private int pbMaxAngle;

        private RectF pbCircleBounds;

        private Map<ComplicationId, ComplicationSettings> complSettingsMap;

        protected Engine() {
            //  Ask for a hardware accelerated canvas.
            super(true);
        }


        @Override
        protected void initializeCustomPanels(Context context, int screenWidth, int screenHeight) {
            timePanel = new DigitalTimePanel(screenWidth, screenHeight, watchfaceConfig);
            timePanel.onCreate(context, sharedPrefs);
        }

        @Override
        void initializeComplications(Context context) {

            bitmap = Bitmap.createBitmap((int) refScreenWidth, (int) refScreenHeight, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            paint = UiUtils.createPaint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(context.getResources().getInteger(R.integer.digital_compl_progressbar_width));

            clearPaint = UiUtils.createErasePaint();

            ambientPaint = UiUtils.createAmbientPaint();
            ambientPaint.setAlpha(0x80);

            backgroundPathColor = context.getColor(R.color.digital_compl_progressbar_bkg_color);

            pbPadding = context.getResources().getInteger(R.integer.digital_compl_progressbar_padding);
            pbMaxAngle = context.getResources().getInteger(R.integer.digital_compl_progressbar_angle);
            pbCircleBounds = new RectF(pbPadding, pbPadding, refScreenWidth -pbPadding, refScreenHeight -pbPadding);

            RectF topLeftComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_left) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_top) / refScreenHeight,
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_right) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_bottom) / refScreenHeight
            );
            RectF topRightComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_left) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_top) / refScreenHeight,
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_right) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_bottom) / refScreenHeight
            );
            RectF bottomLeftComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_left) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_top) / refScreenHeight,
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_right) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_bottom) / refScreenHeight
            );
            RectF bottomRightComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_left) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_top) / refScreenHeight,
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_right) / refScreenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_bottom) / refScreenHeight
            );

            float angleOffset = (90 - pbMaxAngle) / 2f;
            ProgressBarAttrs topLeftProgressBarAttrs = new ProgressBarAttrs(
                    90 + angleOffset,
                    pbMaxAngle,
                    new RectF(0, refScreenHeight /2f, refScreenWidth /2f, refScreenHeight)
            );
            ProgressBarAttrs topRightProgressBarAttrs = new ProgressBarAttrs(
                    270 + angleOffset,
                    pbMaxAngle,
                    new RectF(refScreenWidth /2f, 0, refScreenWidth, refScreenHeight /2f)
            );
            // no progress bar for bottom complications
            ProgressBarAttrs bottomLeftProgressBarAttrs = null;
            ProgressBarAttrs bottomRightProgressBarAttrs = null;

            complSettingsMap = new HashMap<>(4);
            complSettingsMap.put(ComplicationId.TOP_LEFT, new ComplicationSettings(
                    ComplicationId.TOP_LEFT,
                    watchfaceConfig.getPrefsPrefix() + ComplicationConfig.TOP_LEFT_PREFIX,
                    new ComplicationAttrs(),
                    topLeftProgressBarAttrs,
                    topLeftComplCoefs));
            complSettingsMap.put(ComplicationId.TOP_RIGHT, new ComplicationSettings(
                    ComplicationId.TOP_RIGHT,
                    watchfaceConfig.getPrefsPrefix() + ComplicationConfig.TOP_RIGHT_PREFIX,
                    new ComplicationAttrs(),
                    topRightProgressBarAttrs,
                    topRightComplCoefs));
            complSettingsMap.put(ComplicationId.BOTTOM_LEFT, new ComplicationSettings(
                    ComplicationId.BOTTOM_LEFT,
                    watchfaceConfig.getPrefsPrefix() + ComplicationConfig.BOTTOM_LEFT_PREFIX,
                    new ComplicationAttrs(),
                    bottomLeftProgressBarAttrs,
                    bottomLeftComplCoefs));
            complSettingsMap.put(ComplicationId.BOTTOM_RIGHT, new ComplicationSettings(
                    ComplicationId.BOTTOM_RIGHT,
                    watchfaceConfig.getPrefsPrefix() + ComplicationConfig.BOTTOM_RIGHT_PREFIX,
                    new ComplicationAttrs(),
                    bottomRightProgressBarAttrs,
                    bottomRightComplCoefs));

            for (ComplicationSettings settings : complSettingsMap.values()) {
                settings.getAttrs().load(context, sharedPrefs, settings.getPrefix());

                // Creates a ComplicationDrawable for each location where the user can render a
                // complication on the watch face.
                ComplicationDrawable complicationDrawable = new ComplicationDrawable(context);
                complicationDrawable.setRangedValueProgressHidden(true);
                watchfaceConfig.getComplicationConfig(settings.getId())
                        .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, settings.getAttrs()));
            }

            setActiveComplications(watchfaceConfig.getComplicationIds());
        }

        private ComplicationDrawable updateComplicationDrawable(ComplicationDrawable drawable, ComplicationAttrs attrs) {
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

            drawable.setBackgroundColorActive(attrs.getBackgroundColor());
            drawable.setIconColorActive(attrs.getDataColor());
            drawable.setTextColorActive(attrs.getDataColor());
            drawable.setTitleColorActive(attrs.getDataColor());

            int borderStyle = attrs.getBorderDrawableStyle();
            drawable.setBorderStyleActive(borderStyle);
            if (borderStyle != ComplicationDrawable.BORDER_STYLE_NONE) {
                drawable.setBorderColorActive(attrs.getBorderColor());
                drawable.setBorderWidthActive(1);
                if (borderStyle == ComplicationDrawable.BORDER_STYLE_DASHED) {
                    drawable.setBorderWidthActive(1);
                    drawable.setBorderDashGapActive(1);
                    drawable.setBorderDashWidthActive(attrs.isBorderDotted() ? 1 : 2);
                }

                if (attrs.isBorderRounded()) {
                    drawable.setBorderRadiusActive(15);
                } else if (attrs.isBorderRing()) {
                    drawable.setBorderRadiusActive(150);
                } else {
                    drawable.setBorderRadiusActive(0);
                }
            }

            drawable.setNoDataText(ComplicationConfig.NO_DATA_TEXT);

            return drawable;
        }

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onComplicationDataUpdate() id: " + complicationId);
            }
            // Updates correct ComplicationDrawable with updated data.
            ComplicationId id = ComplicationId.valueOf(complicationId);
            watchfaceConfig.getComplicationConfig(id).getComplicationDrawable().setComplicationData(complicationData);
            complSettingsMap.get(id).setComplicationData(complicationData);
            drawComplicationProgressBar(id);

            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onPropertiesChanged: " + properties);
            }
            super.onPropertiesChanged(properties);

            Context context = getApplicationContext();
            timePanel.onPropertiesChanged(context, properties);
        }

        @Override
        protected void adjustSize(int width, int height) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "adjustSize: " + width + " x " + height);
            }
            super.adjustSize(width, height);

            Context context = getApplicationContext();
            timePanel.onSizeChanged(context, width, height);

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            /*
             * Calculates location bounds for right and left circular complications.
             *
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */
            for (ComplicationSettings settings : complSettingsMap.values()) {
                RectF boundsCoefs = settings.getBoundsCoefs();
                watchfaceConfig.getComplicationConfig(settings.getId())
                        .getComplicationDrawable()
                        .setBounds(new Rect(
                                (int) (boundsCoefs.left * width),
                                (int) (boundsCoefs.top * height),
                                (int) (boundsCoefs.right * width),
                                (int) (boundsCoefs.bottom * height)
                        ));
            }

            // update progress bars bounds
            complSettingsMap.get(ComplicationId.TOP_LEFT).getProgressAttrs().setBounds(
                    new RectF(0, height/2f, width/2f, height)
            );
            complSettingsMap.get(ComplicationId.TOP_RIGHT).getProgressAttrs().setBounds(
                    new RectF(width/2f, 0, width, height/2f)
            );

            for (ComplicationId complicationId : complSettingsMap.keySet()) {
                drawComplicationProgressBar(complicationId);
            }
        }

        @Override
        void drawCustomPanels(Canvas canvas, boolean isAmbientMode) {
            timePanel.onDraw(canvas, isAmbientMode);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onVisibilityChanged: " + visible);
            }
            if (visible) {
                // Preferences might have changed since last time watch face was visible.
                Context context = getApplicationContext();

                for (ComplicationSettings settings : complSettingsMap.values()) {
                    ComplicationAttrs attrs = settings.getAttrs();
                    attrs.load(context, sharedPrefs, settings.getPrefix());
                    updateComplicationDrawable(watchfaceConfig.getComplicationConfig(settings.getId()).getComplicationDrawable(), attrs);
                }

                timePanel.onConfigChanged(context, sharedPrefs);

                timePanel.registerReceiver(DigitalWatchfaceService.this);
            } else {
                timePanel.unregisterReceiver(DigitalWatchfaceService.this);
            }
            super.onVisibilityChanged(visible);
        }

        @Override
        protected void drawComplications(Canvas canvas, boolean isAmbientMode) {
            super.drawComplications(canvas, isAmbientMode);

            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, getSurfaceHolder().getSurfaceFrame(), isAmbientMode ? ambientPaint : null);
            }
        }

        private void drawComplicationProgressBar(ComplicationId complicationId) {
            ComplicationSettings settings = complSettingsMap.get(complicationId);
            ProgressBarAttrs progressBarAttrs = settings.getProgressAttrs();
            if (progressBarAttrs == null) {
                return;
            }

            ComplicationAttrs complicationAttrs = settings.getAttrs();
            ComplicationData complicationData = settings.getComplicationData();


            // clear complication bitmap portion
            canvas.drawRect(progressBarAttrs.getBounds(), clearPaint);

            if (complicationData != null && complicationData.getType() == ComplicationData.TYPE_RANGED_VALUE) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "drawComplicationProgressBar: " + complicationId);
                }

                float minValue = complicationData.getMinValue();
                float maxValue = complicationData.getMaxValue();
                float currentValue = complicationData.getValue();

                float startAngle = progressBarAttrs.getStartAngle();
                float maxAngle = progressBarAttrs.getMaxAngle();

                // draw background path
                Path path = new Path();
                path.arcTo(pbCircleBounds, startAngle, maxAngle);
                paint.setColor(backgroundPathColor);
                canvas.drawPath(path, paint);

                float percent = 0;
                float range = Math.abs(maxValue - minValue); // avoid negative values
                if (range != 0) {
                    percent = Math.abs(currentValue - minValue) / range;
                    percent = Math.max(0, percent);
                    percent = Math.min(1, percent);
                }
                float sweepAngle = maxAngle * percent;

                // draw current value path
                path = new Path();
                path.arcTo(pbCircleBounds, startAngle, sweepAngle);
                paint.setColor(complicationAttrs.getDataColor());
                canvas.drawPath(path, paint);
            }
        }
    }

    private static class ProgressBarAttrs {
        final private float startAngle;
        final private float maxAngle;
        private RectF bounds; // drawing area (screen quarter)

        public ProgressBarAttrs(float startAngle, float maxAngle, RectF bounds) {
            this.startAngle = startAngle;
            this.maxAngle = maxAngle;
            this.bounds = bounds;
        }

        public float getStartAngle() {
            return startAngle;
        }

        public float getMaxAngle() {
            return maxAngle;
        }

        public RectF getBounds() {
            return bounds;
        }

        public void setBounds(RectF bounds) {
            this.bounds = bounds;
        }
    }

    private static class ComplicationSettings {
        final private ComplicationId id;
        final private String prefix;
        final private ComplicationAttrs attrs;
        final private ProgressBarAttrs progressAttrs;
        final private RectF boundsCoefs;
        private ComplicationData complicationData;

        public ComplicationSettings(ComplicationId id, String prefix, ComplicationAttrs attrs, ProgressBarAttrs progressAttrs, RectF boundsCoefs) {
            this.id = id;
            this.prefix = prefix;
            this.attrs = attrs;
            this.progressAttrs = progressAttrs;
            this.boundsCoefs = boundsCoefs;
        }

        public ComplicationId getId() {
            return id;
        }

        public String getPrefix() {
            return prefix;
        }

        public ComplicationAttrs getAttrs() {
            return attrs;
        }

        public ProgressBarAttrs getProgressAttrs() {
            return progressAttrs;
        }

        public RectF getBoundsCoefs() {
            return boundsCoefs;
        }

        public ComplicationData getComplicationData() {
            return complicationData;
        }

        public void setComplicationData(ComplicationData complicationData) {
            this.complicationData = complicationData;
        }
    }
}