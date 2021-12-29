package sk.trupici.gwatch.wear.dispatch;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.widget.WidgetUpdateService;

public class WatchDispatcher implements Dispatcher, OnSuccessListener<Integer>, OnFailureListener {
    public static final String LOG_TAG = GWatchApplication.LOG_TAG;

    public void init(Context context) {
        setupWearClient(context);
    }

    @Override
    public boolean dispatch(Packet packet) {
        Log.d(LOG_TAG, "dispatch: " + packet);
        WidgetUpdateService.updateWidget(packet);

        if (nodeId != null) {
            notifySendingPacket(packet);

            String messagePath = getMessagePath(packet);
            if (messagePath == null) {
                Log.d(LOG_TAG, "dispatch: unsupported packet");
                return false;
            }

            Task<Integer> sendTask = Wearable.getMessageClient(GWatchApplication.getAppContext())
                    .sendMessage(nodeId, messagePath, packet.getData());
            // You can add success and/or failure listeners,
            // Or you can call Tasks.await() and catch ExecutionException
            sendTask.addOnSuccessListener(this);
            sendTask.addOnFailureListener(this);
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

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.d(LOG_TAG, "onFailure: " + e.getLocalizedMessage());
    }

    @Override
    public void onSuccess(@NonNull Integer o) {
        Log.d(LOG_TAG, "onSuccess: " + o);
        notifyPacketSent(GWatchApplication.getAppContext());
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
                String status = GWatchApplication.getAppContext().getString(R.string.packet_sent, UiUtils.formatTime(new Date()));
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

}
