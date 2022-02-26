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
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

/**
 * Custom preference for numbers with min and max properties.
 */
public class IntegerPreference extends EditTextPreference {

    private String defaultValue;

    private Integer minValue;
    private Integer maxValue;
    private String units;

    private Integer validationMessageId;

    public IntegerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        parseAndSetCustomAttributes(attrs);
    }

    public IntegerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAndSetCustomAttributes(attrs);
    }

    public IntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAndSetCustomAttributes(attrs);
    }

    public IntegerPreference(Context context) {
        super(context);
    }


    @Override
    public CharSequence getTitle() {
        if (super.getTitle() == null) {
            return null;
        }
        String title = super.getTitle().toString();

        int idx = title.lastIndexOf(":");
        if (idx < 0) {
            return title;
        }
        if (idx >= 0 && title.length() > idx + 1) {
            title = title.substring(0, idx + 1);
        }

        String value = getText();
        if (value == null || value.isEmpty()) {
            value = defaultValue;
        }

        if (value != null) {
            title += " " + value + (units == null ? StringUtils.EMPTY_STRING : units);
        }
        return title;
    }

    private void parseAndSetCustomAttributes(AttributeSet attrs) {
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                String name = attrs.getAttributeName(i);
                if ("android:defaultValue".equalsIgnoreCase(name) || "defaultValue".equalsIgnoreCase(name)) {
                    defaultValue = attrs.getAttributeValue(i);
                } else if ("minValue".equalsIgnoreCase(name)) {
                    minValue = parseValue(attrs.getAttributeValue(i));
                } else if ("maxValue".equalsIgnoreCase(name)) {
                    maxValue = parseValue(attrs.getAttributeValue(i));
                } else if ("validationMessageID".equalsIgnoreCase(name)) {
                    validationMessageId = attrs.getAttributeIntValue(i, -1);
                } else if ("units".equalsIgnoreCase(name)) {
                    units = attrs.getAttributeValue(i);
                }
            }
        }
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int errorMessageId = IntegerPreference.this.onValidate((String)newValue);
                if (errorMessageId == 0) {
                    notifyChanged();
                    return true;
                } else {
                    UiUtils.showToast(getContext(), errorMessageId);
                    return false;
                }
            }
        });
    }

    /**
      * Called to validate contents of the edit text.
      *
      * Return null to indicate success, or return a validation error message to display on the edit text.
      *
      * @param text The text to validate.
      * @return An error message key, or null if the value passes validation.
      */
    public int onValidate(String text) {
        try {
            Integer value = Integer.parseInt(text);

            if (minValue != null && value < minValue) {
                throw new RuntimeException("Too small value");
            }
            if (maxValue != null && value > maxValue) {
                throw new RuntimeException("Too big value");
            }
            return 0;
        } catch (Exception e) {
            if (validationMessageId != null && validationMessageId >= 0) {
                return validationMessageId;
            } else {
                return R.string.edit_preference_validation_error;
            }
        }
    }

    private Integer parseValue(String strValue) {
        try {
            return Integer.parseInt(strValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        super.setText(getPersistedString((String)defaultValue));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        super.setDefaultValue(defaultValue);
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getText() {
        String value = super.getText();
        if (value == null || value.isEmpty()) {
            value = getDefaultValue();
        }
        return value;
    }
}
