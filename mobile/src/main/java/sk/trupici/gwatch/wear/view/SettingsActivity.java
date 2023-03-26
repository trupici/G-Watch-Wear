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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;

import java.io.FileInputStream;
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
import sk.trupici.gwatch.wear.common.data.ConfigPacket;
import sk.trupici.gwatch.wear.common.data.Packet;
import sk.trupici.gwatch.wear.common.data.TLV;
import sk.trupici.gwatch.wear.common.util.PacketUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.followers.DexcomShareFollowerService;
import sk.trupici.gwatch.wear.followers.FollowerService;
import sk.trupici.gwatch.wear.followers.LibreLinkUpFollowerService;
import sk.trupici.gwatch.wear.followers.NightScoutFollowerService;
import sk.trupici.gwatch.wear.settings.ConfigData;
import sk.trupici.gwatch.wear.settings.ConfigType;
import sk.trupici.gwatch.wear.settings.GlucoseLevelPreference;
import sk.trupici.gwatch.wear.settings.PreferenceMap;
import sk.trupici.gwatch.wear.settings.TimePreference;
import sk.trupici.gwatch.wear.settings.ValuePreference;
import sk.trupici.gwatch.wear.settings.fragment.MainFragment;
import sk.trupici.gwatch.wear.util.LangUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.widget.WidgetUpdateService;

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

    public static final int REQUEST_CODE_DEXCOM_PERMISSION = 10;
    public static final int REQUEST_CODE_AIDEX_PERMISSION = 20;

    private static final String CONFIG_FILE_NAME = "gwatch-wear-settings.xml";
    private static final String CONFIG_MEDIA_TYPE = "application/xml";

    protected Map<String, Preference> changedPrefs = new HashMap<>();

    protected View.OnTouchListener gestureListener;
    protected GestureDetector gestureDetector;

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

        toolbar.setNavigationOnClickListener(v -> finish(false));

        toolbar.setOnMenuItemClickListener(item -> {
            try {
                PreferenceFragmentCompat fragment = getVisibleFragment();
                if (fragment == null) {
                    Log.e(GWatchApplication.LOG_TAG, "Could not find fragment: " );//+ FRAGMENT_TAG);
                    return false;
                }
                PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
                int numPreferences = preferenceScreen.getPreferenceCount();
                int id = item.getItemId();
                if (id == R.id.action_reset_all) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.settings_reset_all_message)
                            .setPositiveButton(R.string.action_yes, (dialogInterface, pos) -> {
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
                                    applyValuesToWatch(allPrefs);
                                    // TODO
                                } catch (Throwable t) {
                                    Log.e(LOG_TAG, "Configuration reset failed", t);
                                }
                            })
                            .setNegativeButton(R.string.action_no, (dialog, pos) -> dialog.cancel())
                            .show();
                    return true;
                } else if (id == R.id.action_apply_all) {
                    Map<String, Preference> allPrefs = new HashMap<>();
                    for (int i = 0; i < numPreferences; i++) {
                        Preference preference = preferenceScreen.getPreference(i);
                        PreferenceUtils.getAllInHierarchy(SettingsActivity.this, preference, allPrefs);
                    }
                    for (String key : allPrefs.keySet()) {
                        changedPrefs.remove(key);
                    }
                    applyValuesToWatch(allPrefs);
                    return true;
                } else if (id == R.id.action_apply) {
                    applyValuesToWatch(changedPrefs);
                    changedPrefs.clear();
                    return true;
                } else if (id == R.id.action_export) {
                    selectExportFilePath();
                    return true;
                } else if (id == R.id.action_import) {
                    selectImportFilePath();
                    return true;
                }
            } catch (Throwable t) {
                // catch all exception to close menu item
                Log.e(LOG_TAG, "Configuration failed", t);
            }
            return false;
        });

        gestureDetector = new GestureDetector(this, new HorizontalSwipeDetector(this));
        gestureListener = (v, event) -> gestureDetector.onTouchEvent(event);
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
        if (!changedPrefs.isEmpty()) {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.settings_save_changes_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.action_yes, (dialogInterface, i) -> {
                        applyValuesToWatch(changedPrefs);
                        changedPrefs.clear();
                        doFinish(anim);
                    })
                    .setNegativeButton(R.string.action_no, (dialogInterface, i) -> {
                        changedPrefs.clear();
                        doFinish(anim);
                    })
                    .show();
        } else {
            doFinish(anim);
        }
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
    public View onCreateView(View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        if (parent != null) {
            parent.setOnTouchListener(gestureListener);
        }
        return super.onCreateView(parent, name, context, attrs);
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
    ///////////////////////////////////////////////////////////////////////////

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

        if (PreferenceMap.data.containsKey(key)) { // direct mapping
            addPreference(changedPrefs, pref);
        } else { // custom mapping / advanced handling
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Pref changed: " + pref.getKey() + ": " + (pref instanceof TwoStatePreference ? ((TwoStatePreference) pref).isChecked() : "N/A"));
            }
            if (key.startsWith("pref_widget")) {
                // fake preference value to indicate widget config change
                addPreference(changedPrefs, new ValuePreference(this, "pref_widget_changed", (byte)1));
            } else if ("pref_data_source_nightscout_enable".equals(key)) {
                // fake preference value to indicate nightscout follower config change
                addPreference(changedPrefs, new ValuePreference(this, "pref_data_source_nightscout_enable", (byte)1));
            } else if (key.contains("pref_data_source_dexcom_share_enable")) {
                // fake preference value to indicate dexcom share follower config change
                addPreference(changedPrefs, new ValuePreference(this, "pref_data_source_dexcom_share_enable", (byte)1));
            } else if (key.contains("pref_data_source_librelinkup_enable")) {
                // fake preference value to indicate librelinkup follower config change
                addPreference(changedPrefs, new ValuePreference(this, "pref_data_source_librelinkup_enable", (byte)1));
            } else if (PreferenceUtils.PREF_LOCALE.equals(key)) {
                restartIfLangChanged(((ListPreference) pref).getValue(), sharedPreferences);
            } else {
                changedPrefs.put(pref.getKey(), pref);
            }
        }
        // transitive handling
        if ("cfg_glucose_units_conversion".equals(key)) { // force widget redraw on units change
            addPreference(changedPrefs, new ValuePreference(this, "pref_widget_changed", (byte) 1));
        }
    }

    private void addPreference(Map<String, Preference> prefs, Preference pref) {
        if (pref != null) {
            prefs.put(pref.getKey(), pref);
        }
    }

    public void updatePrefSummary(Preference pref) {
        PreferenceFragmentCompat visibleFragment = getVisibleFragment();
        if ("cfg_glucose_units_conversion".equals(pref.getKey()) && visibleFragment != null) {
            RecyclerView view = visibleFragment.getListView();
            if (view != null && view.getAdapter() != null) {
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
            String value = listPref.getEntry() != null ? listPref.getEntry().toString() : StringUtils.EMPTY_STRING;
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

    protected void applyValuesToWatch(final Map<String, Preference> changedPrefsMap) {
        try {
            if (createAndSendConfiguration(changedPrefsMap.values())) {
                // nothing to do yet
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Send configuration failed", t);
            UiUtils.showToast(this, R.string.cfg_transfer_failed);
        }
    }

    protected boolean createAndSendConfiguration(Collection<Preference> changedPrefs) {
        Log.i(GWatchApplication.LOG_TAG, "createAndSendConfiguration: " + changedPrefs.size());

        boolean result = false;
        List<TLV> config = new ArrayList<>(changedPrefs.size());
        int totalLen = 0;
        boolean widgetPrefsUpdated = false;
        boolean nightscoutPrefsUpdated = false;
        boolean dexcomSharePrefsUpdated = false;
        boolean libreLinkUpPrefsUpdated = false;

        for (Preference pref : changedPrefs) {
            if (BuildConfig.DEBUG) {
                Log.d(GWatchApplication.LOG_TAG, "Changed pref: " + pref.getKey() + " -> " + PreferenceUtils.getValue(pref));
            }
            if (pref.getKey() == null) {
                Log.w(LOG_TAG, "Preference key is null!");
                continue;
            }

            if (pref.getKey().contains("nightscout")) {
                nightscoutPrefsUpdated = true;
            } else if (pref.getKey().contains("dexcom_share")) {
                dexcomSharePrefsUpdated = true;
            } else if (pref.getKey().contains("librelinkup")) {
                libreLinkUpPrefsUpdated = true;
            }

            ConfigData mapping = PreferenceMap.data.get(pref.getKey());
            if (mapping != null) { // direct cfg mapping
                Integer value = null;
                String strValue = null;
                Boolean boolValue = null;
                if (pref instanceof ValuePreference) {
                    value = ((ValuePreference) pref).getValue();
                } else if (pref instanceof TimePreference) {
                    String time = (String)PreferenceUtils.getValue(pref);
                    value = 60 * TimePreference.getHour(time);
                    value += TimePreference.getMinute(time);
                } else {
                    Object tmpValue = PreferenceUtils.getValue(pref);
                    if (tmpValue instanceof String) {
                        if (mapping.getType() == ConfigType.STRING) {
                            strValue = (String) tmpValue;
                        } else {
                            value = Integer.valueOf((String) tmpValue);
                        }
                    } else if (tmpValue instanceof Boolean) {
                        boolValue = (Boolean)tmpValue;
                    } else {
                        value = (Integer) tmpValue;
                    }
                }
                if (value == null && strValue == null && boolValue == null && mapping.getType() != ConfigType.STRING) {
                    Log.w(LOG_TAG, "Null preference value: " + pref.getKey());
                    continue;
                }

                byte[] data;
                if (mapping.getType() == ConfigType.BYTE) {
                    data = new byte[1];
                    data[0] = value.byteValue();
                    if (BuildConfig.DEBUG) {
                        Log.d(GWatchApplication.LOG_TAG,"Send BYTE: " + pref.getKey() + ": " + value);
                    }
                } else if (mapping.getType() == ConfigType.WORD) {
                    data = new byte[2];
                    PacketUtils.encodeShort(data, 0, value.shortValue());
                    if (BuildConfig.DEBUG) {
                        Log.d(GWatchApplication.LOG_TAG,"Send WORD: " + pref.getKey() + ": " + value);
                    }
                } else if (mapping.getType() == ConfigType.DWORD) {
                    data = new byte[4];
                    PacketUtils.encodeInt(data, 0, value);
                    if (BuildConfig.DEBUG) {
                        Log.d(GWatchApplication.LOG_TAG,"Send DWORD: " + pref.getKey() + ": " + value);
                    }
                } else if (mapping.getType() == ConfigType.COLOR) {
                    data = new byte[4];
                    PacketUtils.encodeInt(data, 0, value);
                    if (BuildConfig.DEBUG) {
                        Log.d(GWatchApplication.LOG_TAG,"Send COLOR: " + pref.getKey() + ": " + value);
                    }
                } else if (mapping.getType() == ConfigType.BOOLEAN) {
                    data = new byte[1];
                    PacketUtils.encodeBoolean(data, 0, boolValue);
                    if (BuildConfig.DEBUG) {
                        Log.d(GWatchApplication.LOG_TAG,"Send BOOLEAN: " + pref.getKey() + ": " + boolValue);
                    }
                } else if (mapping.getType() == ConfigType.STRING) {
                    int len = PacketUtils.getNullableStrLen(strValue);
                    data = new byte[len];
                    PacketUtils.encodeString(data, 0, strValue);
                    if (BuildConfig.DEBUG) {
                        Log.d(GWatchApplication.LOG_TAG,"Send STRING: " + pref.getKey() + ": "
                                + strValue);
                    }
                } else {
                    Log.w(LOG_TAG, "Skipping preference: " + pref.getKey());
                    continue;
                }
                TLV tlv = new TLV(mapping.getTag(), (byte)data.length, data);
                config.add(tlv);
                totalLen += tlv.getTotalLen();
            } else { // indirect cfg mapping - try to apply dedicated cfg logic
                if (pref.getKey().startsWith("pref_widget_")) {
                    widgetPrefsUpdated = true;
                }
            }
        }

        if (totalLen > 0) {
            Packet packet = new ConfigPacket(config, totalLen);
            if (GWatchApplication.getDispatcher().dispatch(packet)) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "Config Packet sent: " + config.size() + " in " + totalLen + " bytes");
                }
//                UiUtils.showToast(this, R.string.cfg_transfer_ok);
                result = true;
            } else {
                UiUtils.showToast(this, R.string.cfg_transfer_failed);
                result = false;
            }
        } else if (changedPrefs.size() > 0) {
            UiUtils.showToast(this, R.string.cfg_applied);
            result = true;
        }

        if (widgetPrefsUpdated) {
            // crete empty configuration packet just for widget notification
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Widget update request due to config change");
            }
            WidgetUpdateService.updateWidget(new ConfigPacket(Collections.emptyList(), 0));
        }
        if (nightscoutPrefsUpdated) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Nightscout follower update request due to config change");
            }
            FollowerService.reloadSettings(getApplicationContext(), NightScoutFollowerService.class);
        }
        if (dexcomSharePrefsUpdated) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "Dexcom share follower update request due to config change");
            }
            FollowerService.reloadSettings(getApplicationContext(), DexcomShareFollowerService.class);
        }
        if (libreLinkUpPrefsUpdated) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "LibreLinkUp follower update request due to config change");
            }
            FollowerService.reloadSettings(getApplicationContext(), LibreLinkUpFollowerService.class);
        }
        return result;
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

    private final ActivityResultLauncher<String> exportFileLauncher = registerForActivityResult(
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


    private final ActivityResultLauncher<String[]> importFileLauncher = registerForActivityResult(
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
            }
        } catch (Throwable t) {
            Log.e(GWatchApplication.LOG_TAG, "Configuration import failed", t);
            UiUtils.showToast(this, R.string.cfg_import_failed);
        }
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