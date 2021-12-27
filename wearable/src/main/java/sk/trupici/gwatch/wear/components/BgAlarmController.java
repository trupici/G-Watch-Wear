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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.io.Serializable;
import java.time.LocalTime;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

import static android.content.Context.POWER_SERVICE;

public class BgAlarmController extends BroadcastReceiver {

    private static final String LOG_TAG = CommonConstants.LOG_TAG;

    public static final String PREF_NEW_VALUE_NOTIFICATION_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "notification_new_value_enabled";

    public static final String PREF_ENABLE_ALARMS = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable";
    public static final String PREF_ENABLE_TIME_RANGE = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable_time_range";
    public static final String PREF_ENABLE_FROM = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable_from";
    public static final String PREF_ENABLE_TO = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable_to";

    public static final String PREF_ENABLE_SOUND = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable_sound";
    public static final String PREF_ENABLE_SOUND_TIME_RANGE = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable_sound_time_range";
    public static final String PREF_ENABLE_SOUND_FROM = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable_sound_from";
    public static final String PREF_ENABLE_SOUND_TO = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_enable_sound_to";

    public static final String PREF_CRITICAL_ALARMS_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_critical_enabled";
    public static final String PREF_CRITICAL_ALARMS_DURATION = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_critical_duration";
    public static final String PREF_CRITICAL_ALARMS_SNOOZE_TIME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_critical_snooze_time";
    public static final String PREF_CRITICAL_ALARMS_INTENSITY = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_critical_intensity";
    public static final String PREF_CRITICAL_ALARMS_PERIOD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_critical_period";
    public static final String PREF_CRITICAL_ALARMS_SOUND_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_critical_sound_enabled";
    public static final String PREF_CRITICAL_ALARMS_SOUND_VOLUME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_critical_sound_volume";

    public static final String PREF_HYPER_ALARM_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hyper_enabled";
    public static final String PREF_HYPER_ALARM_DURATION = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hyper_duration";
    public static final String PREF_HYPER_ALARM_SNOOZE_TIME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hyper_snooze_time";
    public static final String PREF_HYPER_ALARM_INTENSITY = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hyper_intensity";
    public static final String PREF_HYPER_ALARM_PERIOD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hyper_period";
    public static final String PREF_HYPER_ALARM_SOUND_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hyper_sound_enabled";
    public static final String PREF_HYPER_ALARM_SOUND_VOLUME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hyper_sound_volume";

    public static final String PREF_HIGH_ALARM_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_high_enabled";
    public static final String PREF_HIGH_ALARM_DURATION = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_high_duration";
    public static final String PREF_HIGH_ALARM_SNOOZE_TIME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_high_snooze_time";
    public static final String PREF_HIGH_ALARM_INTENSITY = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_high_intensity";
    public static final String PREF_HIGH_ALARM_PERIOD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_high_period";
    public static final String PREF_HIGH_ALARM_SOUND_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_high_sound_enabled";
    public static final String PREF_HIGH_ALARM_SOUND_VOLUME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_high_sound_volume";

    public static final String PREF_LOW_ALARM_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_low_enabled";
    public static final String PREF_LOW_ALARM_DURATION = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_low_duration";
    public static final String PREF_LOW_ALARM_SNOOZE_TIME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_low_snooze_time";
    public static final String PREF_LOW_ALARM_INTENSITY = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_low_intensity";
    public static final String PREF_LOW_ALARM_PERIOD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_low_period";
    public static final String PREF_LOW_ALARM_SOUND_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_low_sound_enabled";
    public static final String PREF_LOW_ALARM_SOUND_VOLUME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_low_sound_volume";

    public static final String PREF_HYPO_ALARM_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hypo_enabled";
    public static final String PREF_HYPO_ALARM_DURATION = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hypo_duration";
    public static final String PREF_HYPO_ALARM_SNOOZE_TIME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hypo_snooze_time";
    public static final String PREF_HYPO_ALARM_INTENSITY = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hypo_intensity";
    public static final String PREF_HYPO_ALARM_PERIOD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hypo_period";
    public static final String PREF_HYPO_ALARM_SOUND_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hypo_sound_enabled";
    public static final String PREF_HYPO_ALARM_SOUND_VOLUME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_hypo_sound_volume";

    public static final String PREF_NO_DATA_ALARM_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_enabled";
    public static final String PREF_NO_DATA_ALARM_DURATION = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_duration";
    public static final String PREF_NO_DATA_ALARM_SNOOZE_TIME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_snooze_time";
    public static final String PREF_NO_DATA_ALARM_INTENSITY = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_intensity";
    public static final String PREF_NO_DATA_ALARM_PERIOD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_period";
    public static final String PREF_NO_DATA_ALARM_SOUND_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_sound_enabled";
    public static final String PREF_NO_DATA_ALARM_SOUND_VOLUME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_sound_volume";

    public static final String PREF_FAST_DROP_ALARM_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_enabled";
    public static final String PREF_FAST_DROP_ALARM_DURATION = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_duration";
    public static final String PREF_FAST_DROP_ALARM_SNOOZE_TIME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_snooze_time";
    public static final String PREF_FAST_DROP_ALARM_INTENSITY = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_intensity";
    public static final String PREF_FAST_DROP_ALARM_PERIOD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_period";
    public static final String PREF_FAST_DROP_ALARM_SOUND_ENABLED = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_sound_enabled";
    public static final String PREF_FAST_DROP_ALARM_SOUND_VOLUME = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_sound_volume";
    public static final String PREF_FAST_DROP_ALARM_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_fast_drop_threshold";

    public static final String PREF_LAST_SNOOZED_AT = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_last_snoozed_at";
    public static final String PREF_LAST_TRIGGERED_AT = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_last_triggered_at";
    public static final String PREF_LAST_ALARM_TYPE = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_last_alarm_type";

    public static final String PREF_NO_DATA_LAST_SNOOZED_AT = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_last_snoozed_at";
    public static final String PREF_NO_DATA_LAST_TRIGGERED_AT = AnalogWatchfaceConfig.PREF_PREFIX + "alarm_no_data_last_triggered_at";

    private static final String WAKE_LOCK_TAG = "gwatch.wear:" + BgAlarmController.class.getSimpleName() + ".wake_lock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 60000; // 60s

    private static final int MAX_ALARMS_SAMPLE_TIME = 600000; // [ms], 10 minutes

    enum Type {
        UNKNOWN,
        CRITICAL,
        HYPER,
        HIGH,
        LOW,
        HYPO,
        NO_DATA,
        FAST_DROP
    }

    final private AlarmBasicConfig alarms;
    final private AlarmBasicConfig sounds;

    final private AlarmConfig hyperConfig;
    final private AlarmConfig highConfig;
    final private AlarmConfig lowConfig;
    final private AlarmConfig hypoConfig;
    final private AlarmConfig fastDropConfig;
    final private AlarmConfig noDataConfig;
    final private AlarmConfig criticalConfig;

    private int defaultDuration;
    private int defaultSnoozeTime;
    private int defaultPeriod;
    private int defaultIntensity;
    private boolean defaultSoundEnabled;
    private int defaultVolume;

    private int criticalLowThreshold;
    private int criticalHighThreshold;

    private boolean newBgValueNotificationEnabled;
    private VibrationEffect newBgNotificationEffect;

    private int hyperThreshold;
    private int highThreshold;
    private int lowThreshold;
    private int hypoThreshold;
    private int noDataThreshold;
    private boolean isUnitConv;

    private int warnColor;
    private int urgentColor;
    private int noDataColor;

    // FIXME
    static boolean isAlarmActive = false;

    public BgAlarmController() {
        alarms = new AlarmBasicConfig();
        sounds = new AlarmBasicConfig();

        hyperConfig = new AlarmConfig(Type.HYPER);
        highConfig = new AlarmConfig(Type.HIGH);
        lowConfig = new AlarmConfig(Type.LOW);
        hypoConfig = new AlarmConfig(Type.HYPO);
        fastDropConfig = new AlarmConfig(Type.FAST_DROP);
        noDataConfig = new AlarmConfig(Type.NO_DATA);
        criticalConfig = new AlarmConfig(Type.CRITICAL);
    }

    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        Resources res = context.getResources();
        defaultDuration = res.getInteger(R.integer.def_alarms_duration);
        defaultSnoozeTime = res.getInteger(R.integer.def_alarms_snooze_time);
        defaultPeriod = res.getInteger(R.integer.def_alarms_period);
        defaultIntensity = res.getInteger(R.integer.def_alarms_intensity);
        defaultSoundEnabled = res.getBoolean(R.bool.def_alarms_sound_enabled);
        defaultVolume = res.getInteger(R.integer.def_alarms_sound_volume);

        hyperConfig.vibrationResId = R.array.alarms_pattern_hyper;
        hyperConfig.soundResId = R.raw.alarm_high;

        highConfig.vibrationResId = R.array.alarms_pattern_high;
        highConfig.soundResId = R.raw.alarm_high;

        lowConfig.vibrationResId = R.array.alarms_pattern_low;
        lowConfig.soundResId = R.raw.alarm_low;

        hypoConfig.vibrationResId = R.array.alarms_pattern_hypo;
        hypoConfig.soundResId = R.raw.alarm_low;

        fastDropConfig.vibrationResId = R.array.alarms_pattern_fast_drop;
        fastDropConfig.soundResId = R.raw.alarm_fastdrop;

        noDataConfig.vibrationResId = R.array.alarms_pattern_no_data;
        noDataConfig.soundResId = R.raw.alarm_nodata;

        criticalConfig.vibrationResId = R.array.alarms_pattern_critical;
        criticalConfig.soundResId = R.raw.alarm_critical;

        criticalLowThreshold = res.getInteger(R.integer.def_alarms_critical_threshold_low);
        criticalHighThreshold = res.getInteger(R.integer.def_alarms_critical_threshold_high);

        warnColor = res.getColor(R.color.alarms_warn_color, null);
        urgentColor = res.getColor(R.color.alarms_urgent_color, null);
        noDataColor = res.getColor(R.color.alarms_no_data_color, null);

        newBgNotificationEffect = VibrationEffect.createOneShot(
                res.getInteger(R.integer.def_notification_new_value_duration),
                res.getInteger(R.integer.def_notification_new_value_amplitude)
        );

        onConfigChanged(context, sharedPrefs);
    }

    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {
        Resources res = context.getResources();

        // thresholds
        // FIXME
        hyperThreshold = sharedPrefs.getInt(BgPanel.PREF_HYPER_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hyper));
        highThreshold = sharedPrefs.getInt(BgPanel.PREF_HIGH_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_high));
        lowThreshold = sharedPrefs.getInt(BgPanel.PREF_LOW_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_low));
        hypoThreshold = sharedPrefs.getInt(BgPanel.PREF_HYPO_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_hypo));
        noDataThreshold = sharedPrefs.getInt(BgPanel.PREF_NO_DATA_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_no_data));
        isUnitConv = sharedPrefs.getBoolean(BgPanel.PREF_IS_UNIT_CONVERSION, context.getResources().getBoolean(R.bool.def_bg_is_unit_conversion));;

        newBgValueNotificationEnabled = sharedPrefs.getBoolean(PREF_NEW_VALUE_NOTIFICATION_ENABLED, res.getBoolean(R.bool.def_notification_new_value_enabled));

        alarms.enabled = sharedPrefs.getBoolean(PREF_ENABLE_ALARMS, res.getBoolean(R.bool.def_alarms_enabled));
        alarms.timed = sharedPrefs.getBoolean(PREF_ENABLE_TIME_RANGE, res.getBoolean(R.bool.def_alarms_time_range_enabled));
        alarms.from = LocalTime.ofSecondOfDay(sharedPrefs.getInt(PREF_ENABLE_FROM, res.getInteger(R.integer.def_alarms_enabled_from)) * 60L);
        alarms.to = LocalTime.ofSecondOfDay(sharedPrefs.getInt(PREF_ENABLE_TO, res.getInteger(R.integer.def_alarms_enabled_to)) * 60L);

        sounds.enabled = sharedPrefs.getBoolean(PREF_ENABLE_SOUND, res.getBoolean(R.bool.def_alarms_sound_enabled));
        sounds.timed = sharedPrefs.getBoolean(PREF_ENABLE_SOUND_TIME_RANGE, res.getBoolean(R.bool.def_alarms_sound_time_range_enabled));
        sounds.from = LocalTime.ofSecondOfDay(sharedPrefs.getInt(PREF_ENABLE_SOUND_FROM, res.getInteger(R.integer.def_alarms_sound_enabled_from)) * 60L);
        sounds.to = LocalTime.ofSecondOfDay(sharedPrefs.getInt(PREF_ENABLE_SOUND_TO, res.getInteger(R.integer.def_alarms_sound_enabled_to)) * 60L);

        criticalConfig.enabled = sharedPrefs.getBoolean(PREF_CRITICAL_ALARMS_ENABLED, res.getBoolean(R.bool.def_alarms_critical_enabled));
        criticalConfig.duration = sharedPrefs.getInt(PREF_CRITICAL_ALARMS_DURATION, res.getInteger(R.integer.def_alarms_critical_duration));
        criticalConfig.snoozeTime = sharedPrefs.getInt(PREF_CRITICAL_ALARMS_SNOOZE_TIME, defaultSnoozeTime);
        criticalConfig.period = sharedPrefs.getInt(PREF_CRITICAL_ALARMS_PERIOD, res.getInteger(R.integer.def_alarms_critical_period));
        criticalConfig.intensity = sharedPrefs.getInt(PREF_CRITICAL_ALARMS_INTENSITY, defaultIntensity);
        criticalConfig.soundEnabled = sharedPrefs.getBoolean(PREF_CRITICAL_ALARMS_SOUND_ENABLED, defaultSoundEnabled);
        criticalConfig.volume = sharedPrefs.getInt(PREF_CRITICAL_ALARMS_SOUND_VOLUME, defaultVolume) / 100f;

        hyperConfig.enabled = sharedPrefs.getBoolean(PREF_HYPER_ALARM_ENABLED, res.getBoolean(R.bool.def_alarms_hyper_enabled));
        hyperConfig.duration = sharedPrefs.getInt(PREF_HYPER_ALARM_DURATION, defaultDuration);
        hyperConfig.snoozeTime = sharedPrefs.getInt(PREF_HYPER_ALARM_SNOOZE_TIME, defaultSnoozeTime);
        hyperConfig.period = sharedPrefs.getInt(PREF_HYPER_ALARM_PERIOD, defaultPeriod);
        hyperConfig.intensity = sharedPrefs.getInt(PREF_HYPER_ALARM_INTENSITY, defaultIntensity);
        hyperConfig.soundEnabled = sharedPrefs.getBoolean(PREF_HYPER_ALARM_SOUND_ENABLED, defaultSoundEnabled);
        hyperConfig.volume = sharedPrefs.getInt(PREF_HYPER_ALARM_SOUND_VOLUME, defaultVolume) / 100f;

        highConfig.enabled = sharedPrefs.getBoolean(PREF_HIGH_ALARM_ENABLED, res.getBoolean(R.bool.def_alarms_hyper_enabled));
        highConfig.duration = sharedPrefs.getInt(PREF_HIGH_ALARM_DURATION, defaultDuration);
        highConfig.snoozeTime = sharedPrefs.getInt(PREF_HIGH_ALARM_SNOOZE_TIME, defaultSnoozeTime);
        highConfig.period = sharedPrefs.getInt(PREF_HIGH_ALARM_PERIOD, defaultPeriod);
        highConfig.intensity = sharedPrefs.getInt(PREF_HIGH_ALARM_INTENSITY, defaultIntensity);
        highConfig.soundEnabled = sharedPrefs.getBoolean(PREF_HIGH_ALARM_SOUND_ENABLED, defaultSoundEnabled);
        highConfig.volume = sharedPrefs.getInt(PREF_HIGH_ALARM_SOUND_VOLUME, defaultVolume) / 100f;

        lowConfig.enabled = sharedPrefs.getBoolean(PREF_LOW_ALARM_ENABLED, res.getBoolean(R.bool.def_alarms_hyper_enabled));
        lowConfig.duration = sharedPrefs.getInt(PREF_LOW_ALARM_DURATION, defaultDuration);
        lowConfig.snoozeTime = sharedPrefs.getInt(PREF_LOW_ALARM_SNOOZE_TIME, defaultSnoozeTime);
        lowConfig.period = sharedPrefs.getInt(PREF_LOW_ALARM_PERIOD, defaultPeriod);
        lowConfig.intensity = sharedPrefs.getInt(PREF_LOW_ALARM_INTENSITY, defaultIntensity);
        lowConfig.soundEnabled = sharedPrefs.getBoolean(PREF_LOW_ALARM_SOUND_ENABLED, defaultSoundEnabled);
        lowConfig.volume = sharedPrefs.getInt(PREF_LOW_ALARM_SOUND_VOLUME, defaultVolume) / 100f;

        hypoConfig.enabled = sharedPrefs.getBoolean(PREF_HYPO_ALARM_ENABLED, res.getBoolean(R.bool.def_alarms_hyper_enabled));
        hypoConfig.duration = sharedPrefs.getInt(PREF_HYPO_ALARM_DURATION, defaultDuration);
        hypoConfig.snoozeTime = sharedPrefs.getInt(PREF_HYPO_ALARM_SNOOZE_TIME, defaultSnoozeTime);
        hypoConfig.period = sharedPrefs.getInt(PREF_HYPO_ALARM_PERIOD, defaultPeriod);
        hypoConfig.intensity = sharedPrefs.getInt(PREF_HYPO_ALARM_INTENSITY, defaultIntensity);
        hypoConfig.soundEnabled = sharedPrefs.getBoolean(PREF_HYPO_ALARM_SOUND_ENABLED, defaultSoundEnabled);
        hypoConfig.volume = sharedPrefs.getInt(PREF_HYPO_ALARM_SOUND_VOLUME, defaultVolume) / 100f;

        noDataConfig.enabled = sharedPrefs.getBoolean(PREF_NO_DATA_ALARM_ENABLED, res.getBoolean(R.bool.def_alarms_hyper_enabled));
        noDataConfig.duration = sharedPrefs.getInt(PREF_NO_DATA_ALARM_DURATION, res.getInteger(R.integer.def_alarms_no_data_duration));
        noDataConfig.snoozeTime = sharedPrefs.getInt(PREF_NO_DATA_ALARM_SNOOZE_TIME, defaultSnoozeTime);
        noDataConfig.period = sharedPrefs.getInt(PREF_NO_DATA_ALARM_PERIOD, res.getInteger(R.integer.def_alarms_no_data_period));
        noDataConfig.intensity = sharedPrefs.getInt(PREF_NO_DATA_ALARM_INTENSITY, defaultIntensity);
        noDataConfig.soundEnabled = sharedPrefs.getBoolean(PREF_NO_DATA_ALARM_SOUND_ENABLED, defaultSoundEnabled);
        noDataConfig.volume = sharedPrefs.getInt(PREF_NO_DATA_ALARM_SOUND_VOLUME, defaultVolume) / 100f;

        fastDropConfig.enabled = sharedPrefs.getBoolean(PREF_FAST_DROP_ALARM_ENABLED, res.getBoolean(R.bool.def_alarms_hyper_enabled));
        fastDropConfig.duration = sharedPrefs.getInt(PREF_FAST_DROP_ALARM_DURATION, res.getInteger(R.integer.def_alarms_fast_drop_duration));
        fastDropConfig.snoozeTime = sharedPrefs.getInt(PREF_FAST_DROP_ALARM_SNOOZE_TIME, res.getInteger(R.integer.def_alarms_fast_drop_snooze_time));
        fastDropConfig.period = sharedPrefs.getInt(PREF_FAST_DROP_ALARM_PERIOD, defaultPeriod);
        fastDropConfig.intensity = sharedPrefs.getInt(PREF_FAST_DROP_ALARM_INTENSITY, defaultIntensity);
        fastDropConfig.soundEnabled = sharedPrefs.getBoolean(PREF_FAST_DROP_ALARM_SOUND_ENABLED, defaultSoundEnabled);
        fastDropConfig.volume = sharedPrefs.getInt(PREF_FAST_DROP_ALARM_SOUND_VOLUME, defaultVolume) / 100f;
        fastDropConfig.threshold = sharedPrefs.getInt(PREF_FAST_DROP_ALARM_THRESHOLD, res.getInteger(R.integer.def_alarms_fast_drop_threshold));
    }

    private VibrationEffect createVibrationEffect(Resources res, int resId, int intensity) {
        if (intensity <= 0) {
            intensity = VibrationEffect.DEFAULT_AMPLITUDE;
        }

        int[] pattern = res.getIntArray(resId);
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

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager powerManager = (PowerManager)context.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        try {
            String action = intent.getAction();
            Log.i(LOG_TAG, "alarms: " + action);

            Bundle extras = intent.getExtras();
            BgData bgData = BgData.fromBundle(extras);

            if (bgData.getValue() == 0 || bgData.getTimestamp() <= 0) {
                Log.w(LOG_TAG, "alarms: invalid bg data received, ignored...");
                return;
            }

            long now = System.currentTimeMillis();
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if (newBgValueNotificationEnabled && bgData.getTimestampDiff() > 0) {
                vibrator.vibrate(newBgNotificationEffect);
            }

            boolean isFastDrop = false;
            if (fastDropConfig.enabled) {
                // fastdrop condition:
                // - previous glucose value must be valid (last_glucose still contains the previous value here)
                // - delta must be negative (drop)
                // - absolute difference from the previous value must be greater or equal than the threshold
                isFastDrop = bgData.getValueDiff() > 0 && bgData.getValueDiff() <= -fastDropConfig.threshold;
            }

            if (!alarms.enabled) {
                return;
            }

            if (!isFastDrop && lowThreshold < bgData.getValue() && bgData.getValue() < highThreshold) {
                Log.d(LOG_TAG, "alarms: glucose in range");
                PreferenceUtils.setLongValue(context, PREF_LAST_TRIGGERED_AT, 0L);
                return;
            }

            if (!isAlarmTime(bgData.getTimestamp())) {
                PreferenceUtils.setLongValue(context, PREF_LAST_TRIGGERED_AT, 0L);
                return;
            }


            if (isAlarmActive) {
                Log.d(LOG_TAG, "alarms: another alarm is already running");
                return;
            }

            if (isFastDrop) {
                triggerAlarm(context, fastDropConfig, bgData.getValueDiff(), isSoundAlarmTime(now));
                return;
            }


            boolean isCritical = false;
            boolean snoozeOption = true;
            boolean isLow = false;

            AlarmConfig alarmConfig;


            if (criticalHighThreshold <= bgData.getValue()) {
                isCritical = true;
                alarmConfig = criticalConfig;
            } else if (hyperThreshold <= bgData.getValue()) {
                alarmConfig = hyperConfig;
            } else if (highThreshold <= bgData.getValue()) {
                alarmConfig = highConfig;
            } else if (bgData.getValue() <= criticalLowThreshold) {
                isLow = true;
                isCritical = true;
                alarmConfig = criticalConfig;
            } else if (bgData.getValue() <= hypoThreshold) {
                isLow = true;
                alarmConfig = hypoConfig;
            } else if (bgData.getValue() <= lowThreshold) {
                isLow = true;
                alarmConfig = lowConfig;
            } else {
                Log.d(LOG_TAG, "alarms: alarm for this value is not configured: " + bgData.getValue());
                return;
            }

            if (!alarmConfig.enabled) {
                return;
            }

            if (!isCritical) {
                long snoozedAt = PreferenceUtils.getLongValue(context, PREF_LAST_SNOOZED_AT, 0L);
                long snoozeTime = now - snoozedAt;
                if (snoozeTime < alarmConfig.snoozeTime * CommonConstants.MINUTE_IN_MILLIS) {
                    Log.d(LOG_TAG, "alarms: alarms are snoozed for next seconds: " + ((long) alarmConfig.snoozeTime * CommonConstants.MINUTE_IN_SECONDS - snoozeTime/CommonConstants.SECOND_IN_MILLIS));
                    return;
                }
            }

            long lastTriggeredAt = PreferenceUtils.getLongValue(context, PREF_LAST_TRIGGERED_AT, 0L);
            String lastAlarmTypeName = PreferenceUtils.getStringValue(context, PREF_LAST_ALARM_TYPE, Type.UNKNOWN.name());
            Type lastAlarmType = Type.valueOf(lastAlarmTypeName);

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "alarms: last alarm " + lastAlarmType.name() + " triggered at: " + LocalTime.ofSecondOfDay(lastTriggeredAt/CommonConstants.SECOND_IN_MILLIS));
            }

            // do not trigger alarm if configured period has not elapsed yet
            if (lastAlarmType == alarmConfig.type) {
                if (lastTriggeredAt != 0) {
                    long elapsed = now - lastTriggeredAt;
                    if (elapsed < alarmConfig.period) {
                        Log.d(LOG_TAG, "alarms: " + alarmConfig.type.name() + " alarm period has not elapsed yet: " + elapsed);
                    }
                }
            } else {
                PreferenceUtils.setStringValue(context, PREF_LAST_ALARM_TYPE, alarmConfig.type.name());
            }

            triggerAlarm(context, alarmConfig, bgData.getValue(), isSoundAlarmTime(now));
        } finally {
            wakeLock.release();
        }
    }

    public void handleAlarmTime(Context context) {
        if (isAlarmActive) {
            Log.w(LOG_TAG, "alarms: another alarm is already running");
            return;
        }

        PowerManager powerManager = (PowerManager)context.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        try {
            long now = System.currentTimeMillis();
            if (!noDataConfig.enabled || !isAlarmTime(now)) {
                PreferenceUtils.setLongValue(context, PREF_NO_DATA_LAST_TRIGGERED_AT, 0L);
                return;
            }

            // do not trigger alarm if previous alarm was snoozed and snooze time has not elapsed yet
            long snoozedAt = PreferenceUtils.getLongValue(context, PREF_NO_DATA_LAST_SNOOZED_AT, 0L);
            long snoozeTime = now - snoozedAt;
            if (snoozeTime < noDataConfig.snoozeTime * CommonConstants.MINUTE_IN_MILLIS) {
                Log.d(LOG_TAG, "alarms: no data alarm is snoozed for next seconds: " + ((long) noDataConfig.snoozeTime * CommonConstants.MINUTE_IN_SECONDS - snoozeTime / CommonConstants.SECOND_IN_MILLIS));
                return;
            }

            long lastTriggeredAt = PreferenceUtils.getLongValue(context, PREF_NO_DATA_LAST_TRIGGERED_AT, 0L);
            String lastAlarmTypeName = PreferenceUtils.getStringValue(context, PREF_LAST_ALARM_TYPE, Type.UNKNOWN.name());

            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "alarms: last alarm NO_DATA triggered at: " + LocalTime.ofSecondOfDay(lastTriggeredAt / CommonConstants.SECOND_IN_MILLIS));
            }

            // do not trigger alarm if configured period has not elapsed yet
            if (lastTriggeredAt != 0) {
                long elapsed = now - lastTriggeredAt;
                if (elapsed < noDataConfig.period) {
                    Log.d(LOG_TAG, "alarms: NO_DATA alarm period has not elapsed yet: " + elapsed);
                }
            }

            triggerAlarm(context, noDataConfig, 0, isSoundAlarmTime(now));
        } finally {
            wakeLock.release();
        }
    }

    private boolean isAlarmTime(long timestamp) {
        Log.d(LOG_TAG, "isAlarmTime: " + timestamp);

        if (!alarms.timed) {
            if (alarms.enabled) {
                Log.d(LOG_TAG, "isAlarmTime: alarms always active");
                return true;
            } else {
                Log.d(LOG_TAG, "isAlarmTime: alarms off");
                return false;
            }
        }

        // do not fire alarms for samples older than DEF_MAX_ALARM_SAMPLE_TIME
        if (timestamp + MAX_ALARMS_SAMPLE_TIME < System.currentTimeMillis()) {
            Log.d(LOG_TAG, "isAlarmTime: too old sample");
            return false;
        }

        return isTimeInAlarmRange(alarms.from, alarms.to);
    }

    private boolean isSoundAlarmTime(long timestamp) {
        Log.d(LOG_TAG, "isSoundAlarmTime: " + timestamp);

        if (sounds.timed) {
            return isTimeInAlarmRange(sounds.from, sounds.to);
        } else {
            if (sounds.enabled) {
                Log.d(LOG_TAG, "isSoundAlarmTime: sound always active");
                return true;
            } else {
                Log.d(LOG_TAG, "isSoundAlarmTime: sounds off");
                return false;
            }
        }
    }

    private boolean isTimeInAlarmRange(LocalTime from, LocalTime to) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "isAlarmTime: " + from + " - " + to);
        }

        LocalTime now = LocalTime.now();

        boolean result;
        if (from.isAfter(to)) { // time crosses midnight
            result = !from.isAfter(now) || now.isBefore(to);
        } else { // time in one day
            result = !from.isAfter(now) && now.isBefore(to);
        }

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "isAlarmTime: " + (result ? "" : "NOT ") + "in active time");
            Log.d(LOG_TAG, "isAlarmTime: from=" + from + ", to=" + to + ", time=" + now);
        }
        return result;
    }

    private void triggerAlarm(Context context, AlarmConfig alarmConfig, int bgValue, boolean playSound) {
        Intent intent = new Intent(BgAlarmActivity.ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Bundle extras = new Bundle();
        extras.putSerializable(BgAlarmActivity.EXTRAS_ALARM_CONFIG, alarmConfig);
        extras.putString(BgAlarmActivity.EXTRAS_ALARM_TEXT, getAlarmText(context, alarmConfig));
        if (playSound) {
            extras.putSerializable(BgAlarmActivity.EXTRAS_SOUNDS_CONFIG, sounds);
        }
        if (alarmConfig.type != Type.NO_DATA) {
            extras.putString(BgAlarmActivity.EXTRAS_BG_VALUE, isUnitConv ? UiUtils.convertGlucoseToMmolLStr(bgValue) : String.valueOf(bgValue));
        }
        extras.putInt(BgAlarmActivity.EXTRAS_ALARM_TEXT_COLOR, getAlarmTextColor(alarmConfig));
        intent.putExtras(extras);

        context.startActivity(intent);
    }

    private int getAlarmTextColor(AlarmConfig alarmConfig) {
        switch (alarmConfig.type) {
            case HIGH:
            case LOW:
                return warnColor;
            case NO_DATA:
                return noDataColor;
            default:
                return urgentColor;
        }
    }

    private String getAlarmText(Context context, AlarmConfig alarmConfig) {
        switch (alarmConfig.type) {
            case HIGH:
            case HYPER:
                return context.getString(R.string.alarm_high_value);
            case LOW:
            case HYPO:
                return context.getString(R.string.alarm_low_value);
            case CRITICAL:
                return context.getString(R.string.alarm_critical_value);
            case NO_DATA:
                return context.getString(R.string.alarm_no_data);
            case FAST_DROP:
                return context.getString(R.string.alarm_fast_drop);
            default:
                return context.getString(R.string.invalid_value);
        }
    }

    static class AlarmBasicConfig implements Serializable {
        boolean enabled;
        boolean timed;
        LocalTime from;
        LocalTime to;
    }

    static class AlarmConfig implements Serializable {
        final Type type;
        boolean enabled;
        int snoozeTime;
        int period;

        int duration;
        int intensity;
        int vibrationResId;

        boolean soundEnabled;
        int soundResId;
        float volume;

        int threshold;

        AlarmConfig(Type type) {
            this.type = type;
        }
    }


//    public void test(Context context) {
//        AlarmConfig alarmConfig = highConfig;
//        int bgValue = 180;
//
//        triggerAlarm(context, alarmConfig, bgValue, isSoundAlarmTime(System.currentTimeMillis()));
//    }
}
