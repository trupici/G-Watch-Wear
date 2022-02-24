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

package sk.trupici.gwatch.wear.config;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.MotionEvent;

import java.util.concurrent.Executors;

import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.DumpUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

/**
 * Main {@code Activity} for Analog watch face configuration
 */
public class AnalogWatchfaceConfigActivity extends Activity implements ProviderInfoRetrieverActivity {

    public static final String LOG_TAG = AnalogWatchfaceConfigActivity.class.getSimpleName();

    public AnalogWatchfaceConfig config;

    private AnalogWatchfaceConfigViewAdapter configAdapter;
    private PageIndicatorAdapter pageIndicatorAdapter;

    private SharedPreferences prefs;

    // Required to retrieve complication data from watch face for preview.
    private ProviderInfoRetriever providerInfoRetriever;

    @Override
    public ProviderInfoRetriever getProviderInfoRetriever() {
        return providerInfoRetriever;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            try {
                super.onRestoreInstanceState(savedInstanceState);
            } catch (Exception e) {
                Log.e(LOG_TAG, ">>>>>>>>>>>>> onRestoreInstanceState: failed");
                if (BuildConfig.DEBUG) {
                    DumpUtils.dumpBundle(savedInstanceState, ">>>");
                }
                savedInstanceState = null;
            }
        } else {
            super.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            // log non-public API access
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        config = new AnalogWatchfaceConfig();

        setContentView(R.layout.layout_config_main);

        ViewPager2 viewPager = findViewById(R.id.horizontal_pager);
        viewPager.setNestedScrollingEnabled(true);
        configAdapter = new AnalogWatchfaceConfigViewAdapter(this, config, viewPager, prefs);
        viewPager.setAdapter(configAdapter);

        pageIndicatorAdapter = new PageIndicatorAdapter(
                findViewById(R.id.page_indicator),
                viewPager.getAdapter().getItemCount(),
                viewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Initialization of code to retrieve active complication data for the watch face.
        this.providerInfoRetriever = new ProviderInfoRetriever(this, Executors.newCachedThreadPool());
        providerInfoRetriever.init();
    }

    @Override
    protected void onStop() {
        super.onStop();

        configAdapter.destroy();
        if (pageIndicatorAdapter != null) {
            pageIndicatorAdapter.destroy();
        }

        providerInfoRetriever.release();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return configAdapter.onGenericMotionEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        configAdapter.dispatchActivityResult(requestCode, resultCode, data);
    }
}
