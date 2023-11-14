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

package sk.trupici.gwatch.wear.receivers;

import static android.content.Context.POWER_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.followers.DexcomShareFollowerService;
import sk.trupici.gwatch.wear.followers.FollowerService;
import sk.trupici.gwatch.wear.followers.LibreLinkUpFollowerService;
import sk.trupici.gwatch.wear.followers.NightScoutFollowerService;
import sk.trupici.gwatch.wear.util.AlarmUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

public class AlarmReceiver extends BroadcastReceiver {

    public static final int WAKE_UP_CODE = 1980;
    public static final int WAKE_UP_PERIOD = (5 * 60 * 1000); // 5 min in ms

    private static final String WAKE_LOCK_TAG = "gwatch.wear:" + AlarmReceiver.class.getSimpleName() + ".wake_lock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 30000; // 30s

    @Override
    public void onReceive(Context context, Intent intent) {
        long processingTime = System.currentTimeMillis();
        PowerManager powerManager = (PowerManager)context.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

        try {
            if (BuildConfig.DEBUG) {
                Log.i(GWatchApplication.LOG_TAG, "Alarm received: " + intent.toString());
            }

            if (PreferenceUtils.isConfigured(context, NightScoutFollowerService.PREF_NS_ENABLED, false)) {
                FollowerService.requestNewValue(context, intent, NightScoutFollowerService.class, processingTime);
            } else if (PreferenceUtils.isConfigured(context, DexcomShareFollowerService.PREF_DEXCOM_ENABLED, false)) {
                FollowerService.requestNewValue(context, intent, DexcomShareFollowerService.class, processingTime);
            } else if (PreferenceUtils.isConfigured(context, LibreLinkUpFollowerService.PREF_LLU_ENABLED, false)) {
                FollowerService.requestNewValue(context, intent, LibreLinkUpFollowerService.class, processingTime);
            } else {
                if (GWatchApplication.isDebugEnabled()) {
                    UiUtils.showMessage(context, context.getString(R.string.wakeup_received));
                }
                scheduleNextAlarm(context, WAKE_UP_PERIOD);
            }
        } finally {
            wakeLock.release();
        }
    }

    public static void scheduleNextAlarm(Context context, long delayMs) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        AlarmUtils.scheduleAlarm(context, delayMs, intent, WAKE_UP_CODE);
        Log.d(GWatchApplication.LOG_TAG, "Scheduled alarm: code=" + WAKE_UP_CODE + ", delay=" + delayMs);
    }
}
