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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;

import java.util.Calendar;

import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.ConfigPageData;
import sk.trupici.gwatch.wear.config.complications.WatchfaceConfig;
import sk.trupici.gwatch.wear.util.CommonConstants;

public class WatchHands implements ComponentPanel {
    public static final String LOG_TAG = CommonConstants.LOG_TAG;


    // offsets for watch hands shadows - currently we use the same value for X and Y
    private static final float HOUR_HAND_SHADOW_OFFSET = 3;
    private static final float MINUTE_HAND_SHADOW_OFFSET = 5;
    private static final float SECOND_HAND_SHADOW_OFFSET = 7;

    private Paint paint;

    private Bitmap hourBitmap;
    private Bitmap hourShadowBitmap;

    private Bitmap minuteBitmap;
    private Bitmap minuteShadowBitmap;

    private Bitmap secondBitmap;
    private Bitmap secondShadowBitmap;

    final private WatchfaceConfig watchfaceConfig;

    public WatchHands(WatchfaceConfig watchfaceConfig) {
        this.watchfaceConfig = watchfaceConfig;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        paint = new Paint();
        paint.setAntiAlias(true);

        onConfigChanged(context, sharedPrefs);
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {
        ConfigPageData.HandsConfigData configData = (ConfigPageData.HandsConfigData)
                watchfaceConfig.getConfigItemData(ConfigPageData.ConfigType.HANDS,
                        sharedPrefs.getInt(AnalogWatchfaceConfig.PREF_HANDS_SET_IDX, AnalogWatchfaceConfig.DEF_HANDS_SET_IDX));

        hourBitmap = BitmapFactory.decodeResource(context.getResources(), configData.getHourHandId());
        hourShadowBitmap = configData.getHourHandShadowId() == 0 ? null
                : BitmapFactory.decodeResource(context.getResources(), configData.getHourHandShadowId());

        minuteBitmap = BitmapFactory.decodeResource(context.getResources(), configData.getMinuteHandId());
        minuteShadowBitmap = configData.getMinuteHandShadowId() == 0 ? null
                : BitmapFactory.decodeResource(context.getResources(), configData.getMinuteHandShadowId());

        secondBitmap = BitmapFactory.decodeResource(context.getResources(), configData.getSecondHandId());
        secondShadowBitmap = configData.getSecondHandShadowId() == 0 ? null
                : BitmapFactory.decodeResource(context.getResources(), configData.getSecondHandShadowId());
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {

    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        hourBitmap = Bitmap.createScaledBitmap(hourBitmap, width, height, true);
        if (hourShadowBitmap != null) {
            hourShadowBitmap = Bitmap.createScaledBitmap(hourShadowBitmap, width, height, true);
        }

        minuteBitmap = Bitmap.createScaledBitmap(minuteBitmap, width, height, true);
        if (minuteShadowBitmap != null) {
            minuteShadowBitmap = Bitmap.createScaledBitmap(minuteShadowBitmap, width, height, true);
        }

        secondBitmap = Bitmap.createScaledBitmap(secondBitmap, width, height, true);
        if (secondShadowBitmap != null) {
            secondShadowBitmap = Bitmap.createScaledBitmap(secondShadowBitmap, width, height, true);
        }
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        Calendar calendar = Calendar.getInstance();

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

        float centerX = hourBitmap.getWidth() / 2f;
        float centerY = hourBitmap.getHeight() / 2f;

        Matrix matrix = new Matrix();
        matrix.postRotate(hoursRotation, centerX, centerY);
        matrix.postTranslate(HOUR_HAND_SHADOW_OFFSET, HOUR_HAND_SHADOW_OFFSET);
        canvas.drawBitmap(hourShadowBitmap, matrix, paint);

        matrix.postTranslate(-HOUR_HAND_SHADOW_OFFSET, -HOUR_HAND_SHADOW_OFFSET);
        canvas.drawBitmap(hourBitmap, matrix, paint);

        matrix.reset();
        matrix.postRotate(minutesRotation, centerX, centerY);
        matrix.postTranslate(MINUTE_HAND_SHADOW_OFFSET, MINUTE_HAND_SHADOW_OFFSET);
        canvas.drawBitmap(minuteShadowBitmap, matrix, paint);

        matrix.postTranslate(-MINUTE_HAND_SHADOW_OFFSET, -MINUTE_HAND_SHADOW_OFFSET);
        canvas.drawBitmap(minuteBitmap, matrix, paint);

        if (!isAmbientMode) {
            matrix.reset();
            matrix.postRotate(secondsRotation, centerX, centerY);
            matrix.postTranslate(SECOND_HAND_SHADOW_OFFSET, SECOND_HAND_SHADOW_OFFSET);
            canvas.drawBitmap(secondShadowBitmap, matrix, paint);

            matrix.postTranslate(-SECOND_HAND_SHADOW_OFFSET, -SECOND_HAND_SHADOW_OFFSET);
            canvas.drawBitmap(secondBitmap, matrix, paint);
        }
    }

}
