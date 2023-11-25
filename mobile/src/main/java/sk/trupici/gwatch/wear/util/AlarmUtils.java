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

import static android.content.Context.ALARM_SERVICE;
import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import sk.trupici.gwatch.wear.BuildConfig;

public class AlarmUtils {

    private static final String KEY_ALARM_OFFSET = "ALARM_OFFSET";
    private static final String KEY_ALARM_ELAPSED = "ALARM_ELAPSED";
    private static final String KEY_ALARM_RTC = "ALARM_RTC";

    private static final long ACCEPTABLE_SCHEDULE_DELAY_MS = 60000;
    private static boolean useAlarmClock = false;

    public static void evaluateSchedule(@Nullable Bundle alarmBundle) {
        if (alarmBundle == null) {
            return;
        }
        long offset = alarmBundle.getLong(KEY_ALARM_OFFSET, 0L);
        long elapsed = alarmBundle.getLong(KEY_ALARM_ELAPSED, 0L);
        if (offset == 0 || elapsed == 0) {
            return;
        }

        if (elapsed - SystemClock.elapsedRealtime() > offset + ACCEPTABLE_SCHEDULE_DELAY_MS) {
            useAlarmClock = true;
        }
    }

    public static boolean scheduleAlarm(Context context, long delayMs, Intent intent, int alarmId) {
        if (intent == null) {
            Log.e(LOG_TAG, "Alarms: alarm " + alarmId + " - intent not specified!");
            return false;
        }

        try {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Alarms: Scheduling alarm " + alarmId + " after: " + delayMs + " ms");
            }
            AlarmManager alarmManager = (AlarmManager)context.getSystemService(ALARM_SERVICE);

            long elapsed = SystemClock.elapsedRealtime();
            long rtc = System.currentTimeMillis();
            intent.putExtra(KEY_ALARM_OFFSET, delayMs);
            intent.putExtra(KEY_ALARM_ELAPSED, elapsed);
//            intent.putExtra(KEY_ALARM_RTC, rtc);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT | AndroidUtils.getMutableFlag(true));

            try {
                alarmManager.cancel(pendingIntent);
            } catch (Throwable t) {
                Log.e(LOG_TAG, "Alarms: failed to cancel alarm " + alarmId, t);
            }

            if (useAlarmClock) {
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(rtc + delayMs, null), pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, elapsed + delayMs, pendingIntent);
            }
            return true;
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Alarms: failed to schedule alarm " + alarmId, t);
            return false;
        }
    }

    public static void cancelAlarm(Context context, Intent intent, int alarmId) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Alarms: Cancelling alarm " + alarmId);
        }
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT));
    }
}
