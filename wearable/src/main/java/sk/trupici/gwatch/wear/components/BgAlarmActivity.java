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
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.view.WearableDialogActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.CommonConstants;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;

public class BgAlarmActivity extends WearableDialogActivity {

    public static final String LOG_TAG = BgAlarmActivity.class.getSimpleName();

    final public static String ACTION = "sk.trupici.gwatch.wear.BG_ALARM";

    final public static String EXTRAS_ALARM_CONFIG = "alarm_cfg";
    final public static String EXTRAS_SOUNDS_CONFIG = "sounds_cfg";
    final public static String EXTRAS_BG_VALUE = "bg_value";
    final public static String EXTRAS_ALARM_TEXT = "text";
    final public static String EXTRAS_ALARM_TEXT_COLOR = "color";

    private static final String WAKE_LOCK_TAG = "gwatch.wear:" + BgAlarmActivity.class.getSimpleName() + ".wake_lock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 180000; // 3 minutes
    private PowerManager.WakeLock wakeLock;


    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    // timer to finish the alarm after configured time
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = () -> {
        Log.d(LOG_TAG, "Timer has elapsed");
        finish();
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        PowerManager powerManager = (PowerManager)getApplicationContext().getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        if (wakeLock.isHeld()) {
            Log.w(LOG_TAG, "AlarmActivity: wake log is already held, exiting...");
            return;
        }
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_alarm);

        Bundle extras = getIntent().getExtras();
        final BgAlarmController.AlarmBasicConfig sounds = (BgAlarmController.AlarmBasicConfig) extras.getSerializable(EXTRAS_SOUNDS_CONFIG);
        final BgAlarmController.AlarmConfig alarmConfig = (BgAlarmController.AlarmConfig) extras.getSerializable(EXTRAS_ALARM_CONFIG);
        final String bgValue = extras.getString(EXTRAS_BG_VALUE, null);
        final String text = extras.getString(EXTRAS_ALARM_TEXT, null);
        final int textColor = extras.getInt(EXTRAS_ALARM_TEXT_COLOR, 0);
        if (text == null) {
            Log.e(LOG_TAG, "AlarmActivity: invalid text: " + bgValue);
            return;
        }

        TextView textView = findViewById(R.id.alarm_text);
        textView.setText(text);
        textView.setTextColor(textColor);

        textView = findViewById(R.id.bg_value);
        textView.setText(StringUtils.notNullString(bgValue));
        textView.setTextColor(textColor);

        startAlarm(alarmConfig, sounds);

        if (alarmConfig.type == BgAlarmController.Type.CRITICAL) {
            findViewById(R.id.snooze_button).setVisibility(View.INVISIBLE);
        } else {
            findViewById(R.id.snooze_button).setOnClickListener(view -> {
                String prefName;
                if (alarmConfig.type == BgAlarmController.Type.NO_DATA) {
                    prefName = BgAlarmController.PREF_NO_DATA_LAST_SNOOZED_AT;
                } else if (alarmConfig.type == BgAlarmController.Type.FAST_DROP) {
                    prefName = BgAlarmController.PREF_FAST_DROP_LAST_SNOOZED_AT;
                } else {
                    prefName = BgAlarmController.PREF_LAST_SNOOZED_AT;
                }
                PreferenceUtils.setLongValue(getApplicationContext(), prefName,  System.currentTimeMillis());
                finish();
            });
        }

        findViewById(R.id.dismiss_button).setOnClickListener(view -> finish());
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        try {
            stopAlarm();
        } finally {
            super.onDestroy();
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void startAlarm(BgAlarmController.AlarmConfig alarmConfig, BgAlarmController.AlarmBasicConfig sounds) {
        Log.d(LOG_TAG, "startAlarm");

        if (alarmConfig.type == BgAlarmController.Type.NO_DATA) {
            PreferenceUtils.setLongValue(getApplicationContext(), BgAlarmController.PREF_NO_DATA_LAST_TRIGGERED_AT, System.currentTimeMillis());
        } else if (alarmConfig.type == BgAlarmController.Type.FAST_DROP) {
            PreferenceUtils.setLongValue(getApplicationContext(), BgAlarmController.PREF_FAST_DROP_LAST_TRIGGERED_AT, System.currentTimeMillis());
        } else {
            PreferenceUtils.setLongValue(getApplicationContext(), BgAlarmController.PREF_LAST_TRIGGERED_AT, System.currentTimeMillis());
            PreferenceUtils.setStringValue(getApplicationContext(), BgAlarmController.PREF_LAST_ALARM_TYPE, alarmConfig.type.name());
        }

        AudioAttributes aa = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build();

        if (sounds != null) {
            mediaPlayer = MediaPlayer.create(getApplicationContext(), alarmConfig.soundResId);
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.setVolume(alarmConfig.volume, alarmConfig.volume);
                mediaPlayer.setAudioAttributes(aa);
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
                mediaPlayer.setOnErrorListener((mp, what, extra) -> false);
            }
        }

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        VibrationEffect effect = createVibrationEffect(alarmConfig.vibrationResId, alarmConfig.intensity);
        vibrator.vibrate(effect, aa);

        // schedule timer to finish
        Log.d(LOG_TAG, "Starting timer for: " + alarmConfig.duration * CommonConstants.SECOND_IN_MILLIS);
        timerHandler.postDelayed(timerRunnable, alarmConfig.duration * CommonConstants.SECOND_IN_MILLIS);
    }

    protected void stopAlarm() {
        Log.d(LOG_TAG, "stopAlarm");

        // stop timer
        timerHandler.removeCallbacks(timerRunnable);

        if (vibrator != null) {
            vibrator.cancel();
        }

        /* Your action on positive button clicked. */
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }


    private VibrationEffect createVibrationEffect(int resId, int intensity) {
        if (intensity <= 0) {
            intensity = VibrationEffect.DEFAULT_AMPLITUDE;
        }

        int[] pattern = getResources().getIntArray(resId);
        long[] timings = new long[pattern.length];
        int[] amps = new int[pattern.length];
        for (int i=0; i < pattern.length; i++) {
            timings[i] = pattern[i];
            if ((i & 0x01) == 0) {
                amps[i] = intensity;
            }
        }
        return VibrationEffect.createWaveform(timings, amps, 0);
    }

}
