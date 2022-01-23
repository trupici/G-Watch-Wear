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
package sk.trupici.gwatch.wear.dispatch;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.AAPSPacket;
import sk.trupici.gwatch.wear.data.ConfigPacket;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.service.NotificationService;
import sk.trupici.gwatch.wear.util.BgUtils;
import sk.trupici.gwatch.wear.util.DumpUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.widget.WidgetUpdateService;

public class WatchDispatcher implements Dispatcher {
    public static final String LOG_TAG = GWatchApplication.LOG_TAG;

    public void init(Context context) {
        setupWearClient(context);
    }

    @Override
    public boolean dispatch(Packet packet) {
        Log.d(LOG_TAG, "dispatch: " + packet.toText(GWatchApplication.getAppContext(), null));
        WidgetUpdateService.updateWidget(packet);
        updateNotificationService(packet);

        if (BuildConfig.DEBUG) {
            byte[] data = packet.getData();
            Log.i(LOG_TAG, DumpUtils.dumpData(packet.getData(), data.length));
        }

        if (nodeId != null) {
            notifySendingPacket(packet);

            String messagePath = getMessagePath(packet);
            if (messagePath == null) {
                Log.w(LOG_TAG, "dispatch: unsupported packet");
                return false;
            }

            Wearable.getMessageClient(GWatchApplication.getAppContext())
                    .sendMessage(nodeId, messagePath, packet.getData())
                    .addOnSuccessListener(i -> {
                        Log.d(LOG_TAG, "onSuccess: " + i);
                        notifyPacketSent(GWatchApplication.getAppContext());
                        if (packet instanceof ConfigPacket) {
                            UiUtils.showToast(GWatchApplication.getAppContext(), R.string.cfg_transfer_ok);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.d(LOG_TAG, "onFailure: " + e.getLocalizedMessage());
                        if (packet instanceof ConfigPacket) {
                            UiUtils.showToast(GWatchApplication.getAppContext(), R.string.cfg_transfer_failed);
                        }
                    });
            return true;
        } else if (BuildConfig.DEBUG){
            Log.i(GWatchApplication.LOG_TAG, "Service not bound.");
        }
        return false;
    }

    private String getMessagePath(Packet packet) {
        switch (packet.getType()) {
            case GLUCOSE:
                return "/bg_data";
            case AAPS:
                return "/aaps_data";
            case CONFIG:
                return "/config";
            case SYNC:
                return "/sync";
            default:
                return null;
        }
    }

    private String nodeId = null;

    private void setupWearClient(Context context) {

        // Build a new MessageClient for the Wearable API
        MessageClient mewssageClient = Wearable.getMessageClient(context);
        mewssageClient.addListener((messageEvent) -> { // TODO
            Log.d(LOG_TAG, "onMessageReceived: " + messageEvent);
        });

        new Thread() {
            @Override
            public void run() {
                nodeId = pickBestNodeId(getNodes(context));
            }
        }.start();
    }

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

    private Set<Node> getNodes(Context context) {
        try {
            List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());
            return new HashSet<>(nodes);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }

    }

    private void notifyPacketSent(Context context) {
        try {
            if (context != null) {
                String status = GWatchApplication.getAppContext().getString(R.string.packet_sent, StringUtils.formatTime(new Date()));
                GWatchApplication.getPacketConsole().showPacketStatus(status);
            }
        } catch (Throwable e) {
            Log.e(GWatchApplication.LOG_TAG, e.getLocalizedMessage(), e);
        }
    }

    private void notifySendingPacket(Packet packet) {
        try {
            GWatchApplication.getPacketConsole().showPacket(GWatchApplication.getAppContext(), packet);
        } catch (Throwable e) {
            String errMsg = e.getLocalizedMessage();
            Log.e(GWatchApplication.LOG_TAG, errMsg == null ? e.getClass().getSimpleName() : errMsg, e);
        }
    }

    private void updateNotificationService(Packet packet) {
        try {
            if (packet instanceof AAPSPacket) {
                boolean ignoreAppsBG = PreferenceUtils.isConfigured(
                        GWatchApplication.getAppContext(),
                        "pref_data_source_aaps_ignore_bg",
                        false);
                if (!ignoreAppsBG) {
                    AAPSPacket aapsPacket = (AAPSPacket) packet;
                    packet = new GlucosePacket(
                            aapsPacket.getGlucoseValue(),
                            aapsPacket.getTimestamp(),
                            (byte) 0,
                            BgUtils.slopeArrowToTrend(aapsPacket.getSlopeArrow()),
                            aapsPacket.getSlopeArrow(),
                            aapsPacket.getSource());
                }
            }

            if (packet instanceof GlucosePacket) {
                NotificationService.updateBgData(GWatchApplication.getAppContext(), (GlucosePacket) packet);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "updateNotificationService: failed to update notification service", e);
        }
    }
}
