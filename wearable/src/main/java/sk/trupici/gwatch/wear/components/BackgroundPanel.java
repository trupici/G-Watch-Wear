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
import android.graphics.Paint;
import android.os.Bundle;
import android.support.wearable.watchface.WatchFaceService;

import sk.trupici.gwatch.wear.config.ConfigPageData;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.util.UiUtils;

public class BackgroundPanel implements ComponentPanel {

    public static final String LOG_TAG = BackgroundPanel.class.getSimpleName();

    public static final int CONFIG_ID = 1;

//    final private int refScreenWidth;
//    final private int refScreenHeight;

    final private WatchfaceConfig watchfaceConfig;

    private Paint paint;
    private Paint ambientPaint;

    private Bitmap bitmap;

    private boolean lowBitAmbient;
    private boolean burnInProtection;

    public BackgroundPanel(int screenWidth, int screenHeight, WatchfaceConfig watchfaceConfig) {
//        this.refScreenWidth = screenWidth;
//        this.refScreenHeight = screenHeight;
        this.watchfaceConfig = watchfaceConfig;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        paint = UiUtils.createPaint();

        ambientPaint = UiUtils.createAmbientPaint();

        onConfigChanged(context, sharedPrefs);
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    watchfaceConfig.getSelectedItem(context, ConfigPageData.ConfigType.BACKGROUND).getResourceId()
            );
        }

        if (bitmap != null) {
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {
        bitmap = BitmapFactory.decodeResource(context.getResources(),
                watchfaceConfig.getSelectedItem(context, ConfigPageData.ConfigType.BACKGROUND).getResourceId()
        );
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        if (isAmbientMode) {
            if (lowBitAmbient || burnInProtection) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawBitmap(bitmap, null, canvas.getClipBounds(), ambientPaint);
            }
        } else {
            canvas.drawBitmap(bitmap, null, canvas.getClipBounds(), paint);
        }
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {
        lowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false);
        burnInProtection = properties.getBoolean(WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false);
    }

}
