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


import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Method;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class MainActivity extends LocalizedActivityBase implements HorizontalSwipeDetector.SwipeListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static MainActivity activity;

    public static final int REQUEST_CODE_BATTERY_OPTIMIZATIONS = 1;

    public static MainActivity getActivity() {
        return MainActivity.activity;
    }

    protected GestureDetector gestureDetector;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivity.activity = this;

        setupToolBar();

        gestureDetector = new GestureDetector(this, new HorizontalSwipeDetector(this));
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };

        findViewById(R.id.scroll_view).setOnTouchListener(gestureListener);
//        findViewById(R.id.image_view).setOnTouchListener(gestureListener);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {} // ignore result from permission request explanation
        );
        checkAndRequestBatteryOptimization();

    }


    @Override
    protected void onResume() {
        super.onResume();
        GWatchApplication app = (GWatchApplication) getApplication();
//        setConectionStatus(app.isSapInitialized() ? app.isConnected() : null);
    }

    @Override
    protected void onDestroy() {
        MainActivity.activity = null;
        super.onDestroy();
    }

    ///////////////////////////////////////////////////////////////////////////
    // SwipeListener implementation

    public boolean onLeftSwipe() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        return true;
    }

    public boolean onRightSwipe() {
        showPacketView(true);
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;

            case R.id.action_console:
                showPacketView(false);
                return true;

            case R.id.action_about:
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.app_name));
        toolbar.setTitleTextAppearance(this, R.style.textAppearanceTitleBold);

        setSupportActionBar(toolbar);
    }

    public void setConectionStatus(final Boolean isConnected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView statusView = (TextView) findViewById(R.id.status);
                if (isConnected == null) {
                    statusView.setText(StringUtils.EMPTY_STRING);
                } else {
                    int textId = isConnected ? R.string.conn_status_connected: R.string.conn_status_disconnected;
                    statusView.setText(getResources().getString(textId));
                    int colorId = isConnected ? R.color.status_indicator_connected : R.color.status_indicator_disconnected;
                    statusView.setTextColor(ContextCompat.getColor(MainActivity.this, colorId));
                }
            }
        });
    }

    public void showPacketView(boolean anim) {
        Intent intent = new Intent(MainActivity.this, PacketViewActivity.class);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        if (anim) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
        } else {
            overridePendingTransition(0, 0);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Background permission

    /*
     * Note that requesting this permission and sending this intent is likely to have your application removed from the Google PlayStore,
     * per the following warning in Android Studio:
     * “Use of REQUEST_IGNORE_BATTERY_OPTIMIZATIONS violates the Play Store Content Policy regarding acceptable use cases, as described in …”.
     */
    private void checkAndRequestBatteryOptimization() {
        if (UiUtils.requestPermission(this, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, REQUEST_CODE_BATTERY_OPTIMIZATIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
                String packageName = getPackageName();
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    try {
                        //some device doesn't has activity to handle this intent
                        //so add try catch
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        startActivity(intent);
                    } catch (Exception e) {
                        // intentionally blank
                    }
                }
            }
        }
    }


}