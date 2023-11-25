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
import sk.trupici.gwatch.wear.common.data.AAPSPacket;
import sk.trupici.gwatch.wear.common.data.ConfigPacket;
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.data.Packet;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.common.util.DumpUtils;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.service.NotificationService;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.widget.WidgetUpdateService;

public class WatchDispatcher implements Dispatcher {
    public static final String LOG_TAG = GWatchApplication.LOG_TAG;

    public void init(Context context) {
        setupWearClient(context);
    }

    @Override
    public void reconnect(Context context) {
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
            final Context context = GWatchApplication.getAppContext();

            showSendingPacket(GWatchApplication.getAppContext(), packet);

            String messagePath = getMessagePath(packet);
            if (messagePath == null) {
                Log.w(LOG_TAG, "dispatch: unsupported packet");
                return false;
            }

            Wearable.getMessageClient(GWatchApplication.getAppContext())
                    .sendMessage(nodeId, messagePath, packet.getData())
                    .addOnSuccessListener(i -> {
                        Log.d(LOG_TAG, "onSuccess: " + i);
                        showMessage(context.getString(R.string.packet_sent, StringUtils.formatTime(new Date())));
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
            Log.w(GWatchApplication.LOG_TAG, "Service not bound.");
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

        GWatchApplication.getPacketConsole().onWatchConnectionChanged(false);
        showMessage(context.getString(R.string.connecting_watch));

        // Build a new MessageClient for the Wearable API
        MessageClient messageClient = Wearable.getMessageClient(context);
        messageClient.addListener((messageEvent) -> { // TODO
            Log.d(LOG_TAG, "onMessageReceived: " + messageEvent);
        });

        new Thread() {
            @Override
            public void run() {
                nodeId = pickBestNodeId(getNodes(context));
                boolean isConnected = nodeId != null;
                showMessage(context.getString(isConnected ? R.string.status_ok : R.string.status_failed));
                GWatchApplication.getPacketConsole().onWatchConnectionChanged(isConnected);
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
//                NotificationService.updateBgData(GWatchApplication.getAppContext(), (GlucosePacket) packet);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "updateNotificationService: failed to update notification service", e);
        }
    }


    protected void showMessage(String message) {
        try {
            GWatchApplication.getPacketConsole().showText(message);
        } catch (Throwable e) {
            Log.e(GWatchApplication.LOG_TAG, e.getLocalizedMessage(), e);
        }
    }

    private void showSendingPacket(Context context, Packet packet) {
        try {
            showMessage(packet.toText(context, context.getString(R.string.sending_packet)));
        } catch (Throwable e) {
            String errMsg = e.getLocalizedMessage();
            Log.e(GWatchApplication.LOG_TAG, errMsg == null ? e.getClass().getSimpleName() : errMsg, e);
        }
    }
}
