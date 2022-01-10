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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.util.Log;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.ComplicationAttrs;
import sk.trupici.gwatch.wear.components.DigitalTimePanel;
import sk.trupici.gwatch.wear.config.DigitalWatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;

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

        private ComplicationAttrs topLeftComplAttrs;
        private ComplicationAttrs topRightComplAttrs;
        private ComplicationAttrs bottomLeftComplAttrs;
        private ComplicationAttrs bottomRightComplAttrs;

        private RectF topLeftComplCoefs;
        private RectF topRightComplCoefs;
        private RectF bottomLeftComplCoefs;
        private RectF bottomRightComplCoefs;

        private DigitalTimePanel timePanel;

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
            topLeftComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_left) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_top) / screenHeight,
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_right) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_left_compl_bottom) / screenHeight
            );
            topLeftComplAttrs = new ComplicationAttrs();
            topLeftComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.TOP_LEFT_PREFIX);

            topRightComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_left) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_top) / screenHeight,
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_right) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_top_right_compl_bottom) / screenHeight
            );
            topRightComplAttrs = new ComplicationAttrs();
            topRightComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.TOP_RIGHT_PREFIX);

            bottomLeftComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_left) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_top) / screenHeight,
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_right) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_left_compl_bottom) / screenHeight
            );
            bottomLeftComplAttrs = new ComplicationAttrs();
            bottomLeftComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.BOTTOM_LEFT_PREFIX);

            bottomRightComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_left) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_top) / screenHeight,
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_right) / screenWidth,
                    getResources().getDimension(R.dimen.digital_layout_bottom_right_compl_bottom) / screenHeight
            );
            bottomRightComplAttrs = new ComplicationAttrs();
            bottomRightComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.BOTTOM_RIGHT_PREFIX);

            // FIXME -> do not draw anything
            setDefaultSystemComplicationProvider (ComplicationId.TOP_LEFT.ordinal(), 0, ComplicationData.TYPE_NOT_CONFIGURED);
            setDefaultSystemComplicationProvider (ComplicationId.TOP_RIGHT.ordinal(), 0, ComplicationData.TYPE_NOT_CONFIGURED);
            setDefaultSystemComplicationProvider (ComplicationId.BOTTOM_LEFT.ordinal(), 0, ComplicationData.TYPE_NOT_CONFIGURED);
            setDefaultSystemComplicationProvider (ComplicationId.BOTTOM_RIGHT.ordinal(), 0, ComplicationData.TYPE_NOT_CONFIGURED);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face.
            ComplicationDrawable complicationDrawable = new ComplicationDrawable(context);
            complicationDrawable.setRangedValueProgressHidden(true);
            watchfaceConfig.getComplicationConfig(ComplicationId.TOP_LEFT)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, topLeftComplAttrs));

            complicationDrawable = new ComplicationDrawable(context);
            complicationDrawable.setRangedValueProgressHidden(true);
            watchfaceConfig.getComplicationConfig(ComplicationId.TOP_RIGHT)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, topRightComplAttrs));

            complicationDrawable = new ComplicationDrawable(context);
            complicationDrawable.setRangedValueProgressHidden(true);
            watchfaceConfig.getComplicationConfig(ComplicationId.BOTTOM_LEFT)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, bottomLeftComplAttrs));

            complicationDrawable = new ComplicationDrawable(context);
            complicationDrawable.setRangedValueProgressHidden(true);
            watchfaceConfig.getComplicationConfig(ComplicationId.BOTTOM_RIGHT)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, bottomRightComplAttrs));

            setActiveComplications(watchfaceConfig.getComplicationIds());
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

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            Log.d(LOG_TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationConfig complicationConfig = watchfaceConfig.getComplicationConfig(ComplicationId.valueOf(complicationId));
            complicationConfig.getComplicationDrawable().setComplicationData(complicationData);

            if (complicationData != null && complicationData.getType() == ComplicationData.TYPE_RANGED_VALUE) {
                float minValue = complicationData.getMinValue();
                float maxValue = complicationData.getMaxValue();
                float currentValue = complicationData.getValue();

                // TODO draw custom progress bar on the outer watch ring
            }

            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            Log.d(LOG_TAG, "onPropertiesChanged: " + properties);

            super.onPropertiesChanged(properties);

            Context context = getApplicationContext();
            timePanel.onPropertiesChanged(context, properties);
        }

        @Override
        protected void adjustSize(int width, int height) {
            super.adjustSize(width, height);
            Context context = getApplicationContext();
            timePanel.onSizeChanged(context, width, height);

            /*
             * Calculates location bounds for right and left circular complications. Please note,
             * we are not demonstrating a long text complication in this watch face.
             *
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */

            // top left complication
            int left = (int) (topLeftComplCoefs.left * width);
            int top = (int) (topLeftComplCoefs.top * height);
            int right = (int) (topLeftComplCoefs.right * width);
            int bottom = (int) (topLeftComplCoefs.bottom * height);
            Rect bounds = new Rect(left, top, right, bottom);
            watchfaceConfig.getComplicationConfig(ComplicationId.TOP_LEFT).getComplicationDrawable().setBounds(bounds);

            // top right complication
            left = (int) (topRightComplCoefs.left * width);
            top = (int) (topRightComplCoefs.top * height);
            right = (int) (topRightComplCoefs.right * width);
            bottom = (int) (topRightComplCoefs.bottom * height);
            bounds = new Rect(left, top, right, bottom);
            watchfaceConfig.getComplicationConfig(ComplicationId.TOP_RIGHT).getComplicationDrawable().setBounds(bounds);

            left = (int) (bottomLeftComplCoefs.left * width);
            top = (int) (bottomLeftComplCoefs.top * height);
            right = (int) (bottomLeftComplCoefs.right * width);
            bottom = (int) (bottomLeftComplCoefs.bottom * height);
            bounds = new Rect(left, top, right, bottom);
            watchfaceConfig.getComplicationConfig(ComplicationId.BOTTOM_LEFT).getComplicationDrawable().setBounds(bounds);

            // top right complication
            left = (int) (bottomRightComplCoefs.left * width);
            top = (int) (bottomRightComplCoefs.top * height);
            right = (int) (bottomRightComplCoefs.right * width);
            bottom = (int) (bottomRightComplCoefs.bottom * height);
            bounds = new Rect(left, top, right, bottom);
            watchfaceConfig.getComplicationConfig(ComplicationId.BOTTOM_RIGHT).getComplicationDrawable().setBounds(bounds);
        }

        @Override
        void drawCustomPanels(Canvas canvas, boolean isAmbientMode) {
            timePanel.onDraw(canvas, isAmbientMode);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(LOG_TAG, "onVisibilityChanged: " + visible);

            if (visible) {
                // Preferences might have changed since last time watch face was visible.
                Context context = getApplicationContext();

                topLeftComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.TOP_LEFT_PREFIX);
                updateComplicationDrawable(watchfaceConfig.getComplicationConfig(ComplicationId.TOP_LEFT).getComplicationDrawable(), topLeftComplAttrs);

                topRightComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.TOP_RIGHT_PREFIX);
                updateComplicationDrawable(watchfaceConfig.getComplicationConfig(ComplicationId.TOP_RIGHT).getComplicationDrawable(), topRightComplAttrs);

                bottomLeftComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.BOTTOM_LEFT_PREFIX);
                updateComplicationDrawable(watchfaceConfig.getComplicationConfig(ComplicationId.BOTTOM_LEFT).getComplicationDrawable(), bottomLeftComplAttrs);

                bottomRightComplAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.BOTTOM_RIGHT_PREFIX);
                updateComplicationDrawable(watchfaceConfig.getComplicationConfig(ComplicationId.BOTTOM_RIGHT).getComplicationDrawable(), bottomRightComplAttrs);

                timePanel.onConfigChanged(context, sharedPrefs);

                timePanel.registerReceiver(DigitalWatchfaceService.this);
            } else {
                timePanel.unregisterReceiver(DigitalWatchfaceService.this);
            }
            super.onVisibilityChanged(visible);
        }
    }
}