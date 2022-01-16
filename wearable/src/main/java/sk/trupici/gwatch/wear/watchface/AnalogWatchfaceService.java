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
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.util.Log;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.ComplicationAttrs;
import sk.trupici.gwatch.wear.components.DatePanel;
import sk.trupici.gwatch.wear.components.WatchHands;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;

/**
 * Analog watch face with a ticking second hand.
 **/
public class AnalogWatchfaceService extends WatchfaceServiceBase {

    private static final String LOG_TAG = AnalogWatchfaceService.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        watchfaceConfig = new AnalogWatchfaceConfig();
        return new Engine();
    }

    protected class Engine extends WatchfaceServiceBase.Engine {

        private ComplicationAttrs leftComplicationAttrs;
        private ComplicationAttrs rightComplicationAttrs;

        private RectF leftComplCoefs;
        private RectF rightComplCoefs;

        private WatchHands watchHands;
        private DatePanel datePanel;

        protected Engine() {
            //  Ask for a hardware accelerated canvas.
            super(true);
        }


        @Override
        protected void initializeCustomPanels(Context context, int screenWidth, int screenHeight) {
            datePanel = new DatePanel(screenWidth, screenHeight, watchfaceConfig);
            datePanel.onCreate(context, sharedPrefs);

            watchHands = new WatchHands((AnalogWatchfaceConfig) watchfaceConfig);
            watchHands.onCreate(context, sharedPrefs);
        }

        @Override
        void initializeComplications(Context context) {
            leftComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.analog_layout_left_compl_left) / refScreenWidth,
                    getResources().getDimension(R.dimen.analog_layout_left_compl_top) / refScreenHeight,
                    getResources().getDimension(R.dimen.analog_layout_left_compl_right) / refScreenWidth,
                    getResources().getDimension(R.dimen.analog_layout_left_compl_bottom) / refScreenHeight
            );
            leftComplicationAttrs = new ComplicationAttrs();
            leftComplicationAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.LEFT_PREFIX);

            rightComplCoefs = new RectF(
                    getResources().getDimension(R.dimen.analog_layout_right_compl_left) / refScreenWidth,
                    getResources().getDimension(R.dimen.analog_layout_right_compl_top) / refScreenHeight,
                    getResources().getDimension(R.dimen.analog_layout_right_compl_right) / refScreenWidth,
                    getResources().getDimension(R.dimen.analog_layout_right_compl_bottom) / refScreenHeight
            );
            rightComplicationAttrs = new ComplicationAttrs();
            rightComplicationAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.RIGHT_PREFIX);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face.
            ComplicationDrawable complicationDrawable = new ComplicationDrawable(context);
            watchfaceConfig.getComplicationConfig(ComplicationId.LEFT)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, leftComplicationAttrs));

            complicationDrawable = new ComplicationDrawable(context);
            watchfaceConfig.getComplicationConfig(ComplicationId.RIGHT)
                    .setComplicationDrawable(updateComplicationDrawable(complicationDrawable, rightComplicationAttrs));

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
        public void onPropertiesChanged(Bundle properties) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onPropertiesChanged: " + properties);
            }
            super.onPropertiesChanged(properties);

            Context context = getApplicationContext();
            datePanel.onPropertiesChanged(context, properties);
            watchHands.onPropertiesChanged(context, properties);
        }

        @Override
        protected void adjustSize(int width, int height) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "adjustSize: " + width + " x " + height);
            }
            super.adjustSize(width, height);
            Context context = getApplicationContext();

            datePanel.onSizeChanged(context, width, height);
            watchHands.onSizeChanged(context, width, height);

            /*
             * Calculates location bounds for right and left circular complications.
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
            watchfaceConfig.getComplicationConfig(ComplicationId.LEFT).getComplicationDrawable().setBounds(bounds);

            // right complication
            left = (int) (rightComplCoefs.left * width);
            top = (int) (rightComplCoefs.top * height);
            right = (int) (rightComplCoefs.right * width);
            bottom = (int) (rightComplCoefs.bottom * height);
            bounds = new Rect(left, top, right, bottom);
            watchfaceConfig.getComplicationConfig(ComplicationId.RIGHT).getComplicationDrawable().setBounds(bounds);
        }

        @Override
        void drawCustomPanels(Canvas canvas, boolean isAmbientMode) {
            datePanel.onDraw(canvas, isAmbientMode);
            watchHands.onDraw(canvas, isAmbientMode);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onVisibilityChanged: " + visible);
            }
            if (visible) {
                // Preferences might have changed since last time watch face was visible.
                Context context = getApplicationContext();

                leftComplicationAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.LEFT_PREFIX);
                updateComplicationDrawable(watchfaceConfig.getComplicationConfig(ComplicationId.LEFT)
                        .getComplicationDrawable(), leftComplicationAttrs);

                rightComplicationAttrs.load(context, sharedPrefs, watchfaceConfig.getPrefsPrefix() + ComplicationConfig.RIGHT_PREFIX);
                updateComplicationDrawable(watchfaceConfig.getComplicationConfig(ComplicationId.RIGHT)
                        .getComplicationDrawable(), rightComplicationAttrs);

                datePanel.onConfigChanged(context, sharedPrefs);
                watchHands.onConfigChanged(context, sharedPrefs);

                datePanel.registerReceiver(AnalogWatchfaceService.this);
            } else {
                datePanel.unregisterReceiver(AnalogWatchfaceService.this);
            }
            super.onVisibilityChanged(visible);
        }
    }
}