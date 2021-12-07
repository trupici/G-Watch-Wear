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

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import sk.trupici.gwatch.wear.R;

/**
 * Time picker dialog preference using androidx support library.
 *
 * Base on @Dirk answer on this stack overflow thread:
 * https://stackoverflow.com/questions/38902661/android-time-preference-dialog-with-support-library
 */
public class TimePreference extends DialogPreference {
    private String time;

    public TimePreference(Context context) {
        this(context, null);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceStyle);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setPositiveButtonText(R.string.action_ok);
        setNegativeButtonText(R.string.action_cancel);
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;

        // save to SharedPreference
        persistString(time);

        setTitle(getTitle());
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.pref_time_picker_dialog;
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        // load from SharedPreference if available or set default value
        setTime(getPersistedString((String)defaultValue));
    }

    public static int getHour(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid time value: " + time, e);
        }
    }

    public static int getMinute(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid time value: " + time);
        }
    }

    @Override
    public CharSequence getTitle() {
        if (super.getTitle() == null) {
            return null;
        }
        String title = super.getTitle().toString();

        int idx = title.indexOf(":");
        if (idx < 0) {
            return title;
        }
        if (idx >= 0 && title.length() > idx + 1) {
            title = title.substring(0, idx + 1);
        }

        if (time != null) {
            String strTime;
            if (DateFormat.is24HourFormat(getContext())) {
                strTime = time;
            } else {
                int hour = getHour(time);
                int min = getMinute(time);
                String ampm;
                if (hour >= 12) {
                    ampm = "pm";
                    hour -=12;
                } else {
                    ampm = "am";
                }
                if (hour == 0) {
                    hour = 12;
                }
                strTime = String.format("%d:%02d %s", hour, min, ampm);
            }

            title += " " + strTime;
        }
        return title;
    }

}
