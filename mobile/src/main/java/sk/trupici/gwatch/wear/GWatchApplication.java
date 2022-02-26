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

package sk.trupici.gwatch.wear;


import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.console.ConsoleBuffer;
import sk.trupici.gwatch.wear.console.PacketConsole;
import sk.trupici.gwatch.wear.dispatch.Dispatcher;
import sk.trupici.gwatch.wear.dispatch.WatchDispatcher;
import sk.trupici.gwatch.wear.receivers.AAPSReceiver;
import sk.trupici.gwatch.wear.receivers.AlarmReceiver;
import sk.trupici.gwatch.wear.receivers.BGReceiver;
import sk.trupici.gwatch.wear.receivers.DexComReceiver;
import sk.trupici.gwatch.wear.receivers.DiaboxReceiver;
import sk.trupici.gwatch.wear.receivers.GlimpReceiver;
import sk.trupici.gwatch.wear.receivers.LibreAlarmReceiver;
import sk.trupici.gwatch.wear.receivers.LibreLinkReceiver;
import sk.trupici.gwatch.wear.receivers.XDripReceiver;
import sk.trupici.gwatch.wear.service.NotificationService;
import sk.trupici.gwatch.wear.util.LangUtils;

public class GWatchApplication extends Application {

    public static final String LOG_TAG = "G-Watch Wear";

    public static final int DEF_RECEIVER_PRIO = 100;

    private static Context context;
    public static Context getAppContext() {
        return context;
    }

    private static final PacketConsole packetConsole = new ConsoleBuffer(new Date());
    public static PacketConsole getPacketConsole() {
        return packetConsole;
    }

    private static final WatchDispatcher dispatcher = new WatchDispatcher();
    public static Dispatcher getDispatcher() {
        return dispatcher;
    }

    private final List<BGReceiver> bgReceivers = new ArrayList<>(7);

    public static boolean isDebugEnabled() {
        if (BuildConfig.DEBUG && context != null) {
            return PreferenceUtils.isConfigured(context, "cfg_debug_enabled",  false);
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        GWatchApplication.context = LangUtils.createLangContext(getApplicationContext());
        packetConsole.init();
        dispatcher.init(context);

        // register BG receivers
        registerReceiver(new GlimpReceiver());
        registerReceiver(new XDripReceiver());
        registerReceiver(new LibreLinkReceiver());
        registerReceiver(new AAPSReceiver());
        registerReceiver(new DexComReceiver());
        registerReceiver(new LibreAlarmReceiver());
        registerReceiver(new DiaboxReceiver());

        NotificationService.startService(this);

        AlarmReceiver.scheduleNextAlarm(context, 15);
    }

    @Override
    public void onTerminate() {
        Log.e(LOG_TAG, "GWatchApplication terminated...");

        // unregister all BG receivers
        // This action is better placed in activity onDestroy() method.
        for (BGReceiver receiver : bgReceivers) {
            if (receiver != null) {
                unregisterReceiver(receiver);
            }
        }

        super.onTerminate();
    }

    /**
     * Register itself as <code>BroadcastReceiver</code>
     * @param receiver <code>BGReceiver</code> to register
     */
    private void registerReceiver(BGReceiver receiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(receiver.getAction());
        intentFilter.setPriority(DEF_RECEIVER_PRIO);
        registerReceiver(receiver, intentFilter);
        bgReceivers.add(receiver);
    }
    public void processIntent(Context context, Intent intent) {

        for (BGReceiver receiver : bgReceivers) {
            if (receiver != null) {
                if (intent.getAction().equals(receiver.getAction())) {
                    intent.setPackage(null); // clear package to pretend implicit filter
                    receiver.onReceive(context, intent);
                    break;
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
}
