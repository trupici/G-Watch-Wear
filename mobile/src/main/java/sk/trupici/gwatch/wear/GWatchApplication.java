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


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import sk.trupici.gwatch.wear.console.ConsoleBuffer;
import sk.trupici.gwatch.wear.console.PacketConsole;
import sk.trupici.gwatch.wear.data.ConfigPacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.dispatch.Dispatcher;
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
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.view.MainActivity;
import sk.trupici.gwatch.wear.widget.WidgetUpdateService;

public class GWatchApplication extends Application implements Dispatcher, OnSuccessListener, OnFailureListener {

    public static final String LOG_TAG = "G-Watch Wear";

    public static final int DEF_RECEIVER_PRIO = 100;

    private static Context context;
    public static Context getAppContext() {
        return GWatchApplication.context;
    }

    private MessageClient messageClient;


    private static final PacketConsole packetConsole = new ConsoleBuffer(new Date());
    public static PacketConsole getPacketConsole() {
        return packetConsole;
    }

    private List<BGReceiver> bgReceivers = new ArrayList<>(5);

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

        // register BG receivers
        registerReceiver(new GlimpReceiver());
        registerReceiver(new XDripReceiver());
        registerReceiver(new LibreLinkReceiver());
        registerReceiver(new AAPSReceiver());
        registerReceiver(new DexComReceiver());
        registerReceiver(new LibreAlarmReceiver());
        registerReceiver(new DiaboxReceiver());

        NotificationService.startService(this);
        setupWearClient();

        AlarmReceiver.scheduleNextAlarm(context, 15);
    }

    @Override
    public void onTerminate() {
        Log.e(LOG_TAG, "GWatchApplication terminated...");

        // unregister all BG receivers
        // This action is better placed in activity onDestroy() method.
        if (bgReceivers != null) {
            for (BGReceiver receiver : bgReceivers) {
                if (receiver != null) {
                    unregisterReceiver(receiver);
                }
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
//        if (packetView != null) {
//            packetView.showText("Intent: " + intent.toString());
//        }

        if (bgReceivers != null) {
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
    }

    ///////////////////////////////////////////////////////////////////////////
    // Dispatcher implementation

    @Override
    public boolean dispatch(Packet packet) {
        Log.d(LOG_TAG, "dispatch: " + packet);
        WidgetUpdateService.updateWidget(packet);

        if (nodeId != null) {
            notifySendingPacket(packet);

            Task<Integer> sendTask = Wearable.getMessageClient(context)
                    .sendMessage(nodeId, "/bg_data", packet.getData());
            // You can add success and/or failure listeners,
            // Or you can call Tasks.await() and catch ExecutionException
            sendTask.addOnSuccessListener(this);
            sendTask.addOnFailureListener(this);

//        if (sapService != null) {
//            if (sapService.sendData(packet)) {
//                Log.i(GWatchApplication.LOG_TAG, "Data sent.");
//                return true;
//            }
        } else if (BuildConfig.DEBUG){
            Log.i(GWatchApplication.LOG_TAG, "Service not bound.");
        }
        return false;
    }

    @Override
    public boolean dispatchNow(Packet packet) {
//        if (sapService != null) {
//            if (sapService.directSend(packet)) {
//                Log.i(GWatchApplication.LOG_TAG, "Data sent.");
//                return true;
//            }
//        } else if (BuildConfig.DEBUG) {
            Log.i(GWatchApplication.LOG_TAG, "Service not bound.");
//        }
        return false;
    }

    @Override
    public boolean sync(Packet packet) {
//        if (sapService != null) {
//            if (sapService.directSendWithReconnect(packet, false)) {
//                if (BuildConfig.DEBUG) {
//                    Log.i(GWatchApplication.LOG_TAG, "Data sent.");
//                }
//                return true;
//            } else if (BuildConfig.DEBUG) {
//                Log.i(GWatchApplication.LOG_TAG, "Data not sent. Forced reconnect");
//            }
//        } else {
            Log.e(GWatchApplication.LOG_TAG, "Service not bound.");
//        }
        return false;
    }

    @Override
    public boolean repeatLastGlucosePacket() {
//        if (sapService != null) {
//            if (sapService.sendLastGlucosePacket()) {
//                if (BuildConfig.DEBUG) {
//                    Log.i(GWatchApplication.LOG_TAG, "Last glucose packet sent.");
//                }
//                return true;
//            }
//        } else {
            Log.e(GWatchApplication.LOG_TAG, "Service not bound.");
//        }
        return false;
    }

    @Override
    public boolean isConnected() {
        return true; //sapService != null && sapService.isConnected();
    }

    @Override
    public void connectionChangedCallback(boolean isConnected) {
        packetConsole.onWatchConnectionChanged(isConnected);

        if (MainActivity.getActivity() != null) {
            MainActivity.getActivity().setConectionStatus(isConnected);
        }

        // crete empty configuration packet just for widget notification
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Widget update request due to connection status change");
        }
        WidgetUpdateService.updateWidget(new ConfigPacket(Collections.emptyList(), 0));

    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.d(LOG_TAG, "onFailure: " + e.getLocalizedMessage());
    }

    @Override
    public void onSuccess(@NonNull Object o) {
        Log.d(LOG_TAG, "onSuccess: " + o);
        notifyPacketSent();
    }

    ///////////////////////////////////////////////////////////////////////////

    private static final String BG_DATA_VIEW_CAPABILITY_NAME = "bg_data_view";
    private String nodeId = null;

    private void setupWearClient() {

        // Build a new MessageClient for the Wearable API
        messageClient = Wearable.getMessageClient(context);
        messageClient.addListener((messageEvent) -> {
            Log.d(LOG_TAG, "onMessageReceived: " + messageEvent);
        });

        new Thread() {
            @Override
            public void run() {
                nodeId = pickBestNodeId(getNodes());
            }
        }.start();

//        new Runnable({
//                nodeId = pickBestNodeId(getNodes())
//            }).run();

//        CapabilityClient.OnCapabilityChangedListener capabilityListener = this::updateBgDataViewCapability;
//        Wearable.getCapabilityClient(context).addListener(capabilityListener, BG_DATA_VIEW_CAPABILITY_NAME);
    }
//    private void updateBgDataViewCapability(CapabilityInfo capabilityInfo) {
//        Set<Node> connectedNodes = capabilityInfo.getNodes();
//
//        nodeId = pickBestNodeId(connectedNodes);
//    }

    private String pickBestNodeId(Set<Node> nodes) {
        if (nodes == null) {
            return null;
        }
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private Set<Node> getNodes() {
        HashSet<Node> results = new HashSet<>();
        List<Node> nodes = null;
        try {
            nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        for (Node node : nodes) {
            results.add(node);
        }
        return results;
    }

    private void notifyPacketSent() {
        try {
            if (packetConsole != null && context != null) {
                String status = GWatchApplication.getAppContext().getString(R.string.packet_sent, UiUtils.formatTime(new Date()));
                packetConsole.showPacketStatus(status);
            }
        } catch (Throwable e) {
            Log.e(GWatchApplication.LOG_TAG, e.getLocalizedMessage(), e);
        }
    }

    private void notifySendingPacket(Packet packet) {
        try {
            if (packetConsole != null) {
                packetConsole.showPacket(GWatchApplication.getAppContext(), packet);
            }
        } catch (Throwable e) {
            String errMsg = e.getLocalizedMessage();
            Log.e(GWatchApplication.LOG_TAG, errMsg == null ? e.getClass().getSimpleName() : errMsg, e);
        }
    }

}
