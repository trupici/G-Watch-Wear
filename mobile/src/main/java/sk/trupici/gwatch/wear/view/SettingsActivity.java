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

package sk.trupici.gwatch.wear.view;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.ConfigPacket;
import sk.trupici.gwatch.wear.data.MediaPacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.data.TLV;
import sk.trupici.gwatch.wear.dispatch.Dispatcher;
import sk.trupici.gwatch.wear.followers.DexcomShareFollowerService;
import sk.trupici.gwatch.wear.followers.FollowerService;
import sk.trupici.gwatch.wear.followers.NightScoutFollowerService;
import sk.trupici.gwatch.wear.settings.GlucoseLevelPreference;
import sk.trupici.gwatch.wear.settings.PreferenceMap;
import sk.trupici.gwatch.wear.settings.TimePreference;
import sk.trupici.gwatch.wear.settings.ValuePreference;
import sk.trupici.gwatch.wear.settings.fragment.MainFragment;
import sk.trupici.gwatch.wear.util.LangUtils;
import sk.trupici.gwatch.wear.util.MediaUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.widget.WidgetUpdateService;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

public class SettingsActivity extends LocalizedActivityBase implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SharedPreferences.OnSharedPreferenceChangeListener,
        HorizontalSwipeDetector.SwipeListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        FragmentResultListener {


    public static final String EXTRA_REQUEST_CODE = "REQUEST_CODE";

    public static final int REQUEST_CODE_EXPORT_TO = 1;
    public static final int REQUEST_CODE_IMPORT_FROM = 2;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE_PERM_BKG = 3;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE_PERM_AOD_BKG = 4;

    public static final int REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_CRITICAL = 5;
    public static final int REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_LOW = 6;
    public static final int REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_HIGH = 7;
    public static final int REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_FASTDROP = 8;
    public static final int REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_NODATA = 9;

    public static final int REQUEST_CODE_DEXCOM_PERMISSION = 10;

    private static final String CONFIG_FILE_NAME = "gwatch-settings.xml";
    private static final String CONFIG_MEDIA_TYPE = "application/xml";

    protected Map<String, Preference> changedPrefs = new HashMap<>();

    protected View.OnTouchListener gestureListener;
    protected GestureDetector gestureDetector;

    protected MediaUtils.MediaDesc bkgImageDesc = null;
    protected MediaUtils.MediaDesc aodBkgImageDesc = null;

    protected MediaUtils.MediaDesc criticalAlarmsSoundDesc = null;
    protected MediaUtils.MediaDesc lowAlarmsSoundDesc = null;
    protected MediaUtils.MediaDesc highAlarmsSoundDesc = null;
    protected MediaUtils.MediaDesc fastdropAlarmsSoundDesc = null;
    protected MediaUtils.MediaDesc nodataAlarmsSoundDesc = null;

    public static final String MAIN_PREFS_KEY = "pref_main";

    private Map<String, Preference> preferenceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("gwatch", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new MainFragment(), MAIN_PREFS_KEY)
                    .commit();
        }

        preferenceMap = PreferenceUtils.getAllPreferences(this);

        resetBackgroundImageConfig();
        resetAodBackgroundImageConfig();

        resetCriticalAlarmsSoundConfig();
        resetLowAlarmsSoundConfig();
        resetHighAlarmsSoundConfig();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager()
                .getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(args);
//        fragment.setTargetFragment(caller, 0);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment, pref.getKey())
                .addToBackStack(pref.getKey())
                .commit();

        getSupportFragmentManager().setFragmentResultListener("0", this, this);

        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        toolbar.inflateMenu(R.menu.menu_settings);

        MenuCompat.setGroupDividerEnabled(toolbar.getMenu(), true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(false);
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    PreferenceFragmentCompat fragment = getVisibleFragment();
                    if (fragment == null) {
                        Log.e(GWatchApplication.LOG_TAG, "Could not find fragment: " );//+ FRAGMENT_TAG);
                        return false;
                    }
                    PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
                    int numPreferences = preferenceScreen.getPreferenceCount();
                    int id = item.getItemId();
                    switch (id) {
                        case R.id.action_reset_all:
                            new AlertDialog.Builder(SettingsActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.settings_reset_all_message)
                                    .setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int pos) {
                                            Log.d(LOG_TAG, "onClick: " + pos);
                                            try {
                                                changedPrefs.clear();
                                                Map<String, Preference> allPrefs = new HashMap<>();
                                                for (int i = 0; i < numPreferences; i++) {
                                                    Preference preference = preferenceScreen.getPreference(i);
                                                    PreferenceUtils.getAllInHierarchy(SettingsActivity.this, preference, allPrefs);
                                                }
                                                for (Preference pref : allPrefs.values()) {
                                                    PreferenceUtils.resetToDefaultValue(pref);
                                                }
                                                PreferenceUtils.enableSecondHandTailCapOptions(preferenceScreen);

//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_background_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_background_type", PreferenceUtils.DEF_VALUE_BKG_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_dial_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_dial_type", PreferenceUtils.DEF_VALUE_DIAL));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_connection_bt",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_connection_type", PreferenceUtils.DEF_VALUE_CONN_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_graph_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_graph_type", PreferenceUtils.DEF_VALUE_GRAPH_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_graph_refresh_rate",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_graph_refresh_rate", PreferenceUtils.DEF_VALUE_GRAPH_REFRESH));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_aod_type_analog",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_aod_type", PreferenceUtils.DEF_VALUE_AOD_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_aod_analog_indicator_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_aod_analog_indicator_type", PreferenceUtils.DEF_VALUE_AOD_ANALOG_INDICATOR_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_aod_analog_dial_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_aod_analog_dial_type", PreferenceUtils.DEF_VALUE_AOD_ANALOG_DIAL_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_calendar_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_calendar_type", PreferenceUtils.DEF_CALENDAR_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_sensor1_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_sensor1_type", PreferenceUtils.DEF_SENSOR_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_sensor2_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_sensor2_type", PreferenceUtils.DEF_SENSOR_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_alarms_enable",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_alarms_type", PreferenceUtils.DEF_ALARMS_TYPE));
//                                                PreferenceUtils.setCustomPreference(allPrefs, "pref_digital_time_format",
//                                                        new ValuePreference(SettingsActivity.this, "cfg_digital_time_format", PreferenceUtils.DEF_DIGITAL_TIME_FORMAT));
//                                                resetBackgroundImageConfig();
//                                                resetAodBackgroundImageConfig();
//                                                resetCriticalAlarmsSoundConfig();
//                                                resetLowAlarmsSoundConfig();
//                                                resetHighAlarmsSoundConfig();
//                                                applyValuesToWatch(allPrefs);
                                                // TODO
                                            } catch (Throwable t) {
                                                Log.e(LOG_TAG, "Configuration reset failed", t);
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.action_no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int pos) {
                                            dialog.cancel();
                                        }
                                    })
                                    .show();
                            return true;
//                        case R.id.action_apply_all:
//                            Map<String, Preference> allPrefs = new HashMap<>();
//                            for (int i = 0; i < numPreferences; i++) {
//                                Preference preference = preferenceScreen.getPreference(i);
//                                PreferenceUtils.getAllInHierarchy(SettingsActivity.this, preference, allPrefs);
//                            }
//                            for (String key : allPrefs.keySet()) {
//                                changedPrefs.remove(key);
//                            }
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_background_enable", getBackgroundConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_dial_enable", getDialConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_connection_bt", getConnectionConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_graph_enable", getGraphTypeConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_graph_refresh_rate", getGraphRefreshConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_aod_type_analog", getAodTypeConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_aod_analog_indicator_enable", getAodAnalogIndicatorConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_aod_pref_dial_enable", getAodAnalogDialConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_calendar_enable", getCalendarConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_sensor1_enable", getSensor1Config(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_sensor2_enable", getSensor2Config(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_alarms_enable", getAlarmsConfig(allPrefs));
//                            PreferenceUtils.setCustomPreference(allPrefs, "pref_digital_time_format", getDigitalTimeFormatConfig(allPrefs));
//                            applyValuesToWatch(allPrefs);
//                            return true;
//                        case R.id.action_apply:
//                            applyValuesToWatch(changedPrefs);
//                            changedPrefs.clear();
//                            return true;
                        case R.id.action_export:
                            selectExportFilePath();
                            return true;
                        case R.id.action_import:
                            selectImportFilePath();
                            return true;
                    }
                } catch (Throwable t) {
                    // catch all exception to close menu item
                    Log.e(LOG_TAG, "Configuration failed", t);
                }
                return false;
            }
        });

        gestureDetector = new GestureDetector(this, new HorizontalSwipeDetector(this));
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
    }

    public void setToolbarTitle(CharSequence title) {
        if (title == null) {
            setTitle(R.string.action_settings);
        } else {
            setTitle(title);
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getTitle());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // SwipeListener implementation

    public boolean onLeftSwipe() {
        if (BuildConfig.DEBUG) {
            Log.i("gwatch", "Request for left swipe");
        }
        return false;
    }

    public boolean onRightSwipe() {
        if (BuildConfig.DEBUG) {
            Log.i("gwatch", "Request for right swipe");
        }
        finish(true);
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////


    public void finish(final boolean anim) {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
            return;
        }
//        if (!changedPrefs.isEmpty()) {
//            AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
//                    .setTitle(R.string.app_name)
//                    .setMessage(R.string.settings_save_changes_message)
//                    .setCancelable(false)
//                    .setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            // TODO
////                            applyValuesToWatch(changedPrefs);
//                            changedPrefs.clear();
//                            doFinish(anim);
//                        }
//                    })
//                    .setNegativeButton(R.string.action_no, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            changedPrefs.clear();
//                            doFinish(anim);
//                        }
//                    })
//                    .show();
//        } else {
            doFinish(anim);
//        }
    }

    protected void doFinish(boolean anim) {
        super.finish();
        if (anim) {
            this.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
        } else {
            this.overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onBackPressed() {
        finish(false);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (parent != null) {
            parent.setOnTouchListener(gestureListener);
        }
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(LOG_TAG, "onSharedPreferenceChanged: " + key);
        if (key == null) {
            return;
        }
        Preference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        if (!PreferenceUtils.loadSharedValue(pref, sharedPreferences)) {
            Log.e(LOG_TAG, "onSharedPreferenceChanged: failed to load changed value of " + key);
        }

        updatePrefSummary(pref);

//        if (PreferenceMap.data.containsKey(key)) { // direct mapping
//            addPreference(changedPrefs, pref);
//        } else { // custom mapping / advanced handling
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Pref changed: " + pref.getKey() + ": " + (pref instanceof TwoStatePreference ? ((TwoStatePreference) pref).isChecked() : "N/A"));
            }
            if (false) {
//            if ("pref_background_enable".equals(key) || "pref_background_standard".equals(key) || "pref_background_custom".equals(key) || "pref_background_fill".equals(key)) {
//                addPreference(changedPrefs, getBackgroundConfig(null));
//            } else if ("pref_dial_enable".equals(key) || "pref_dial_standard".equals(key) || "pref_dial_simple".equals(key)) {
//                addPreference(changedPrefs, getDialConfig(null));
//            } else if ("pref_connection_bt".equals(key) || "pref_connection_wifi".equals(key)) {
//                addPreference(changedPrefs, getConnectionConfig(null));
//            } else if ("pref_graph_enable".equals(key) || "pref_graph_type_dots".equals(key) || "pref_graph_type_line".equals(key)) {
//                addPreference(changedPrefs, getGraphTypeConfig(null));
//            } else if ("pref_graph_refresh_rate".equals(key)) {
//                addPreference(changedPrefs, getGraphRefreshConfig(null));
            } else if (key.startsWith("pref_widget")) {
                // fake preference value to indicate widget config change
                addPreference(changedPrefs, new ValuePreference(this, "pref_widget_changed", (byte)1));
//            } else if ("pref_aod_type_analog".equals(key) || "pref_aod_type_digital".equals(key) || "pref_aod_type_sport".equals(key)) {
//                addPreference(changedPrefs, getAodTypeConfig(null));
//            } else if ("pref_aod_analog_indicator_enable".equals(key) || "pref_aod_analog_indicator_type_label".equals(key) || "pref_aod_analog_indicator_type_value".equals(key) || "pref_aod_analog_indicator_type_value_big".equals(key) || "pref_aod_analog_indicator_type_hands".equals(key)) {
//                addPreference(changedPrefs, getAodAnalogIndicatorConfig(null));
//            } else if ("pref_aod_analog_dial_enable".equals(key) || "pref_aod_analog_dial_type_standard".equals(key) || "pref_aod_analog_dial_type_simple".equals(key)) {
//                addPreference(changedPrefs, getAodAnalogDialConfig(null));
//            } else if ("pref_calendar_enable".equals(key) || "pref_calendar_standard".equals(key) || "pref_calendar_day_of_week".equals(key)) {
//                addPreference(changedPrefs, getCalendarConfig(null));
//            } else if ("pref_sensor1_enable".equals(key) || "pref_sensor1_type_bat".equals(key) || "pref_sensor1_type_hrm".equals(key) || "pref_sensor1_type_pdm".equals(key) || "pref_sensor1_type_phone_bat".equals(key)) {
//                addPreference(changedPrefs, getSensor1Config(null));
//            } else if ("pref_sensor2_enable".equals(key) || "pref_sensor2_type_bat".equals(key) || "pref_sensor2_type_hrm".equals(key) || "pref_sensor2_type_pdm".equals(key) || "pref_sensor2_type_phone_bat".equals(key)) {
//                addPreference(changedPrefs, getSensor2Config(null));
//            } else if ("pref_alarms_enable".equals(key) || "pref_alarms_active_time".equals(key)) {
//                ValuePreference valuePref = getAlarmsConfig(null);
//                addPreference(changedPrefs, valuePref);
//                if (valuePref.getValue() == PreferenceUtils.VALUE_ALARMS_TIMED) {
//                    addPreference(changedPrefs, findPreference("cfg_alarms_active_from"));
//                    addPreference(changedPrefs, findPreference("cfg_alarms_active_to"));
//                }
//            } else if ("pref_alarms_critical_only".equals(key)) {
//                addPreference(changedPrefs, findPreference("cfg_alarms_warn_enable"));
//                addPreference(changedPrefs, findPreference("cfg_alarms_danger_enable"));
            } else if ("pref_data_source_nightscout_enable".equals(key)) {
                // fake preference value to indicate nightscout follower config change
                addPreference(changedPrefs, new ValuePreference(this, "pref_data_source_nightscout_enable", (byte)1));
            } else if (key.contains("pref_data_source_dexcom_share_enable")) {
                // fake preference value to indicate dexcom share follower config change
                addPreference(changedPrefs, new ValuePreference(this, "pref_data_source_dexcom_share_enable", (byte)1));
//            } else if ("pref_digital_time_format_system".equals(key) || "pref_digital_time_format_12".equals(key) || "pref_digital_time_format_24".equals(key) || "pref_sensor2_type_phone_bat".equals(key)) {
//                addPreference(changedPrefs, getDigitalTimeFormatConfig(null));
            } else if (PreferenceUtils.PREF_LOCALE.equals(key)) {
                restartIfLangChanged(((ListPreference) pref).getValue(), sharedPreferences);
            } else {
                changedPrefs.put(pref.getKey(), pref);
            }
//        }
        // transitive handling
        if ("cfg_glucose_units".equals(key)) { // force widget redraw on units change
            addPreference(changedPrefs, new ValuePreference(this, "pref_widget_changed", (byte) 1));
        }
    }

    private void addPreference(Map<String, Preference> prefs, Preference pref) {
        if (pref != null) {
            prefs.put(pref.getKey(), pref);
        }
    }

//    public void hideBatteryPanel() {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        if (sharedPreferences.getBoolean("cfg_battery_type", false)) {
//            sharedPreferences.edit().putBoolean("cfg_battery_type", false).apply();
//            addPreference(changedPrefs, new ValuePreference(this, "cfg_battery_type", PreferenceUtils.VALUE_BATTERY_TYPE_OFF));
//            Toast.makeText(getApplicationContext(), R.string.warn_battery_panel_hidden, Toast.LENGTH_LONG).show();
//        }
//    }
//
//    public void hideGWatchLogo() {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        if (sharedPreferences.getBoolean("cfg_show_logo", true)) {
//            sharedPreferences.edit().putBoolean("cfg_show_logo", false).apply();
//            addPreference(changedPrefs, new ValuePreference(this, "cfg_show_logo", PreferenceUtils.VALUE_LOGO_HIDDEN));
////            Toast.makeText(getApplicationContext(), R.string.warn_logo_was_hidden, Toast.LENGTH_LONG).show();
//        }
//    }

//    private ValuePreference getGraphRefreshConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getGraphRefreshConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_graph_refresh_rate", prefsMap)) {
//            value = PreferenceUtils.VALUE_GRAPH_REFRESH_1_MIN;
//        } else {
//            value = PreferenceUtils.VALUE_GRAPH_REFRESH_5_MIN;
//        }
//        return new ValuePreference(this, "cfg_graph_refresh_rate", value);
//    }

//    private ValuePreference getGraphTypeConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getGraphTypeConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_graph_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_graph_type_dots", prefsMap)) {
//                value = PreferenceUtils.VALUE_GRAPH_TYPE_DOTS;
//            } else if (isTwoStatePreferenceConfigured("pref_graph_type_line", prefsMap)) {
//                value = PreferenceUtils.VALUE_GRAPH_TYPE_LINE;
//            } else {
//                return null;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_GRAPH_TYPE_OFF;
//        }
//        return new ValuePreference(this, "cfg_graph_type", value);
//    }

//    private ValuePreference getBackgroundConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getBackgroundConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_background_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_background_standard", prefsMap)) {
//                value = PreferenceUtils.VALUE_BKG_STD;
//                resetBackgroundImageConfig();
//            } else if (isTwoStatePreferenceConfigured("pref_background_custom", prefsMap)) {
//                value = PreferenceUtils.VALUE_BKG_CUSTOM;
//            } else {
//                return null;
//            }
//        } else if (isTwoStatePreferenceConfigured("pref_background_fill", prefsMap)) {
//            value = PreferenceUtils.VALUE_BKG_COLOR;
//            resetBackgroundImageConfig();
//        } else {
//            return null;
//        }
//        return new ValuePreference(this, "cfg_background_type", value);
//    }
//
//    private ValuePreference getDialConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getDialConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_dial_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_dial_standard", prefsMap)) {
//                value = PreferenceUtils.VALUE_DIAL_STD;
//            } else if (isTwoStatePreferenceConfigured("pref_dial_simple", prefsMap)) {
//                value = PreferenceUtils.VALUE_DIAL_SIMPLE;
//            } else {
//                return null;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_DIAL_OFF;
//        }
//        return new ValuePreference(this, "cfg_dial_type", value);
//    }
//
//    private ValuePreference getConnectionConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getConnectionConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value = 0;
//        if (isTwoStatePreferenceConfigured("pref_connection_bt", prefsMap)) {
//            value += PreferenceUtils.VALUE_CONN_TYPE_BT;
//        }
//        if (isCheckBoxPreferenceConfiguredEnabled("pref_connection_wifi", prefsMap)) {
//            value += PreferenceUtils.VALUE_CONN_TYPE_WIFI;
//        }
//        if (value == 0) {
//            return null;
//        }
//        return new ValuePreference(this, "cfg_connection_type", value);
//    }
//
//    private ValuePreference getAodTypeConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getAodTypeConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value = 0;
//        if (isTwoStatePreferenceConfigured("pref_aod_type_analog", prefsMap)) {
//            value = PreferenceUtils.VALUE_AOD_TYPE_ANALOG;
//        } else if (isCheckBoxPreferenceConfiguredEnabled("pref_aod_type_sport", prefsMap)) {
//            value = PreferenceUtils.VALUE_AOD_TYPE_SPORT;
//        } else if (isCheckBoxPreferenceConfiguredEnabled("pref_aod_type_digital", prefsMap)) {
//            value = PreferenceUtils.VALUE_AOD_TYPE_DIGITAL;
//        }
//        if (value == 0) {
//            return null;
//        }
//        return new ValuePreference(this, "cfg_aod_type", value);
//    }
//
//    private ValuePreference getAodAnalogIndicatorConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getAodAnalogIndicatorConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value = 0;
//        if (isTwoStatePreferenceConfigured("pref_aod_analog_indicator_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_aod_analog_indicator_type_label", prefsMap)) {
//                value = PreferenceUtils.VALUE_AOD_ANALOG_INDICATOR_TYPE_LABEL;
//            } else if (isCheckBoxPreferenceConfiguredEnabled("pref_aod_analog_indicator_type_value", prefsMap)) {
//                value = PreferenceUtils.VALUE_AOD_ANALOG_INDICATOR_TYPE_VALUE;
//            } else if (isCheckBoxPreferenceConfiguredEnabled("pref_aod_analog_indicator_type_value_big", prefsMap)) {
//                value = PreferenceUtils.VALUE_AOD_ANALOG_INDICATOR_TYPE_VALUE_BIG;
//            } else if (isCheckBoxPreferenceConfiguredEnabled("pref_aod_analog_indicator_type_hands", prefsMap)) {
//                value = PreferenceUtils.VALUE_AOD_ANALOG_INDICATOR_TYPE_HANDS;
//            } else {
//                return null;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_AOD_ANALOG_INDICATOR_TYPE_OFF;
//        }
//        return new ValuePreference(this, "cfg_aod_analog_indicator_type", value);
//    }
//
//    private ValuePreference getAodAnalogDialConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getAodAnalogDialConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value = 0;
//        if (isTwoStatePreferenceConfigured("pref_aod_analog_dial_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_aod_analog_dial_type_standard", prefsMap)) {
//                value = PreferenceUtils.VALUE_AOD_ANALOG_DIAL_TYPE_STANDARD;
//            } else if (isCheckBoxPreferenceConfiguredEnabled("pref_aod_analog_dial_type_simple", prefsMap)) {
//                value = PreferenceUtils.VALUE_AOD_ANALOG_DIAL_TYPE_SIMPLE;
//            } else {
//                return null;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_AOD_ANALOG_DIAL_TYPE_OFF;
//        }
//        return new ValuePreference(this, "cfg_aod_analog_dial_type", value);
//    }
//
//    private ValuePreference getCalendarConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getCalendarConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_calendar_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_calendar_standard", prefsMap)) {
//                value = PreferenceUtils.VALUE_CALENDAR_STD;
//            } else if (isTwoStatePreferenceConfigured("pref_calendar_day_of_week", prefsMap)) {
//                value = PreferenceUtils.VALUE_CALENDAR_DAY_OF_WEEK;
//            } else {
//                return null;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_CALENDAR_OFF;
//        }
//        return new ValuePreference(this, "cfg_calendar_type", value);
//    }
//
//    private ValuePreference getSensor1Config(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getSensor1Config: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_sensor1_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_sensor1_type_bat", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_BAT;
//            } else if (isTwoStatePreferenceConfigured("pref_sensor1_type_hrm", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_HRM;
//            } else if (isTwoStatePreferenceConfigured("pref_sensor1_type_pdm", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_PDM;
//            } else if (isTwoStatePreferenceConfigured("pref_sensor1_type_phone_bat", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_PHONE_BAT;
//            } else {
//                return null;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_SENSOR_OFF;
//        }
//        return new ValuePreference(this, "cfg_sensor1_type", value);
//    }
//
//    private ValuePreference getSensor2Config(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getSensor2Config: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_sensor2_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_sensor2_type_bat", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_BAT;
//            } else if (isTwoStatePreferenceConfigured("pref_sensor2_type_hrm", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_HRM;
//            } else if (isTwoStatePreferenceConfigured("pref_sensor2_type_pdm", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_PDM;
//            } else if (isTwoStatePreferenceConfigured("pref_sensor2_type_phone_bat", prefsMap)) {
//                value = PreferenceUtils.VALUE_SENSOR_PHONE_BAT;
//                // set fresh battery status to watch
//                BatteryStatusReceiver.force(getApplicationContext());
//            } else {
//                return null;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_SENSOR_OFF;
//        }
//        return new ValuePreference(this, "cfg_sensor2_type", value);
//    }
//
//    private ValuePreference getAlarmsConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getAlarmsConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("pref_alarms_enable", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_alarms_active_time", prefsMap)) {
//                value = PreferenceUtils.VALUE_ALARMS_TIMED;
//            } else {
//                value = PreferenceUtils.VALUE_ALARMS_ON;
//            }
//        } else {
//            value = PreferenceUtils.VALUE_ALARMS_OFF;
//        }
//        return new ValuePreference(this, "cfg_alarms_type", value);
//    }
//
//    private ValuePreference getDigitalTimeFormatConfig(Map<String, Preference> prefsMap) {
//        if (BuildConfig.DEBUG) {
//            Log.d(LOG_TAG, "getDigitalTimeFormatConfig: " + (prefsMap != null ? prefsMap.size() : null));
//        }
//        byte value;
//        if (isTwoStatePreferenceConfigured("cfg_digital_type", prefsMap)) {
//            if (isTwoStatePreferenceConfigured("pref_digital_time_format_12", prefsMap)) {
//                value = PreferenceUtils.VALUE_DIGITAL_TIME_FORMAT_12;
//            } else if (isTwoStatePreferenceConfigured("pref_digital_time_format_24", prefsMap)) {
//                value = PreferenceUtils.VALUE_DIGITAL_TIME_FORMAT_24;
//            } else {
//                value = PreferenceUtils.VALUE_DIGITAL_TIME_FORMAT_SYSTEM;
//            }
//        } else {
//            value = PreferenceUtils.DEF_DIGITAL_TIME_FORMAT;
//        }
//        return new ValuePreference(this, "cfg_digital_time_format", value);
//    }

    private boolean isCheckBoxPreferenceConfiguredEnabled(String key, Map<String, Preference> prefsMap) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "isCheckBoxPreferenceConfiguredEnabled: " + key + ", " + (prefsMap != null ? prefsMap.size() : null));
        }
        CheckBoxPreference preference = (CheckBoxPreference) findPreference(key, prefsMap);
        return preference != null && preference.isEnabled() && preference.isChecked();
    }

    private boolean isTwoStatePreferenceConfigured(String key, Map<String, Preference> prefsMap) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "isTwoStatePreferenceConfigured: " + key + ", " + (prefsMap != null ? prefsMap.size() : null));
        }
        TwoStatePreference preference = (TwoStatePreference) findPreference(key, prefsMap);
        return preference != null && preference.isChecked();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(GWatchApplication.getAppContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(GWatchApplication.getAppContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    public void updatePrefSummary(Preference pref) {
        PreferenceFragmentCompat visibleFragment = getVisibleFragment();
        if ("cfg_glucose_units".equals(pref.getKey()) && visibleFragment != null) {
            RecyclerView view = visibleFragment.getListView();
            if (view != null) {
                for (int i = 0; i < view.getAdapter().getItemCount(); i++) {
                    view.getAdapter().notifyItemChanged(i);
                }
            }
        }
        if (pref instanceof GlucoseLevelPreference) {
            // do not update title
        } else if (pref instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) pref;
            String value = editTextPref.getText();
            editTextPref.setTitle(applyValue(editTextPref.getTitle().toString(), value, null));
        } else if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            String value = listPref.getEntry().toString();
            listPref.setSummary(value);
        }
    }

    protected static String applyValue(String content, String value, String postfix) {
        final String placeholder = ":";
        StringBuffer buf = new StringBuffer(content);
        int idx = buf.lastIndexOf(placeholder);
        if (idx < 0) {
            return content;
        } else {
            buf.setLength(idx + placeholder.length());
        }
        buf.append(" ").append(value);
        if (postfix != null) {
            buf.append(" ").append(postfix);
        }
        return buf.toString();
    }

//    protected void applyValuesToWatch(final Map<String, Preference> changedPrefsMap) {
//        try {
//            if (createAndSendConfiguration(changedPrefsMap.values())) {
//                // reset preference if requested
//                for (String prefName : PreferenceMap.resetablePrefs) {
//                    if (changedPrefsMap.containsKey(prefName)) {
//                        PreferenceUtils.resetToDefaultValue(changedPrefsMap.get(prefName));
//                    }
//                }
//            }
//        } catch (Throwable t) {
//            Log.e(LOG_TAG, "Send configuration failed", t);
//            UiUtils.showToast(this, R.string.cfg_transfer_failed);
//        }
//    }

//    protected boolean createAndSendConfiguration(Collection<Preference> changedPrefs) {
//        Log.i(GWatchApplication.LOG_TAG, "createAndSendConfiguration: " + changedPrefs.size());
//
//        boolean result = false;
//        List<TLV> config = new ArrayList<>(changedPrefs.size());
//        int totalLen = 0;
//        boolean widgetPrefsUpdated = false;
//        boolean nightscoutPrefsUpdated = false;
//        boolean dexcomSharePrefsUpdated = false;
//
//        for (Preference pref : changedPrefs) {
//            if (BuildConfig.DEBUG) {
//                Log.d(GWatchApplication.LOG_TAG, "Changed pref: " + pref.getKey() + " -> " + PreferenceUtils.getValue(pref));
//            }
//            if (pref.getKey() == null) {
//                Log.w(LOG_TAG, "Preference key is null!");
//                continue;
//            }
//
//            if (pref.getKey().contains("nightscout")) {
//                nightscoutPrefsUpdated = true;
//            } else if (pref.getKey().contains("dexcom_share")) {
//                dexcomSharePrefsUpdated = true;
//            }
//
//// Debug for graph showing issue
////            if (pref.getKey().equals("pref_graph_enable")) {
////                String msg = "Graph settings: pref_graph_enable: " + PreferenceUtils.getValue(pref);
////                UiUtils.showAlertDialog(this, msg, "GWatch");
////            }
//
//            Pair<Byte, PreferenceMap.PreferenceType> mapping = PreferenceMap.data.get(pref.getKey());
//            if (mapping != null) { // direct cfg mapping
//
//// Debug for graph showing issue
////                if (pref.getKey().equals("cfg_graph_type")) {
////                    String msg = "Graph settings:"
////                        + "\ncfg_graph_type: " + PreferenceUtils.getValue(pref)
////                        + "\nmapping: " + mapping;
////                    UiUtils.showAlertDialog(this, msg, "GWatch");
////                }
//
//
//                Integer value = null;
//                String strValue = null;
//                if (pref instanceof ValuePreference) {
//                    value = ((ValuePreference) pref).getValue();
//                } else if (pref instanceof TimePreference) {
//                    String time = (String)PreferenceUtils.getValue(pref);
//                    value = 60 * TimePreference.getHour(time);
//                    value += TimePreference.getMinute(time);
//                } else {
//                    Object tmpValue = PreferenceUtils.getValue(pref);
//                    if (tmpValue instanceof String) {
//                        if (mapping.second == PreferenceMap.PreferenceType.STRING) {
//                            strValue = (String) tmpValue;
//                        } else {
//                            value = Integer.valueOf((String) tmpValue);
//                        }
//                    } else if (tmpValue instanceof Boolean) {
//                        value = ((Boolean)tmpValue) ? 1 : 0;
//                    } else {
//                        value = (Integer) tmpValue;
//                    }
//                }
//                if (value == null && strValue == null && mapping.second != PreferenceMap.PreferenceType.STRING) {
//                    Log.w(LOG_TAG, "Null preference value: " + pref.getKey());
//                    continue;
//                }
//
//                byte data[];
//                if (mapping.second == PreferenceMap.PreferenceType.BYTE) {
//                    data = new byte[1];
//                    data[0] = value.byteValue();
//                    if (BuildConfig.DEBUG) {
//                        Log.d(GWatchApplication.LOG_TAG,"Send BYTE: " + pref.getKey() + ": " + value);
//                    }
//                } else if (mapping.second == PreferenceMap.PreferenceType.WORD) {
//                    data = new byte[2];
//                    data[0] = (byte) ((value & 0xFF00) >> 8);
//                    data[1] = (byte) (value & 0xFF);
//                    if (BuildConfig.DEBUG) {
//                        Log.d(GWatchApplication.LOG_TAG,"Send WORD: " + pref.getKey() + ": " + value);
//                    }
//                } else if (mapping.second == PreferenceMap.PreferenceType.DWORD) {
//                    data = new byte[4];
//                    data[0] = (byte) ((value & 0xFF000000) >> 24);
//                    data[1] = (byte) ((value & 0xFF0000) >> 16);
//                    data[2] = (byte) ((value & 0xFF00) >> 8);
//                    data[3] = (byte) (value & 0xFF);
//                    if (BuildConfig.DEBUG) {
//                        Log.d(GWatchApplication.LOG_TAG,"Send DWORD: " + pref.getKey() + ": " + value);
//                    }
//                } else if (mapping.second == PreferenceMap.PreferenceType.COLOR) {
//                    data = new byte[4];
//                    data[0] = (byte) ((value & 0xFF000000) >> 24);
//                    data[1] = (byte) ((value & 0xFF0000) >> 16);
//                    data[2] = (byte) ((value & 0xFF00) >> 8);
//                    data[3] = (byte) (value & 0xFF);
//                    if (BuildConfig.DEBUG) {
//                        Log.d(GWatchApplication.LOG_TAG,"Send COLOR: " + pref.getKey() + ": " + value);
//                    }
//                } else if (mapping.second == PreferenceMap.PreferenceType.BOOLEAN) {
//                    data = new byte[1];
//                    data[0] = value.byteValue();
//                    if (BuildConfig.DEBUG) {
//                        Log.d(GWatchApplication.LOG_TAG,"Send BOOLEAN: " + pref.getKey() + ": "
//                                + (value == 0 ? "false" : "true"));
//                    }
//                } else if (mapping.second == PreferenceMap.PreferenceType.STRING) {
//                    int len = 0;
//                    if (strValue != null) {
//                        strValue = StringUtils.normalize(strValue);
//                        len = strValue.length();
//                    }
//                    data = new byte[len];
//                    for (int i=0; i<len; i++) {
//                        data[i] = (byte)strValue.charAt(i);
//                    }
//                    if (BuildConfig.DEBUG) {
//                        Log.d(GWatchApplication.LOG_TAG,"Send STRING: " + pref.getKey() + ": "
//                                + strValue);
//                    }
//                } else {
//                    Log.w(LOG_TAG, "Skipping preference: " + pref.getKey());
//                    continue;
//                }
//                TLV tlv = new TLV(mapping.first, (byte)data.length, data);
//                config.add(tlv);
//                totalLen += tlv.getTotalLen();
//            } else { // indirect cfg mapping - try to apply dedicated cfg logic
//                if (pref.getKey().startsWith("pref_widget_")) {
//                    widgetPrefsUpdated = true;
//                }
//            }
//        }
//
//        Dispatcher dispatcher = (Dispatcher) getApplication();
//        if (totalLen > 0) {
//            Packet packet = new ConfigPacket(config, totalLen);
//            if (dispatcher.dispatchNow(packet)) {
//                if (BuildConfig.DEBUG) {
//                    Log.d(LOG_TAG, "Config Packet sent: " + config.size() + " in " + totalLen + " bytes");
//                }
//                UiUtils.showToast(this, R.string.cfg_transfer_ok);
//                result = true;
//            } else {
//                UiUtils.showToast(this, R.string.cfg_transfer_failed);
//                result = false;
//            }
//        } else if (changedPrefs.size() > 0
//                && bkgImageDesc == null && aodBkgImageDesc == null
//                && criticalAlarmsSoundDesc == null && lowAlarmsSoundDesc == null && highAlarmsSoundDesc == null
//                && fastdropAlarmsSoundDesc == null && nodataAlarmsSoundDesc == null) {
//            UiUtils.showToast(this, R.string.cfg_applied);
//            result = true;
//        }
//
//        // send media files if available
//        if (sendImageFile(dispatcher, bkgImageDesc, MediaPacket.MediaType.IMAGE_BACKGROUND, PreferenceUtils.VALUE_BKG_IMAGE_FILE, "background")) {
//            resetBackgroundImageConfig();
//        }
//        if (sendImageFile(dispatcher, aodBkgImageDesc, MediaPacket.MediaType.IMAGE_AOD_BACKGROUND, PreferenceUtils.VALUE_AOD_BKG_IMAGE_FILE, "AOD background")) {
//            resetAodBackgroundImageConfig();
//        }
//        if (sendAudioFile(dispatcher, criticalAlarmsSoundDesc, MediaPacket.MediaType.SOUND_ALARM_CRITICAL, PreferenceUtils.VALUE_ALARMS_SOUND_CRITICAL_FILE, "Critical alarm sound")) {
//            resetCriticalAlarmsSoundConfig();
//        }
//        if (sendAudioFile(dispatcher, lowAlarmsSoundDesc, MediaPacket.MediaType.SOUND_ALARM_LOW, PreferenceUtils.VALUE_ALARMS_SOUND_LOW_FILE, "Low alarm sound")) {
//            resetLowAlarmsSoundConfig();
//        }
//        if (sendAudioFile(dispatcher, highAlarmsSoundDesc, MediaPacket.MediaType.SOUND_ALARM_HIGH, PreferenceUtils.VALUE_ALARMS_SOUND_HIGH_FILE, "High alarm sound")) {
//            resetHighAlarmsSoundConfig();
//        }
//        if (sendAudioFile(dispatcher, fastdropAlarmsSoundDesc, MediaPacket.MediaType.SOUND_ALARM_FASTDROP, PreferenceUtils.VALUE_ALARMS_SOUND_FASTDROP_FILE, "Fast drop alarm sound")) {
//            resetFastdropAlarmsSoundConfig();
//        }
//        if (sendAudioFile(dispatcher, nodataAlarmsSoundDesc, MediaPacket.MediaType.SOUND_ALARM_NODATA, PreferenceUtils.VALUE_ALARMS_SOUND_NODATA_FILE, "High alarm sound")) {
//            resetNodataAlarmsSoundConfig();
//        }
//
//        if (widgetPrefsUpdated) {
//            // crete empty configuration packet just for widget notification
//            if (BuildConfig.DEBUG) {
//                Log.d(LOG_TAG, "Widget update request due to config change");
//            }
//            WidgetUpdateService.updateWidget(new ConfigPacket(Collections.emptyList(), 0));
//        }
//        if (nightscoutPrefsUpdated) {
//            if (BuildConfig.DEBUG) {
//                Log.d(LOG_TAG, "Nightscout follower update request due to config change");
//            }
//            FollowerService.reloadSettings(getApplicationContext(), NightScoutFollowerService.class);
//        }
//        if (dexcomSharePrefsUpdated) {
//            if (BuildConfig.DEBUG) {
//                Log.d(LOG_TAG, "Dexcom share follower update request due to config change");
//            }
//            FollowerService.reloadSettings(getApplicationContext(), DexcomShareFollowerService.class);
//        }
//        return result;
//    }

    /**
     * @return <b>true</b> if image descriptor shall be reset, <b>false</b> otherwise
     */
    private boolean sendImageFile(Dispatcher dispatcher, MediaUtils.MediaDesc mediaDesc, MediaPacket.MediaType mediaType, String fileName, String label) {
        if (mediaDesc != null) {
            if (BuildConfig.DEBUG) {
                Log.d(GWatchApplication.LOG_TAG,"Set: " + label + " -> " + mediaDesc.getName());
            }
            try {
                byte[] image = MediaUtils.readMediaToByteArray(getApplicationContext(), mediaDesc);
                Packet packet = new MediaPacket(mediaType, fileName, image);
                if (dispatcher.dispatchNow(packet)) {
                    UiUtils.showToast(this, R.string.image_transfer_ok);
                    return true;
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Sending image failed", e);
            }
            UiUtils.showToast(this, R.string.image_transfer_failed);
        }
        return false;
    }

    /**
     * @return <b>true</b> if media descriptor shall be reset, <b>false</b> otherwise
     */
    private boolean sendAudioFile(Dispatcher dispatcher, MediaUtils.MediaDesc mediaDesc, MediaPacket.MediaType mediaType, String fileName, String label) {
        if (mediaDesc != null) {
            if (BuildConfig.DEBUG) {
                Log.d(GWatchApplication.LOG_TAG,"Set: " + label + " -> " + mediaDesc.getName());
            }
            try {
                byte[] media = MediaUtils.readMediaToByteArray(getApplicationContext(), mediaDesc);
                Packet packet = new MediaPacket(mediaType, fileName, media);
                if (dispatcher.dispatchNow(packet)) {
                    UiUtils.showToast(this, R.string.audio_transfer_ok);
                    return true;
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Sending audio file failed", e);
            }
            UiUtils.showToast(this, R.string.audio_transfer_failed);
        }
        return false;
    }

    private Integer parseIntValue(String strValue) {
        if (strValue == null || strValue.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(strValue);
        } catch (NumberFormatException e) {
            try {
                return (int)Math.round(Double.parseDouble(strValue));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean isPermissionGranted = false;
        if (permissions.length == 0) {
            // request cancelled / interrupted
            // what to do now?
        }
        for (int result : grantResults) {
            if (result == PERMISSION_GRANTED) {
                isPermissionGranted = true;
                break;
            }
        }
        if (!isPermissionGranted) {
            return;
        }

        if (requestCode == REQUEST_CODE_EXTERNAL_STORAGE_PERM_BKG) {
            onSelectBackgroundFile(null);
        } else if (requestCode == REQUEST_CODE_EXTERNAL_STORAGE_PERM_AOD_BKG) {
            onSelectAodBackgroundFile(null);
        } else if (requestCode == REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_CRITICAL) {
            onSelectCriticalAlarmSound(null);
        } else if (requestCode == REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_LOW) {
            onSelectLowAlarmSound(null);
        } else if (requestCode == REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_HIGH) {
            onSelectHighAlarmSound(null);
        } else if (requestCode == REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_FASTDROP) {
            onSelectFastdropAlarmSound(null);
        } else if (requestCode == REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_NODATA) {
            onSelectNodataAlarmSound(null);
        }
    }

    protected void setSelectFilePreferenceSummary(String prefName, String name) {
        Preference preference = findPreference(prefName);
        if (preference != null) {
            if (name == null) {
                name = getResources().getString(R.string.selected_file_not_selected);
            }
            preference.setSummary(name);
        }
    }

    protected void resetBackgroundImageConfig() {
        bkgImageDesc = null;
        setSelectFilePreferenceSummary("ctrl_select_bkg_file", null);
    }

    protected void resetAodBackgroundImageConfig() {
        aodBkgImageDesc = null;
        setSelectFilePreferenceSummary("ctrl_aod_select_bkg_file", null);
    }

    protected void resetCriticalAlarmsSoundConfig() {
        criticalAlarmsSoundDesc = null;
        setSelectFilePreferenceSummary("ctrl_alarms_sound_critical_select_file", null);
    }

    protected void resetLowAlarmsSoundConfig() {
        lowAlarmsSoundDesc = null;
        setSelectFilePreferenceSummary("ctrl_alarms_sound_lows_select_file", null);
    }

    protected void resetHighAlarmsSoundConfig() {
        highAlarmsSoundDesc = null;
        setSelectFilePreferenceSummary("ctrl_alarms_sound_highs_select_file", null);
    }

    protected void resetFastdropAlarmsSoundConfig() {
        fastdropAlarmsSoundDesc = null;
        setSelectFilePreferenceSummary("ctrl_alarms_sound_fastdrop_select_file", null);
    }

    protected void resetNodataAlarmsSoundConfig() {
        nodataAlarmsSoundDesc = null;
        setSelectFilePreferenceSummary("ctrl_alarms_sound_nodata_select_file", null);
    }

    private Preference findPreference(String key, Map<String, Preference> prefsMap) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "findPreference: " + key + ", " + (prefsMap == null ? null : prefsMap.size()));
        }
        if (prefsMap == null) {
            prefsMap = preferenceMap;
        }
        if (prefsMap.containsKey(key)) {
            return prefsMap.get(key);
        } else {
            Log.e(GWatchApplication.LOG_TAG, "Could not find preference: " + key);
            return null;
        }
    }

    private Preference findPreference(String key) {
        return findPreference(key, null);
    }

    protected PreferenceFragmentCompat getVisibleFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int count = fragmentManager.getBackStackEntryCount();
        if (count > 0) {
            String tag = fragmentManager.getBackStackEntryAt(count - 1).getName();
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                return (PreferenceFragmentCompat) fragment;
            }
        }

        return (PreferenceFragmentCompat) fragmentManager.findFragmentByTag(MAIN_PREFS_KEY);
    }


    ///////////////////////////////////////////////////////////////////////////
    // imports / exports

    private ActivityResultLauncher<String> exportFileLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> exportSettings(getApplicationContext(), uri)
    );

    /**
     * Open system file dialog to select destination file path
     */
    protected void selectExportFilePath() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(CONFIG_MEDIA_TYPE);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(Intent.EXTRA_TITLE, CONFIG_FILE_NAME);
        intent.putExtra(EXTRA_REQUEST_CODE, REQUEST_CODE_EXPORT_TO);

        exportFileLauncher.launch(CONFIG_FILE_NAME);
    }

    /**
     * Export configuration to a file with provided path name
     */
    protected void exportSettings(Context context, Uri uri) {
        try (OutputStream output = context.getContentResolver().openOutputStream(uri, "rwt")) {
            // Preferences might not be stored already!!!

            PreferenceScreen preferenceScreen = getVisibleFragment().getPreferenceScreen();
            StringBuffer comment = new StringBuffer("G-Watch App");
            if (preferenceScreen.getTitle() != null) {
                comment.append(": ").append(preferenceScreen.getTitle());
            } else {
                comment.append(" Settings");
            }
            Map<String, Preference> allPrefs = new HashMap<>();
            for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                Preference preference = preferenceScreen.getPreference(i);
                PreferenceUtils.getAllInHierarchy(this, preference, allPrefs);
            }

            Properties props = new Properties();
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(PreferenceUtils.DUMMY_KEY_PREFIX)) { // do not export preferences with dummy keys
                    continue;
                }
                Object value = entry.getValue();
                if (!(value instanceof Preference)) {
                    Log.w(LOG_TAG, "Invalid pref value: " + key);
                    continue;
                }
                Preference pref = (Preference) value;
                Object prefValue = PreferenceUtils.getValue(pref);
                String strValue = prefValue == null ? StringUtils.EMPTY_STRING : String.valueOf(prefValue);
                props.put(key, strValue);
            }
            props.storeToXML(output, comment.toString(), "utf-8");
            output.flush();
            UiUtils.showToast(this, R.string.cfg_export_success);
        } catch (Throwable t) {
            Log.e(GWatchApplication.LOG_TAG, "Configuration export failed", t);
            UiUtils.showToast(this, R.string.cfg_export_failed);
        }
    }


    private ActivityResultLauncher<String[]> importFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> importSettings(getApplicationContext(), uri)
    );

    /**
     * Open system file dialog to select destination file path
     */
    protected void selectImportFilePath() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.setType(CONFIG_MEDIA_TYPE);
        intent.putExtra(Intent.EXTRA_TITLE, CONFIG_FILE_NAME);
        intent.putExtra(EXTRA_REQUEST_CODE, REQUEST_CODE_IMPORT_FROM);

        importFileLauncher.launch(new String[] {"*/*"});
    }

    /**
     * Import configuration from a file with provided path name
     */
    // FIXME check inconsistency in selections?
    protected void importSettings(Context context, Uri uri) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
            try (FileInputStream in = new FileInputStream(pfd.getFileDescriptor())) {
                Properties props = new Properties();
                props.loadFromXML(in);

                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    String key = (String)entry.getKey();
                    if (BuildConfig.DEBUG) {
                        Log.d(GWatchApplication.LOG_TAG, "Import pref: " + key + " -> " + entry.getValue());
                    }
                    Preference pref = findPreference(key);
                    if (pref == null) {
                        Log.e(LOG_TAG, "Unknown preference: " + key);
                        continue;
                    }
                    if (entry.getValue() == null) {
                        Log.i(LOG_TAG, "Preference value not set: " + key);
                        PreferenceUtils.resetToDefaultValue(pref);
                    } else if (pref instanceof GlucoseLevelPreference) {
                        ((GlucoseLevelPreference) pref).setRawValue((String)entry.getValue());
                    } else {
                        PreferenceUtils.setValue(pref, entry.getValue());
                    }
                }

                // TODO update prefs on current screen
                restartFragment();
//                PreferenceFragmentCompat fragment = getVisibleFragment();
//                fragment.getListView().get
//                if (fragment != null) {
//                    getSupportFragmentManager()
//                            .beginTransaction()
//                            .detach(fragment)
//                            .attach(fragment)
//                            .commitNow();
//                }

                UiUtils.showToast(this, R.string.cfg_import_success);
            } catch (IOException e) {
                throw e;
            }
        } catch (Throwable t) {
            Log.e(GWatchApplication.LOG_TAG, "Configuration import failed", t);
            UiUtils.showToast(this, R.string.cfg_import_failed);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // backgrounds

    private ActivityResultLauncher<String[]> bkgImageLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                bkgImageDesc = MediaUtils.checkImageAndGetName(this, uri);
                setSelectFilePreferenceSummary("ctrl_select_bkg_file", bkgImageDesc == null ? null : bkgImageDesc.getName());
            }
    );

    public void onSelectBackgroundFile(View view) {
        if (!UiUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXTERNAL_STORAGE_PERM_BKG)) {
            return;
        }
        bkgImageLauncher.launch(new String[] {MediaUtils.MIME_TYPE_IMAGE});
    }

    private ActivityResultLauncher<String[]> aodBkgImageLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                aodBkgImageDesc = MediaUtils.checkImageAndGetName(this, uri);
                setSelectFilePreferenceSummary("ctrl_aod_select_bkg_file", aodBkgImageDesc == null ? null : aodBkgImageDesc.getName());
            }
    );

    public void onSelectAodBackgroundFile(View view) {
        if (!UiUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXTERNAL_STORAGE_PERM_AOD_BKG)) {
            return;
        }
        aodBkgImageLauncher.launch(new String[] {MediaUtils.MIME_TYPE_IMAGE});
    }


    ///////////////////////////////////////////////////////////////////////////
    // alarm sounds

    private ActivityResultLauncher<String[]> criticalAlarmSndLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                criticalAlarmsSoundDesc = MediaUtils.checkAudioFileAndGetName(this, uri);
                setSelectFilePreferenceSummary("ctrl_alarms_sound_critical_select_file", criticalAlarmsSoundDesc == null ? null : criticalAlarmsSoundDesc.getName());
            }
    );

    public void onSelectCriticalAlarmSound(View view) {
        if (!UiUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_CRITICAL)) {
            return;
        }
        criticalAlarmSndLauncher.launch(MediaUtils.MIME_TYPES_AUDIO);
    }

    private ActivityResultLauncher<String[]> lowAlarmSndLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                lowAlarmsSoundDesc = MediaUtils.checkAudioFileAndGetName(this, uri);
                setSelectFilePreferenceSummary("ctrl_alarms_sound_lows_select_file", lowAlarmsSoundDesc == null ? null : lowAlarmsSoundDesc.getName());
            }
    );

    public void onSelectLowAlarmSound(View view) {
        if (!UiUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_LOW)) {
            return;
        }
        lowAlarmSndLauncher.launch(MediaUtils.MIME_TYPES_AUDIO);
    }

    private ActivityResultLauncher<String[]> highAlarmSndLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                highAlarmsSoundDesc = MediaUtils.checkAudioFileAndGetName(this, uri);
                setSelectFilePreferenceSummary("ctrl_alarms_sound_highs_select_file", highAlarmsSoundDesc == null ? null : highAlarmsSoundDesc.getName());
            }
    );

    public void onSelectHighAlarmSound(View view) {
        if (!UiUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_HIGH)) {
            return;
        }
        highAlarmSndLauncher.launch(MediaUtils.MIME_TYPES_AUDIO);
    }

    private ActivityResultLauncher<String[]> fastdropAlarmSndLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                fastdropAlarmsSoundDesc = MediaUtils.checkAudioFileAndGetName(this, uri);
                setSelectFilePreferenceSummary("ctrl_alarms_sound_fastdrop_select_file", fastdropAlarmsSoundDesc == null ? null : fastdropAlarmsSoundDesc.getName());
            }
    );

    public void onSelectFastdropAlarmSound(View view) {
        if (!UiUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_FASTDROP)) {
            return;
        }
        fastdropAlarmSndLauncher.launch(MediaUtils.MIME_TYPES_AUDIO);
    }

    private ActivityResultLauncher<String[]> nodataAlarmSndLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                nodataAlarmsSoundDesc = MediaUtils.checkAudioFileAndGetName(this, uri);
                setSelectFilePreferenceSummary("ctrl_alarms_sound_nodata_select_file", nodataAlarmsSoundDesc == null ? null : nodataAlarmsSoundDesc.getName());
            }
    );

    public void onSelectNodataAlarmSound(View view) {
        if (!UiUtils.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_CODE_EXT_STORAGE_PERM_SOUND_ALARMS_NODATA)) {
            return;
        }
        nodataAlarmSndLauncher.launch(MediaUtils.MIME_TYPES_AUDIO);
    }

    ///////////////////////////////////////////////////////////////////////////

    protected void restartIfLangChanged(String newLangCode, SharedPreferences sharedPreferences) {
        String currentLang = LangUtils.getCurrentLang(this);
        Log.d(LOG_TAG, "restartIfLangChanged: " + currentLang + " -> " + newLangCode);
        if (!currentLang.startsWith(newLangCode)) {
            sharedPreferences.edit().commit(); // commit here to be sure everything is really saved
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            Runtime.getRuntime().exit(0);
        }
    }

    protected void restartFragment() {
        // preference fragment cannot be refreshed
        // it must be replaced with new instance
        PreferenceFragmentCompat originalFragment = getVisibleFragment();

        // create new fragment instance
        FragmentManager fragmentManager = getSupportFragmentManager();
        PreferenceFragmentCompat fragment = (PreferenceFragmentCompat) fragmentManager
                .getFragmentFactory()
                .instantiate(getClassLoader(), originalFragment.getClass().getName());
        fragment.setArguments(originalFragment.getArguments());

        // replace the existing fragment with the new fragment instance
        fragmentManager.popBackStack();
        fragmentManager.beginTransaction()
                .replace(R.id.settings, fragment, originalFragment.getTag())
                .addToBackStack(originalFragment.getTag())
                .commit();
    }

    @Override
    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        Log.w(LOG_TAG, "received fragment result for requestCode=" + requestKey + ": " + result);
    }
}