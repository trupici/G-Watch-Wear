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
