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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import java.util.Date;

import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.common.util.DumpUtils;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.util.CommonConstants;

import static sk.trupici.gwatch.wear.common.util.CommonConstants.DAY_IN_MILLIS;
import static sk.trupici.gwatch.wear.common.util.CommonConstants.HOUR_IN_MILLIS;


public class ProviderDataUpdater extends BroadcastReceiver {
    private static final String LOG_TAG = ProviderDataUpdater.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "onReceive: ");
            DumpUtils.dumpIntent(intent);
        }

        Bundle extras = intent.getExtras();
        BgData bgData = BgData.fromBundle(extras);

        boolean invalidTimestampDiff = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastBgTimestamp = prefs.getLong(BgDataProviderService.PREF_LAST_UPDATE, 0L);
        if (lastBgTimestamp > bgData.getTimestamp()) {
            if (BuildConfig.DEBUG) {
                Log.i(LOG_TAG, "onReceive: last ts > bg ts. Back-filling?");
            }
            if (lastBgTimestamp - bgData.getTimestamp() > DAY_IN_MILLIS) {
                invalidTimestampDiff = true; // save the last timestamp, maybe the stored value is invalid
            } else { // back filling ?
                return;
            }
        }

        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(BgDataProviderService.PREF_LAST_UPDATE, bgData.getTimestamp());

        if (bgData.getTimestampDiff() < 0) {
            invalidTimestampDiff = true; // historical data ?
        }

        long timeDiff = System.currentTimeMillis() - bgData.getTimestamp();
        if (invalidTimestampDiff || timeDiff > DAY_IN_MILLIS) {
            // signal no data
            if (BuildConfig.DEBUG) {
                Log.i(LOG_TAG, "onReceive: data is too old: " + new Date(bgData.getTimestamp()));
                Log.i(LOG_TAG, "onReceive: ts diff=" + (System.currentTimeMillis() - bgData.getTimestamp()) + " vs " + HOUR_IN_MILLIS);
            }
            edit.remove(BgDataProviderService.PREF_TEXT);
            edit.remove(BgDataProviderService.PREF_TITLE);
            edit.remove(BgDataProviderService.PREF_VALUE);
        } else {
            boolean isUnitConversion = prefs.getBoolean(CommonConstants.PREF_IS_UNIT_CONVERSION, context.getResources().getBoolean(R.bool.def_bg_is_unit_conversion));
            String text = BgUtils.formatBgValueString(bgData.getValue(), bgData.getTrend(), isUnitConversion);
            // do not send time delta to complications - since the content might not be updated regularly
            String title = BgUtils.formatBgDeltaForComplication(
                    bgData.getValueDiff(),
                    timeDiff,
                    isUnitConversion,
                    prefs.getInt(CommonConstants.PREF_NO_DATA_THRESHOLD, context.getResources().getInteger(R.integer.def_bg_threshold_no_data))
            );

            if (BuildConfig.DEBUG) {
                Log.i(LOG_TAG, "onReceive: saving: text=" + text + ", title=" + title + ", value=" + bgData.getValue());
            }

            edit.putString(BgDataProviderService.PREF_TEXT, text);
            edit.putString(BgDataProviderService.PREF_TITLE, title);
            edit.putInt(BgDataProviderService.PREF_VALUE, bgData.getValue());
        }
        edit.commit();

        // Request an update for all active complications
        ComponentName provider = new ComponentName(context, BgDataProviderService.class);
        ProviderUpdateRequester requester = new ProviderUpdateRequester(context, provider);
        requester.requestUpdateAll();
    }
}
