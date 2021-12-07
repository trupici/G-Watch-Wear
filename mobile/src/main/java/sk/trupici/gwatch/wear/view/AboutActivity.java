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

package sk.trupici.gwatch.wear.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.IoUtils;

public class AboutActivity extends LocalizedActivityBase  {

    private static final long CHANGE_OPTIONS_LIMIT = 3000; // 3s
    private static final long CHANGE_OPTIONS_TAP_COUNT = 5;

    long startMillis = 0;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);

        setupToolBar();

        findViewById(R.id.compatible_cgm_apps).setOnClickListener(v -> {
            long time = System.currentTimeMillis();
            if (startMillis == 0 || (time - startMillis) > CHANGE_OPTIONS_LIMIT) { // 1st tap
                startMillis = time;
                count = 1;
            } else { // next tap
                count++;
            }

            if (count == CHANGE_OPTIONS_TAP_COUNT) {
                IoUtils.vibrate(getApplicationContext());
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GWatchApplication.getAppContext());
                boolean value = prefs.getBoolean("pref_data_source_libre_visible", false);
                prefs.edit().putBoolean("pref_data_source_libre_visible", !value).apply();
                startMillis = 0;
                count = 0;
            }
        });
    }

    private void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.action_about);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // close this activity as oppose to navigating up
        return false;
    }
}
