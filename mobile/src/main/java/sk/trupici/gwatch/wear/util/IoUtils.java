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

package sk.trupici.gwatch.wear.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.RequiresApi;

public class IoUtils {

    private static final long VIBE_DURATION = 200;

    public static void vibrate(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            vibrateOld(context, VIBE_DURATION);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            vibrate8(context, VIBE_DURATION);
        } else {
            vibrate31(context, VIBE_DURATION);
        }
    }

    @SuppressWarnings("deprecation")
    private static void vibrateOld(Context context, long duration) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBE_DURATION);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void vibrate8(Context context, long duration) {
        @SuppressWarnings("deprecation")
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createOneShot(VIBE_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static void vibrate31(Context context, long duration) {
        @SuppressLint("WrongConstant")
        VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        vibratorManager.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(VIBE_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
    }

}
