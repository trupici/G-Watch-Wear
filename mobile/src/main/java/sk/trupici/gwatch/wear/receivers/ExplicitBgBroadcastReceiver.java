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

/**
 * BG broadcast receiver registered in manifest listening
 * to explicit BG broadcasts with specified G-Watch package name.
 * <p/>
 * Explicit broadcast is delivered to app even if app is not running or it is in sleep mode.
 * In this case the app is started or woken up to process the broadcast.
 */
public class ExplicitBgBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//        UiUtils.showMessage(context, context.getString(R.string.glucose_packet_received, "<ExplicitBgBroadcastReceiver>", UiUtils.formatTime(new Date())));

        // now the app is awake and all implicit BG receivers are registered,
        // so we can use them to process new BG data
        Log.i(GWatchApplication.LOG_TAG, "Received static intent: " + intent.getAction());
        if (intent.getPackage() != null) {
            ((GWatchApplication) context.getApplicationContext()).processIntent(context, intent);
        }
    }
}
