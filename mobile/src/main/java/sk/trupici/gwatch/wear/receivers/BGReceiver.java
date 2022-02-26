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
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.data.Packet;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

import static android.content.Context.POWER_SERVICE;

/**
 * Base class for <code>BroadcastReceiver</code> with Blood Glucose value
 */
public abstract class BGReceiver extends BroadcastReceiver {

    private static final String WAKE_LOCK_TAG = "gwatch.wear:BGReceiver.wake_lock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 60000; // 1 min

    abstract public String getPreferenceKey();
    abstract public String getSourceLabel();
    abstract public String getAction();
    abstract protected Packet processIntent(Context context, Intent intent);

    private final Executor executor = Executors.newSingleThreadExecutor(); // change according to your requirements

    @Override
    public final void onReceive(Context context, Intent intent) {
        // wait for implicit broadcast or broadcast from static BG receiver only
        if (intent.getPackage() != null) { // avoid dual processing
            return;
        }

        Log.i(GWatchApplication.LOG_TAG, getSourceLabel() + " packet received");

        final PendingResult pendingResult = goAsync();

        executor.execute(() -> {
            Context appContext = GWatchApplication.getAppContext();

            PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,WAKE_LOCK_TAG);
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

            try {
                if (!PreferenceUtils.isConfigured(GWatchApplication.getAppContext(), getPreferenceKey(), true)) {
                    if (BuildConfig.DEBUG) {
                        Log.w(GWatchApplication.LOG_TAG, getSourceLabel() + " packet not processed due to configuration");
                    }
                } else {
                    UiUtils.showMessage(appContext, appContext.getString(R.string.glucose_packet_received, getSourceLabel(), StringUtils.formatTime(new Date())));
                    Packet packet = processIntent(appContext, intent);
                    if (packet != null) {
                        GWatchApplication.getDispatcher().dispatch(packet);
                    } else {
                        UiUtils.showMessage(appContext, appContext.getString(R.string.glucose_packet_invalid));
                    }
                }
            } finally {
                wakeLock.release();
                // Must call finish() so the BroadcastReceiver can be recycled.
                if (pendingResult != null) {
                    pendingResult.finish();
                }
            }
        });
    }

    protected void dumpIntent(Intent intent) {
        String indent = "   ";
        Log.i(GWatchApplication.LOG_TAG, "\n");
        Log.i(GWatchApplication.LOG_TAG, indent + "Intent: " + intent.getAction());
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.i(GWatchApplication.LOG_TAG, indent + key + ": " + value);
            }
        } else {
            Log.i(GWatchApplication.LOG_TAG, indent + "No extras data");
        }
        Log.i(GWatchApplication.LOG_TAG, "\n");
    }
}
