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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

public class AppWidgetGraphFragment extends sk.trupici.gwatch.wear.settings.fragment.SettingsFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_app_widget_graph, rootKey);
        super.onCreatePreferences(savedInstanceState, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if (!TMP_FRAGMENT_TAG.equals(getTag())) {
            // show/hide patched LibreLink data source functionality
            PreferenceCategory prefCategory = (PreferenceCategory) getPreferenceScreen().findPreference("widget_graph_refresh");
            SwitchPreferenceCompat pref = (SwitchPreferenceCompat) prefCategory.findPreference("pref_widget_graph_1min_update");
            if (PreferenceUtils.isConfigured(GWatchApplication.getAppContext(), "pref_data_source_libre_visible", false)) {
                prefCategory.setVisible(true);
                pref.setVisible(true);
            } else if (pref != null) {
                pref.setChecked(false);
                pref.setVisible(false);
                prefCategory.setVisible(false);
            }
        }

        super.onViewCreated(view, savedInstanceState);
    }
}
