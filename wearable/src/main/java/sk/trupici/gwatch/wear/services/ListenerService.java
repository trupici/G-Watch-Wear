/*
 * Copyright (C) 2021 Juraj Antal
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

package sk.trupici.gwatch.wear.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import sk.trupici.gwatch.wear.util.DumpUtils;

import static sk.trupici.gwatch.wear.util.CommonConstants.LOG_TAG;

public class ListenerService extends WearableListenerService {

    // TODO

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOG_TAG, "Received event with Message path: " + messageEvent.getPath());

        if (messageEvent.getPath().equals("/bg_data")) {
            final byte[] data = messageEvent.getData();
            Log.v("myTag", "Message received:\n" + DumpUtils.dumpData(data, data.length));

            // Broadcast message to wearable activity for display
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("bgData", data);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        } else {
            super.onMessageReceived(messageEvent);
        }
    }
}