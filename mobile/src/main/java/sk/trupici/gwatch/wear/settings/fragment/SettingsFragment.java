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

import com.rarepebble.colorpicker.ColorPreference;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import sk.trupici.gwatch.wear.settings.PreferenceMap;
import sk.trupici.gwatch.wear.settings.TimePreference;
import sk.trupici.gwatch.wear.settings.TimePreferenceDialog;
import sk.trupici.gwatch.wear.view.SettingsActivity;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

public abstract class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, FragmentResultListener {

    public static final String TMP_FRAGMENT_TAG = "HIDDEN";

    private static final String DIALOG_FRAGMENT_TAG = "CustomPreference";

    public static final String FRAGMENT_REQUEST_CODE = "SettingsFragment";

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // do not add any callback if fragment is temporary
        if (!TMP_FRAGMENT_TAG.equals(getTag())) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();

            ((SettingsActivity)getActivity()).setToolbarTitle(preferenceScreen.getTitle());

            int count = preferenceScreen.getPreferenceCount();
            for (int i=0; i < count; i++) {
                setOnClickCallback(preferenceScreen.getPreference(i));
            }
        }

        getParentFragmentManager().setFragmentResultListener(FRAGMENT_REQUEST_CODE, getViewLifecycleOwner(), this);
    }

    /**
     * Override default {@code Preference#mSingleLineTitle} value
     * to enable multi-line preference title in all preferences.
     *
     * Code taken from https://stackoverflow.com/questions/9220039/android-preferencescreen-title-in-two-lines
     */
    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null) {
            setMultiLineTitles(preferenceScreen);
        }
        super.setPreferenceScreen(preferenceScreen);
    }

    private void setMultiLineTitles(Preference preference) {
        preference.setSingleLineTitle(false);
        if (preference instanceof PreferenceGroup) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
            for (int i=0; i < preferenceGroup.getPreferenceCount(); i++) {
                setMultiLineTitles(preferenceGroup.getPreference(i));
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (!TMP_FRAGMENT_TAG.equals(getTag())) {
            initSummary(getPreferenceScreen());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // implement in subclasses
        return false;
    }

    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else if (preference instanceof TimePreference) {
                final DialogFragment f = TimePreferenceDialog.newInstance(preference.getKey());
                f.setTargetFragment(this, 0);
                f.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void setOnClickCallback(Preference preference) {
        if (preference instanceof PreferenceGroup) {
            int count = ((PreferenceGroup) preference).getPreferenceCount();
            for (int i=0; i < count; i++) {
                setOnClickCallback(((PreferenceGroup) preference).getPreference(i));
            }
        } else {
            if (preference instanceof TwoStatePreference /*&& !PreferenceMap.data.containsKey(preference.getKey()) */
                    || PreferenceMap.mappedOnClickPrefs.contains(preference.getKey())) {
                preference.setOnPreferenceClickListener(this);
            }
        }
    }

    protected void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            ((SettingsActivity)getActivity()).updatePrefSummary(p);
        }
    }

    protected void selectRadioButton(TwoStatePreference pref, String ... others) {
        pref.setChecked(true);
        for (String other : others) {
            checkPreference(other, false);
        }
    }

    protected void checkPreference(String key, boolean value) {
        TwoStatePreference tsp = (TwoStatePreference) findPreference(key);
        if (tsp != null) {
            tsp.setChecked(value);
        } else {
            // FIXME
            Log.e(LOG_TAG, "Could not change value for preference: " + key + " -> " + value);
        }
    }

    @Override
    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        // do nothing here now
        Log.w(LOG_TAG, "onFragmentResult() called with result: " + result);
    }
}
