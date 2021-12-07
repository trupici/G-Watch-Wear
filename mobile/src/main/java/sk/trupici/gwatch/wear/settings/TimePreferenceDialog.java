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

package sk.trupici.gwatch.wear.settings;

import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import androidx.annotation.RequiresApi;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
import sk.trupici.gwatch.wear.R;

/**
 * Time picker preference dialog using androidx support library.
 *
 * Base on @Dirk answer on this stack overflow thread:
 * https://stackoverflow.com/questions/38902661/android-time-preference-dialog-with-support-library
 */
public class TimePreferenceDialog extends PreferenceDialogFragmentCompat {

    private static final String TIME_PATTERN = "%02d:%02d";

    private TimePicker timePicker;

    public static TimePreferenceDialog newInstance(String key) {
        final TimePreferenceDialog fragment = new TimePreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        timePicker = view.findViewById(R.id.time_picker);
        if (timePicker == null) {
            throw new IllegalStateException("Dialog view must contain a TimePicker with id 'time_picker'");
        }

        String time = null;
        DialogPreference preference = getPreference();
        if (preference instanceof sk.trupici.gwatch.wear.settings.TimePreference) {
            time = ((sk.trupici.gwatch.wear.settings.TimePreference) preference).getTime();
        }

        // Set the time to the TimePicker
        timePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        if (time != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                setTimeOld(timePicker, time);
            } else {
                setTime(timePicker, time);
            }
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            // Generate value to save
            String time = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? getTimeOld(timePicker, TIME_PATTERN) : getTime(timePicker, TIME_PATTERN);

            DialogPreference preference = getPreference();
            if (preference instanceof sk.trupici.gwatch.wear.settings.TimePreference) {
                sk.trupici.gwatch.wear.settings.TimePreference timePreference = ((sk.trupici.gwatch.wear.settings.TimePreference) preference);
                if (timePreference.callChangeListener(time)) {
                    timePreference.setTime(time);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setTimeOld(TimePicker timePicker, String time) {
        timePicker.setCurrentHour(sk.trupici.gwatch.wear.settings.TimePreference.getHour(time));
        timePicker.setCurrentMinute(sk.trupici.gwatch.wear.settings.TimePreference.getMinute(time));
    }

    @SuppressWarnings("deprecation")
    private String getTimeOld(TimePicker timePicker, String pattern) {
        int hour = timePicker.getCurrentHour();
        int minute = timePicker.getCurrentMinute();
        return String.format(pattern, hour, minute);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void setTime(TimePicker timePicker, String time) {
        timePicker.setHour(sk.trupici.gwatch.wear.settings.TimePreference.getHour(time));
        timePicker.setMinute(sk.trupici.gwatch.wear.settings.TimePreference.getMinute(time));
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private String getTime(TimePicker timePicker, String pattern) {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        return String.format(pattern, hour, minute);
    }
}
