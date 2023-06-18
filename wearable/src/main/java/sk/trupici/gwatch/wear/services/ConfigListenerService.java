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
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.HashMap;
import java.util.Map;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.common.data.ConfigPacket;
import sk.trupici.gwatch.wear.common.data.TLV;
import sk.trupici.gwatch.wear.common.util.DumpUtils;
import sk.trupici.gwatch.wear.common.util.PacketUtils;
import sk.trupici.gwatch.wear.data.ConfigData;
import sk.trupici.gwatch.wear.data.ConfigType;
import sk.trupici.gwatch.wear.util.CommonConstants;

import static sk.trupici.gwatch.wear.data.ConfigData.TAG_GL_THRESHOLD_HIGH;
import static sk.trupici.gwatch.wear.data.ConfigData.TAG_GL_THRESHOLD_HYPER;
import static sk.trupici.gwatch.wear.data.ConfigData.TAG_GL_THRESHOLD_HYPO;
import static sk.trupici.gwatch.wear.data.ConfigData.TAG_GL_THRESHOLD_LOW;
import static sk.trupici.gwatch.wear.data.ConfigData.TAG_GL_UNIT_CONVERSION;

public class ConfigListenerService extends WearableListenerService {

    private static final String LOG_TAG = ConfigListenerService.class.getSimpleName();

    private static final String WAKE_LOCK_TAG = "gwatch.wear:" + ConfigListenerService.class.getSimpleName() + ".wake_lock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 60000; // 60s

    public final static Map<Byte, ConfigData> preferenceMap = new HashMap<Byte, ConfigData>() {{
        // glucose levels
        put(TAG_GL_THRESHOLD_HYPO, new ConfigData(TAG_GL_THRESHOLD_HYPO, ConfigType.BYTE, CommonConstants.PREF_HYPO_THRESHOLD));
        put(TAG_GL_THRESHOLD_LOW, new ConfigData(TAG_GL_THRESHOLD_LOW, ConfigType.BYTE, CommonConstants.PREF_LOW_THRESHOLD));
        put(TAG_GL_THRESHOLD_HIGH, new ConfigData(TAG_GL_THRESHOLD_HIGH, ConfigType.BYTE, CommonConstants.PREF_HIGH_THRESHOLD));
        put(TAG_GL_THRESHOLD_HYPER, new ConfigData(TAG_GL_THRESHOLD_HYPER, ConfigType.WORD, CommonConstants.PREF_HYPER_THRESHOLD));
        put(TAG_GL_UNIT_CONVERSION, new ConfigData(TAG_GL_UNIT_CONVERSION, ConfigType.BOOLEAN, CommonConstants.PREF_IS_UNIT_CONVERSION));
    }};


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOG_TAG, "Received event with Message path: " + messageEvent.getPath());

        PowerManager powerManager = (PowerManager)getApplicationContext().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        try {
            if (!messageEvent.getPath().equals("/config")) {
                super.onMessageReceived(messageEvent);
                return;
            }

            final byte[] data = messageEvent.getData();
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, DumpUtils.dumpData(data, data.length));
            }

            ConfigPacket packet = ConfigPacket.of(data);
            if (packet == null || packet.getTlvList().size() == 0) {
                Log.e(LOG_TAG, "Failed to decode packet: " + (packet == null ? "null" : packet.getTlvList().size()));
                return;
            }

            // decode and persist configuration
            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
            for (TLV tlv : packet.getTlvList()) {
                ConfigData cfg = preferenceMap.get(tlv.getTag());
                if (cfg == null) {
                    Log.e(LOG_TAG, "Unknown configuration type: " + Integer.toHexString((tlv.getTag() & 0xFF)));
                    continue;
                }
                persistConfig(tlv, cfg, edit);
            }
            edit.apply();

            // notify watchface that config has changed
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(CommonConstants.REMOTE_CONFIG_ACTION));
        } finally {
            wakeLock.release();
        }
    }

    private void persistConfig(TLV tlv, ConfigData cfg, SharedPreferences.Editor edit) {
        int intValue;
        switch (cfg.getType()) {
            case BYTE:
                intValue = (tlv.getValue()[0] & 0xFF);
                edit.putInt(cfg.getPrefName(), intValue);
                break;
            case WORD:
                intValue = PacketUtils.decodeShort(tlv.getValue(), 0);
                edit.putInt(cfg.getPrefName(), intValue);
                break;
            case DWORD:
            case COLOR:
                intValue = PacketUtils.decodeInt(tlv.getValue(), 0);
                edit.putInt(cfg.getPrefName(), intValue);
                break;
            case BOOLEAN:
                boolean boolValue = PacketUtils.decodeBoolean(tlv.getValue(), 0);
                edit.putBoolean(cfg.getPrefName(), boolValue);
                break;
            case FLOAT:
                float floatValue = PacketUtils.decodeFloat(tlv.getValue(), 0);
                edit.putFloat(cfg.getPrefName(), floatValue);
                break;
            case STRING:
                String strValue = new String(tlv.getValue());
                edit.putString(cfg.getPrefName(), strValue);
                break;
            default:
                Log.e(LOG_TAG, "persistConfig: unsupported type: " + cfg.getType());
                break;
        }
    }
}
