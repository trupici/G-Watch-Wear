package sk.trupici.gwatch.wear.services;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import sk.trupici.gwatch.wear.data.PacketBase;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.DumpUtils;

import static sk.trupici.gwatch.wear.util.CommonConstants.LOG_TAG;

public class ConfigListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOG_TAG, "Received event with Message path: " + messageEvent.getPath());

        if (!messageEvent.getPath().equals("/config")) {
            super.onMessageReceived(messageEvent);
            return;
        }

        final byte[] data = messageEvent.getData();
        Log.v("myTag", "Message received:\n" + DumpUtils.dumpData(data, data.length));
        Log.d(CommonConstants.LOG_TAG, DumpUtils.dumpData(data, data.length));

        if (data.length < PacketBase.PACKET_HEADER_SIZE) {
            return;
        }

        // TODO decode config packet
    }
}
