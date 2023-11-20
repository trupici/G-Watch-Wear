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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.followers.DexcomShareFollowerService;
import sk.trupici.gwatch.wear.followers.FollowerService;
import sk.trupici.gwatch.wear.followers.LibreLinkUpFollowerService;
import sk.trupici.gwatch.wear.followers.NightScoutFollowerService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent arg1) {
        Log.w(GWatchApplication.LOG_TAG, "Broadcast receiver notified...");
        // App should be already created and thus SAP service instantiated...

        // start Follower service if configured
        if (PreferenceUtils.isConfigured(context, NightScoutFollowerService.PREF_NS_ENABLED, false)) {
            FollowerService.startService(context, NightScoutFollowerService.class);
        } else if (PreferenceUtils.isConfigured(context, DexcomShareFollowerService.PREF_DEXCOM_ENABLED, false)) {
            FollowerService.startService(context, DexcomShareFollowerService.class);
        } else if (PreferenceUtils.isConfigured(context, LibreLinkUpFollowerService.PREF_LLU_ENABLED, false)) {
            FollowerService.startService(context, LibreLinkUpFollowerService.class);
        } else {
            AlarmReceiver.scheduleNextAlarm(context, AlarmReceiver.WAKE_UP_PERIOD);
        }
    }
}
