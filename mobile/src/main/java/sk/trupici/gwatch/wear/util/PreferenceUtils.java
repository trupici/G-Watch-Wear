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

package sk.trupici.gwatch.wear.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.rarepebble.colorpicker.ColorPreference;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.settings.GlucoseLevelPreference;
import sk.trupici.gwatch.wear.settings.TimePreference;
import sk.trupici.gwatch.wear.settings.ValuePreference;
import sk.trupici.gwatch.wear.settings.fragment.MainFragment;
import sk.trupici.gwatch.wear.settings.fragment.SettingsFragment;
import sk.trupici.gwatch.wear.view.SettingsActivity;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

public class PreferenceUtils {

    public static final String PREF_LOCALE = "pref_language";
    public static final String DEF_VALUE_LOCALE = "system";

    public static final String DUMMY_KEY_PREFIX = "dummy";

    public static void dumpPreferences(SharedPreferences prefs) {
        prefs.getAll().forEach((key, value) -> Log.d(LOG_TAG, "PREFS -> " + key + ": " + value));
    }

    public static boolean isConfigured(Context context, String prefName, boolean defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(prefName, defValue);
    }

    public static int getIntValue(Context context, String prefName, int defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(prefName, defValue);
    }

    public static int getStringValueAsInt(Context context, String prefName, int defValue) {
        return getStringValueAsInt(PreferenceManager.getDefaultSharedPreferences(context), prefName, defValue);
    }

    public static String getStringValue(Context context, String prefName, String defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(prefName, defValue);
    }

    public static void setStringValue(Context context, String prefName, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(prefName, value).commit();
    }

    public static int getStringValueAsInt(SharedPreferences prefs, String prefName, int defValue) {
        String strValue = prefs.getString(prefName, StringUtils.EMPTY_STRING);
        return strValue.trim().isEmpty() ? defValue : Integer.valueOf(strValue);
    }

    public static Object getDefaultValue(Preference pref) {
        try {
            Field field = Preference.class.getDeclaredField("mDefaultValue");
            field.setAccessible(true);
            return field.get(pref);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(LOG_TAG, pref.getClass().getSimpleName() + ": " + e.toString());
            return null;
        }
    }

    public static Object getValue(Preference preference) {
        if (preference instanceof GlucoseLevelPreference) {
            return ((GlucoseLevelPreference)preference).getRawValue();
        } else if (preference instanceof EditTextPreference) {
            return ((EditTextPreference) preference).getText();
        } else if (preference instanceof ColorPreference) {
            return ((ColorPreference) preference).getColor();
        } else if (preference instanceof TwoStatePreference) {
            return ((TwoStatePreference) preference).isChecked();
        } else if (preference instanceof ListPreference) {
            return ((ListPreference) preference).getValue();
        } else if (preference instanceof ValuePreference) {
            return ((ValuePreference) preference).getValue();
        } else if (preference instanceof TimePreference) {
            return ((TimePreference) preference).getTime();
        } else {
            String key = preference.getKey();
            if (key == null) {
                Log.e(LOG_TAG, "Cannot get value of " + preference.getClass().getSimpleName());
            } else if (!key.startsWith(PreferenceUtils.DUMMY_KEY_PREFIX)) {
                Log.e(LOG_TAG, "Cannot get value of " + key);
            }
        }
        return null;
    }

    public static boolean setValue(Preference preference, Object value) {
        if (preference == null) {
            Log.e(LOG_TAG, "Cannot set value " + value + " to null preference");
            return false;
        }
        String key = preference.getKey();
        if (key == null) {
            Log.e(LOG_TAG, "Cannot set value " + value + " to " + preference.getClass().getSimpleName());
            return false;
        } else if (key.startsWith(PreferenceUtils.DUMMY_KEY_PREFIX)) {
            // ignore dummy preferences
            return false;
        }

        if (preference instanceof EditTextPreference) {
            ((EditTextPreference) preference).setText((String) value);
            SharedPreferences.Editor editor = preference.getSharedPreferences().edit();
            editor.putString(key, (String) value);
            editor.apply();
        } else if (preference instanceof TwoStatePreference) {
            Boolean bValue = (value instanceof Boolean) ? (Boolean) value : Boolean.parseBoolean((String) value);
            ((TwoStatePreference) preference).setChecked(bValue);
            SharedPreferences.Editor editor = preference.getSharedPreferences().edit();
            editor.putBoolean(key, bValue);
            editor.apply();
        } else if (preference instanceof ColorPreference) {
            Integer color;
            if (value instanceof String) {
                try {
                    color = Color.parseColor((String)value);
                } catch (IllegalArgumentException e) {
                    color = Integer.valueOf((String) value);
                }
            } else {
                color = (Integer) value;
            }
            ((ColorPreference) preference).setColor(color);
            // value persisted in method above
        } else if (preference instanceof TimePreference) {
            ((TimePreference) preference).setTime((String) value);
            // value persisted in method above
        } else if (preference instanceof ListPreference) {
            ((ListPreference) preference).setValue((String) value);
        } else {
            Log.e(LOG_TAG, "Cannot set value " + value + " to " + key);
            return false;
        }
        return true;
    }

    public static boolean loadSharedValue(Preference pref, SharedPreferences sharedPrefs) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "loadSharedValue: " + pref.getKey());
        }
        if (pref instanceof GlucoseLevelPreference) {
            GlucoseLevelPreference typePref = (GlucoseLevelPreference) pref;
            typePref.setRawValue(sharedPrefs.getString(typePref.getKey(), typePref.getRawValue()));
        } else if (pref instanceof EditTextPreference) {
            EditTextPreference typePref = (EditTextPreference) pref;
            typePref.setText(sharedPrefs.getString(typePref.getKey(), typePref.getText()));
        } else if (pref instanceof ColorPreference) {
            ColorPreference typePref = (ColorPreference) pref;
            typePref.setColor(sharedPrefs.getInt(typePref.getKey(), typePref.getColor()));
        } else if (pref instanceof TwoStatePreference) {
            TwoStatePreference typePref = (TwoStatePreference) pref;
            typePref.setChecked(sharedPrefs.getBoolean(typePref.getKey(), typePref.isChecked()));
        } else if (pref instanceof ListPreference) {
            ListPreference typePref = (ListPreference) pref;
            typePref.setValue(sharedPrefs.getString(typePref.getKey(), typePref.getValue()));
        } else if (pref instanceof TimePreference) {
            TimePreference typePref = (TimePreference) pref;
            typePref.setTime(sharedPrefs.getString(typePref.getKey(), typePref.getTime()));
        } else {
            return false;
        }
        return true;
    }

    public static boolean resetToDefaultValue(Preference pref) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "resetToDefaultValue: " + pref.getKey());
        }
        return setValue(pref, getDefaultValue(pref));
    }

    public static Map<String, Preference> getAllInHierarchy(SettingsActivity activity, Preference pref, Map<String, Preference> prefMap) {
        if (pref instanceof PreferenceGroup) {
            PreferenceGroup prefGroup = (PreferenceGroup) pref;
            int count = prefGroup.getPreferenceCount();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    getAllInHierarchy(activity, prefGroup.getPreference(i), prefMap);
                }
            }
            if (pref.getFragment() != null) {
                //
                // this is the ugly thing of fragments hierarchy:
                // - fragment must be instantiated and populate its properties
                // - at the end it is removed from FragmentManager
                //
                PreferenceFragmentCompat f = (PreferenceFragmentCompat) activity
                        .getSupportFragmentManager()
                        .getFragmentFactory()
                        .instantiate(activity.getClassLoader(), pref.getFragment());
                if (f != null) {
                    f.setArguments(pref.getExtras());
                    activity
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.special, f, SettingsFragment.TMP_FRAGMENT_TAG)
                            .hide(f)
                            .disallowAddToBackStack()
                            .commitNow();
                    getAllInHierarchy(activity, f.getPreferenceScreen(), prefMap);
                    activity.getSupportFragmentManager().beginTransaction()
                            .detach(f)
                            .remove(f)
                            .commitNow();
                }
            }
        } else {
//            loadSharedValue(pref);
            prefMap.put(pref.getKey(), pref);
        }
        return prefMap;
    }

    public static Map<String, Preference> getAllPreferences(SettingsActivity activity) {
        Map<String, Preference> prefsMap = new HashMap<>();
        PreferenceFragmentCompat f = new MainFragment();
        activity
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.special, f, SettingsFragment.TMP_FRAGMENT_TAG)
                .hide(f)
                .disallowAddToBackStack()
                .commitNow();
        getAllInHierarchy(activity, f.getPreferenceScreen(), prefsMap);
        activity.getSupportFragmentManager().beginTransaction()
                .detach(f)
                .remove(f)
                .commitNow();
        Log.i(LOG_TAG, "Loaded preferences count: " + prefsMap.size());
        return prefsMap;
    }

    public static boolean setCustomPreference(Map<String, Preference> prefMap, String key, ValuePreference defPreference) {
        Preference pref = prefMap.remove(key);
        if (pref == null || defPreference == null) {
            return false;
        }
        prefMap.put(defPreference.getKey(), defPreference);
        return true;
    }

    public static void enableSecondHandTailCapOptions(PreferenceScreen preferenceScreen) {
        TwoStatePreference pref = (TwoStatePreference) preferenceScreen.findPreference("cfg_sec_hand_tail_negative");
        if (pref != null) {
            boolean enable = !pref.isChecked();
            preferenceScreen.findPreference("cfg_sec_hand_tail_width").setEnabled(enable);
            preferenceScreen.findPreference("cfg_sec_hand_cap").setEnabled(enable);
        }
    }

}
