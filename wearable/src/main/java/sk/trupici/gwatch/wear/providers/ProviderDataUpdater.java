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
import sk.trupici.gwatch.wear.components.BgPanel;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.UiUtils;


public class ProviderDataUpdater extends BroadcastReceiver {
    private static final String LOG_TAG = ProviderDataUpdater.class.getSimpleName();

    private static final char[] TREND_SET = {' ', '⇈', '↑', '↗', '→', '↘', '↓', '⇊'}; // standard arrows

    /** Receives intents on tap and causes complication states to be toggled and updated. */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "onReceive: " + intent);
        }

        Bundle extras = intent.getExtras();
        BgData bgData = BgData.fromBundle(extras);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastBgTimestamp = prefs.getLong(BgDataProviderService.PREF_LAST_UPDATE, 0L);
        if (lastBgTimestamp > bgData.getTimestamp()) {
            if (BuildConfig.DEBUG) {
                Log.i(LOG_TAG, "onReceive: last ts > bg ts. Back-filling?");
            }
            return; // back filling ?
        }

        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(BgDataProviderService.PREF_LAST_UPDATE, bgData.getTimestamp());

        if (System.currentTimeMillis() - bgData.getTimestamp() > CommonConstants.HOUR_IN_MILLIS) {
            // signal no data
            if (BuildConfig.DEBUG) {
                Log.i(LOG_TAG, "onReceive: data is too old: " + new Date(bgData.getTimestamp()));
                Log.i(LOG_TAG, "onComplicationUpdate: ts diff=" + (System.currentTimeMillis() - bgData.getTimestamp()) + " vs " + CommonConstants.HOUR_IN_MILLIS);
            }
            edit.remove(BgDataProviderService.PREF_TEXT);
            edit.remove(BgDataProviderService.PREF_TITLE);
            edit.remove(BgDataProviderService.PREF_VALUE);
        } else {
            char trendArrow = TREND_SET[bgData.getTrend().ordinal()];

            String text;
            String title;
            boolean isUnitConversion = prefs.getBoolean(CommonConstants.PREF_IS_UNIT_CONVERSION, context.getResources().getBoolean(R.bool.def_bg_is_unit_conversion));
            if (isUnitConversion) {
                text = UiUtils.convertGlucoseToMmolLStr(bgData.getValue()) + trendArrow;
                title = bgData.getTimestampDiff() < 0 ? "" : "Δ " + UiUtils.convertGlucoseToMmolL2Str(bgData.getValueDiff());
            } else {
                text = "" + bgData.getValue() + trendArrow;
                title = bgData.getTimestampDiff() < 0 ? "" : "Δ " + bgData.getValueDiff();
            }

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
