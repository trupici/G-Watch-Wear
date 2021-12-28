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
import android.view.MotionEvent;

import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

public class StandardAnalogWatchFaceConfigActivity extends Activity {

    public static final String LOG_TAG = StandardAnalogWatchFaceConfigActivity.class.getSimpleName();

    final public static int COMPLICATION_CONFIG_REQUEST_CODE = 101;
    final public static int UPDATE_COLORS_CONFIG_REQUEST_CODE = 102;
    final public static int BORDER_TYPE_CONFIG_REQUEST_CODE = 103;


    public AnalogWatchfaceConfig config;

    private ViewPager2 viewPager;
    private MainConfigViewAdapter configAdapter;
    private PageIndicatorAdapter pageIndicatorAdapter;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        config = new AnalogWatchfaceConfig();

        onCreatePageView(this);
    }

    private void onCreatePageView(Context context) {
        setContentView(R.layout.layout_main_config);

        viewPager = findViewById(R.id.horizontal_pager);
        viewPager.setNestedScrollingEnabled(true);
        configAdapter = new MainConfigViewAdapter(this, config, viewPager, prefs);
        viewPager.setAdapter(configAdapter);

        pageIndicatorAdapter = new PageIndicatorAdapter(
                findViewById(R.id.page_indicator),
                viewPager.getAdapter().getItemCount(),
                viewPager);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if (BuildConfig.DEBUG) {
            PreferenceUtils.dumpPreferences(sharedPref);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        configAdapter.destroy();
        pageIndicatorAdapter.destroy();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
//        Log.d(LOG_TAG, "onGenericMotionEvent: " + event);
        return configAdapter.onGenericMotionEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        configAdapter.dispatchActivityResult(requestCode, resultCode, data);
    }
}
