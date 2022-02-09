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

package sk.trupici.gwatch.wear.settings.fragment;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

public class AppWidgetActionFragment extends sk.trupici.gwatch.wear.settings.fragment.SettingsFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_app_widget_action, rootKey);
        super.onCreatePreferences(savedInstanceState, rootKey);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
        String key = twoStatePreference.getKey();
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Pref clicked: " + key + ": " + twoStatePreference.isChecked());
        }

        if ("pref_widget_launch_me".equals(key)) {
            selectRadioButton(twoStatePreference, "pref_widget_launch_glimp", "pref_widget_launch_xdrip", "pref_widget_launch_aaps", "pref_widget_launch_diabox", "pref_widget_launch_dexcom", "pref_widget_launch_dexcom_follow");
        } else if ("pref_widget_launch_glimp".equals(key)) {
            selectRadioButton(twoStatePreference, "pref_widget_launch_me", "pref_widget_launch_xdrip", "pref_widget_launch_aaps", "pref_widget_launch_diabox", "pref_widget_launch_dexcom", "pref_widget_launch_dexcom_follow");
        } else if ("pref_widget_launch_xdrip".equals(key)) {
            selectRadioButton(twoStatePreference, "pref_widget_launch_me", "pref_widget_launch_glimp", "pref_widget_launch_aaps", "pref_widget_launch_diabox", "pref_widget_launch_dexcom", "pref_widget_launch_dexcom_follow");
        } else if ("pref_widget_launch_aaps".equals(key)) {
            selectRadioButton(twoStatePreference, "pref_widget_launch_me", "pref_widget_launch_glimp", "pref_widget_launch_xdrip", "pref_widget_launch_diabox", "pref_widget_launch_dexcom", "pref_widget_launch_dexcom_follow");
        } else if ("pref_widget_launch_diabox".equals(key)) {
            selectRadioButton(twoStatePreference, "pref_widget_launch_me", "pref_widget_launch_glimp", "pref_widget_launch_xdrip", "pref_widget_launch_aaps", "pref_widget_launch_dexcom", "pref_widget_launch_dexcom_follow");
        } else if ("pref_widget_launch_dexcom".equals(key)) {
            selectRadioButton(twoStatePreference, "pref_widget_launch_me", "pref_widget_launch_glimp", "pref_widget_launch_xdrip", "pref_widget_launch_aaps", "pref_widget_launch_diabox", "pref_widget_launch_dexcom_follow");
        } else if ("pref_widget_launch_dexcom_follow".equals(key)) {
            selectRadioButton(twoStatePreference, "pref_widget_launch_me", "pref_widget_launch_glimp", "pref_widget_launch_xdrip", "pref_widget_launch_aaps", "pref_widget_launch_diabox", "pref_widget_launch_dexcom");
        }
        return false;
    }
}
