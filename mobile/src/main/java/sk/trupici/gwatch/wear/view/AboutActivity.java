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
import android.text.method.LinkMovementMethod;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.IoUtils;

public class AboutActivity extends AppCompatActivity {

    private static final long CHANGE_OPTIONS_LIMIT = 3000; // 3s
    private static final long CHANGE_OPTIONS_TAP_COUNT = 5;

    long startMillis = 0;
    int count = 0;

    ImageButton licenseButton;
    LinearLayout licenseExpandable;
    CardView licenseCardView;

    ImageButton disclaimerButton;
    LinearLayout disclaimerExpandable;
    CardView disclaimerCardView;

    ImageButton appsButton;
    LinearLayout appsExpandable;
    CardView appsCardView;

    ImageButton resourcesButton;
    LinearLayout resourcesExpandable;
    CardView resourcesCardView;

    ImageButton libsButton;
    LinearLayout libsExpandable;
    CardView libsCardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);

        setupToolBar();

        ((TextView)findViewById(R.id.designer)).setMovementMethod(LinkMovementMethod.getInstance());

        licenseCardView = findViewById(R.id.lic_cardview);
        licenseExpandable = findViewById(R.id.lic_expandable);
        licenseButton = findViewById(R.id.lic_button);
        licenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (licenseExpandable.getVisibility() == View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(licenseCardView, new AutoTransition());
                    licenseExpandable.setVisibility(View.GONE);
                    licenseButton.setImageResource(R.drawable.ic_baseline_expand_more_24);
                } else {
                    TransitionManager.beginDelayedTransition(licenseCardView, new AutoTransition());
                    licenseExpandable.setVisibility(View.VISIBLE);
                    licenseButton.setImageResource(R.drawable.ic_baseline_expand_less_24);
                }
            }
        });

        disclaimerCardView = findViewById(R.id.disc_cardview);
        disclaimerExpandable = findViewById(R.id.disc_expandable);
        disclaimerButton = findViewById(R.id.disc_button);
        disclaimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (disclaimerExpandable.getVisibility() == View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(disclaimerCardView, new AutoTransition());
                    disclaimerExpandable.setVisibility(View.GONE);
                    disclaimerButton.setImageResource(R.drawable.ic_baseline_expand_more_24);
                } else {
                    TransitionManager.beginDelayedTransition(disclaimerCardView, new AutoTransition());
                    disclaimerExpandable.setVisibility(View.VISIBLE);
                    disclaimerButton.setImageResource(R.drawable.ic_baseline_expand_less_24);
                }
            }
        });

        appsCardView = findViewById(R.id.apps_cardview);
        appsExpandable = findViewById(R.id.apps_expandable);
        appsButton = findViewById(R.id.apps_button);
        appsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (appsExpandable.getVisibility() == View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(appsCardView, new AutoTransition());
                    appsExpandable.setVisibility(View.GONE);
                    appsButton.setImageResource(R.drawable.ic_baseline_expand_more_24);
                } else {
                    TransitionManager.beginDelayedTransition(appsCardView, new AutoTransition());
                    appsExpandable.setVisibility(View.VISIBLE);
                    appsButton.setImageResource(R.drawable.ic_baseline_expand_less_24);
                }
            }
        });

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

        resourcesCardView = findViewById(R.id.res_cardview);
        resourcesExpandable = findViewById(R.id.res_expandable);
        resourcesButton = findViewById(R.id.res_button);
        resourcesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (resourcesExpandable.getVisibility() == View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(resourcesCardView, new AutoTransition());
                    resourcesExpandable.setVisibility(View.GONE);
                    resourcesButton.setImageResource(R.drawable.ic_baseline_expand_more_24);
                } else {
                    TransitionManager.beginDelayedTransition(resourcesCardView, new AutoTransition());
                    resourcesExpandable.setVisibility(View.VISIBLE);
                    resourcesButton.setImageResource(R.drawable.ic_baseline_expand_less_24);
                }
            }
        });

        libsCardView = findViewById(R.id.libs_cardview);
        libsExpandable = findViewById(R.id.libs_expandable);
        libsButton = findViewById(R.id.libs_button);
        libsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (libsExpandable.getVisibility() == View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(libsCardView, new AutoTransition());
                    libsExpandable.setVisibility(View.GONE);
                    libsButton.setImageResource(R.drawable.ic_baseline_expand_more_24);
                } else {
                    TransitionManager.beginDelayedTransition(libsCardView, new AutoTransition());
                    libsExpandable.setVisibility(View.VISIBLE);
                    libsButton.setImageResource(R.drawable.ic_baseline_expand_less_24);
                }
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
