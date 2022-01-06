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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.wearable.watchface.WatchFaceService;

import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.ConfigPageData;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;

public class BackgroundPanel implements ComponentPanel {

    public static final String LOG_TAG = BackgroundPanel.class.getSimpleName();

    public static final int CONFIG_ID = 1;

    private Paint backgroundPaint;
    private Bitmap backgroundBitmap;
    private Bitmap ambientBackgroundBitmap;

    private boolean lowBitAmbient;
    private boolean burnInProtection;

    final private WatchfaceConfig watchfaceConfig;

    public BackgroundPanel(WatchfaceConfig watchfaceConfig) {
        this.watchfaceConfig = watchfaceConfig;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        backgroundPaint = new Paint();
        onConfigChanged(context, sharedPrefs);
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {

        backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true);

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
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {
        backgroundBitmap = BitmapFactory.decodeResource(context.getResources(),
                watchfaceConfig.getConfigItemData(ConfigPageData.ConfigType.BACKGROUND,
                        sharedPrefs.getInt(AnalogWatchfaceConfig.PREF_BACKGROUND_IDX, AnalogWatchfaceConfig.DEF_BACKGROUND_IDX)
                ).getResourceId());
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        if (isAmbientMode) {
            if (lowBitAmbient || burnInProtection) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawBitmap(ambientBackgroundBitmap, 0, 0, backgroundPaint);
            }
        } else {
            canvas.drawBitmap(backgroundBitmap, 0, 0, backgroundPaint);
        }
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {
        lowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false);
        burnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false);
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

}
