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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.AidexUtils;
import sk.trupici.gwatch.wear.util.DexcomUtils;
import sk.trupici.gwatch.wear.view.SettingsActivity;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

public class DataSourceFragment extends sk.trupici.gwatch.wear.settings.fragment.SettingsFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_data_source, rootKey);
        super.onCreatePreferences(savedInstanceState, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if (!TMP_FRAGMENT_TAG.equals(getTag())) {
            // show/hide patched LibreLink data source functionality
            CheckBoxPreference pref = (CheckBoxPreference) getPreferenceScreen().findPreference("pref_data_source_libre");
            CheckBoxPreference pref2 = (CheckBoxPreference) getPreferenceScreen().findPreference("pref_data_source_libre_send_to_aaps");
            if (PreferenceUtils.isConfigured(GWatchApplication.getAppContext(), "pref_data_source_libre_visible", false)) {
                pref.setVisible(true);
                pref2.setVisible(true);
            } else if (pref != null) {
                pref.setChecked(false);
                pref.setVisible(false);
                pref2.setChecked(false);
                pref2.setVisible(false);
            }
        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
        String key = twoStatePreference.getKey();
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Pref clicked: " + key + ": " + twoStatePreference.isChecked());
        }

        if ("pref_data_source_nightscout_enable".equals(key)) {
            if (twoStatePreference.isChecked()) {
                checkPreference("pref_data_source_dexcom_share_enable", false);
                checkPreference("pref_data_source_librelinkup_enable", false);
            }
        } else if ("pref_data_source_dexcom_share_enable".equals(key)) {
            if (twoStatePreference.isChecked()) {
                checkPreference("pref_data_source_nightscout_enable", false);
                checkPreference("pref_data_source_librelinkup_enable", false);
            }
        } else if ("pref_data_source_librelinkup_enable".equals(key)) {
            if (twoStatePreference.isChecked()) {
                checkPreference("pref_data_source_nightscout_enable", false);
                checkPreference("pref_data_source_dexcom_share_enable", false);
            }
        } else if ("pref_data_source_dexcom".equals(key)) {
            if (twoStatePreference.isChecked()) {
                DexcomUtils.checkAndRequestDexcomPermission((SettingsActivity)getActivity(), SettingsActivity.REQUEST_CODE_DEXCOM_PERMISSION);
            }
        } else if ("pref_data_source_aidex".equals(key)) {
            if (twoStatePreference.isChecked()) {
                AidexUtils.checkAndRequestAppPermission((SettingsActivity)getActivity(), SettingsActivity.REQUEST_CODE_AIDEX_PERMISSION);
            }
        }
        return false;
    }
}
