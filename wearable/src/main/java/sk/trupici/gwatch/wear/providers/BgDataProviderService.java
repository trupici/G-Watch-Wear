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
package sk.trupici.gwatch.wear.providers;

import android.content.SharedPreferences;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

public class BgDataProviderService extends ComplicationProviderService {

    private final static String LOG_TAG = BgDataProviderService.class.getSimpleName();

    public final static String PREF_TEXT = "provider_bg_text";
    public final static String PREF_TITLE = "provider_bg_title";
    public final static String PREF_VALUE = "provider_bg_value";
    public final static String PREF_LAST_UPDATE = "provider_bg_ts";
    public final static String PREF_COMPLICATION_IDS = "provider_ids";

    @Override
    public void onComplicationActivated(int complicationId, int type, ComplicationManager manager) {
        super.onComplicationActivated(complicationId, type, manager);

        synchronized (this) {
            int[] ids = PreferenceUtils.getIntArrayValue(getApplicationContext(), PREF_COMPLICATION_IDS);
            if (ids == null) {
                ids = new int[1];
                ids[0] = complicationId;
            } else {
                Set<Integer> idSet = Arrays.stream(ids).boxed().collect(Collectors.toSet());
                idSet.add(complicationId);
                ids = idSet.stream().mapToInt(Integer::intValue).toArray();
            }
            PreferenceUtils.setIntArrayValue(getApplicationContext(), PREF_COMPLICATION_IDS, ids);
        }
    }

    @Override
    public void onComplicationDeactivated(int complicationId) {
        super.onComplicationDeactivated(complicationId);

        synchronized (this) {
            int[] ids = PreferenceUtils.getIntArrayValue(getApplicationContext(), PREF_COMPLICATION_IDS);
            if (ids == null) {
                Log.w(LOG_TAG, "provider: onComplicationDeactivated: not found: " + complicationId);
                return;
            }

            Set<Integer> idSet = Arrays.stream(ids).boxed().collect(Collectors.toSet());
            if (!idSet.remove(complicationId)) {
                Log.w(LOG_TAG, "provider: onComplicationDeactivated: not found: " + complicationId);
                return; // not found
            }
            ids = idSet.stream().mapToInt(Integer::intValue).toArray();
            PreferenceUtils.setIntArrayValue(getApplicationContext(), PREF_COMPLICATION_IDS, ids);
        }
    }

    @Override
    public void onComplicationUpdate(int complicationId, int type, ComplicationManager manager) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "onComplicationUpdate: id=" + complicationId + ", type=" + type);
        }

        if (type != ComplicationData.TYPE_SHORT_TEXT && type != ComplicationData.TYPE_LONG_TEXT && type != ComplicationData.TYPE_RANGED_VALUE) {
            manager.noUpdateRequired(complicationId);
            if (BuildConfig.DEBUG) {
                Log.w(LOG_TAG, "onComplicationUpdate: unsupported type for id=" + complicationId + ", type=" + type);
            }
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String text = prefs.getString(PREF_TEXT, null);
        String title = prefs.getString(PREF_TITLE, null);
        int value = prefs.getInt(PREF_VALUE, 0);
        long lastUpdate = prefs.getLong(PREF_LAST_UPDATE, 0);

        boolean isNoData = text == null || System.currentTimeMillis() - lastUpdate > CommonConstants.HOUR_IN_MILLIS;

        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "onComplicationUpdate: text=" + text + ", title=" + title + ", value=" + value + ", ts=" + new Date(lastUpdate));
            Log.i(LOG_TAG, "onComplicationUpdate: ts diff=" + (System.currentTimeMillis() - lastUpdate) + " vs " + CommonConstants.HOUR_IN_MILLIS);
        }

        ComplicationData data;
        if (isNoData) {
            if (BuildConfig.DEBUG) {
                Log.i(LOG_TAG, "onComplicationUpdate: no data for id=" + complicationId + ", type=" + type);
            }
            data = new ComplicationData.Builder(ComplicationData.TYPE_NO_DATA).build();
        } else if (type == ComplicationData.TYPE_SHORT_TEXT) {
            data = new ComplicationData.Builder(type)
                    .setShortText(ComplicationText.plainText(text))
                    .setShortTitle(ComplicationText.plainText(title))
                    .build();
        } else if (type == ComplicationData.TYPE_LONG_TEXT) {
            data = new ComplicationData.Builder(type)
                    .setLongText(ComplicationText.plainText(text))
                    .setLongTitle(ComplicationText.plainText(title))
                    .build();
        } else { //if (type == ComplicationData.TYPE_RANGED_VALUE) {
            int minValue = 40;
            int maxValue = 400;
            data = new ComplicationData.Builder(type)
                    .setMinValue(minValue)
                    .setMaxValue(maxValue)
                    .setValue(value)
                    .setShortText(ComplicationText.plainText(text))
                    .setShortTitle(ComplicationText.plainText(title))
                    .build();
        }
        manager.updateComplicationData(complicationId, data);
    }
}
