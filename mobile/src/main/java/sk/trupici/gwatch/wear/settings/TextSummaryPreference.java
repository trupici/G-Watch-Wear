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
import android.util.AttributeSet;

import java.util.regex.Pattern;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

/**
 * Custom {@code EditTextPreference} showing current value in summary field
 */
public class TextSummaryPreference extends EditTextPreference {

    private boolean maskSummary;
    private Integer validationMessageId;
    private Pattern pattern;


    public TextSummaryPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        parseAndSetCustomAttributes(attrs);
    }

    public TextSummaryPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAndSetCustomAttributes(attrs);
    }

    public TextSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAndSetCustomAttributes(attrs);
    }

    public TextSummaryPreference(Context context) {
        super(context);
    }

    @Override
    public CharSequence getSummary() {
        String value = getText();
        if (value == null || value.length() == 0) {
            return super.getSummary();
        }
        return maskSummary ? StringUtils.populateString("*", value.length()) : value;
    }

    private void parseAndSetCustomAttributes(AttributeSet attrs) {
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                String name = attrs.getAttributeName(i);
                if ("maskSummary".equalsIgnoreCase(name)) {
                    maskSummary = Boolean.valueOf(attrs.getAttributeValue(i));
                } else if ("validationMessageId".equalsIgnoreCase(name)) {
                    validationMessageId = attrs.getAttributeIntValue(i, -1);
                } else if ("regexp".equalsIgnoreCase(name)) {
                    String regexp = attrs.getAttributeValue(i);
                    if (regexp != null && !regexp.isEmpty()) {
                        pattern = Pattern.compile(regexp);
                    }
                }
            }
        }
        if (pattern != null) {
            setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int errorMessageId = TextSummaryPreference.this.onValidate((String) newValue);
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
    }
    /**
     * Called to validate contents of the edit text.
     *
     * Return 0 to indicate success, or return a validation error message to display on the edit text.
     *
     * @param text The text to validate.
     * @return An error message key, or null if the value passes validation.
     */
    public int onValidate(String text) {
        try {
            if (text == null || text.length() == 0) {
                return 0;
            }
            if (!pattern.matcher(text).matches()) {
                throw new RuntimeException("Invalid content: " + text);
            }
            return 0;
        } catch (Exception e) {
            if (validationMessageId != null && validationMessageId >= 0) {
                return validationMessageId;
            } else {
                return R.string.text_preference_validation_error;
            }
        }
    }

}
