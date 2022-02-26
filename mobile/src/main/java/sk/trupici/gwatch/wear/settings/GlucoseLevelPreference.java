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
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

public class GlucoseLevelPreference extends EditTextPreference {

    private static final Double MAX_THRESHOLD_MGDL_VALUE = 400d;
    private static final Double MIN_THRESHOLD_MGDL_VALUE = 30d;

    private String defaultValue;

    private Double minValue;
    private Double maxValue;

    // override mn/max values if there is linked limiting preference
    private String minPreferenceKey;
    private String maxPreferenceKey;

    private Integer validationMessageId;

    private String unitsPreferenceKey = "dummy";

    private boolean isDeltaValue = false;

    public GlucoseLevelPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        parseAndSetCustomAttributes(attrs);
    }

    public GlucoseLevelPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAndSetCustomAttributes(attrs);
    }

    public GlucoseLevelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAndSetCustomAttributes(attrs);
    }

    public GlucoseLevelPreference(Context context) {
        super(context);
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    private void parseAndSetCustomAttributes(AttributeSet attrs) {
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                String name = attrs.getAttributeName(i);
                if ("android:defaultValue".equalsIgnoreCase(name) || "defaultValue".equalsIgnoreCase(name)) {
                    defaultValue = attrs.getAttributeValue(i);
                } else if ("minValue".equalsIgnoreCase(name)) {
                    minValue = parseDoubleValue(attrs.getAttributeValue(i));
                } else if ("maxValue".equalsIgnoreCase(name)) {
                    maxValue = parseDoubleValue(attrs.getAttributeValue(i));
                } else if ("validationMessageID".equalsIgnoreCase(name)) {
                    validationMessageId = attrs.getAttributeIntValue(i, -1);
                } else if ("unitsPreferenceKey".equalsIgnoreCase(name)) {
                    unitsPreferenceKey = attrs.getAttributeValue(i);
                } else if ("minPreferenceKey".equalsIgnoreCase(name)) {
                    minPreferenceKey = attrs.getAttributeValue(i);
                } else if ("maxPreferenceKey".equalsIgnoreCase(name)) {
                    maxPreferenceKey = attrs.getAttributeValue(i);
                } else if ("isDeltaValue".equalsIgnoreCase(name)) {
                    isDeltaValue = Boolean.valueOf(attrs.getAttributeValue(i));
                }
            }
        }
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int errorMessageId = GlucoseLevelPreference.this.onValidate((String)newValue);
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

    @Override
    protected void onClick() {
        super.onClick();
    }

    @Override
    public CharSequence getTitle() {
        if (super.getTitle() == null) {
            return null;
        }
        String title = super.getTitle().toString();

        int idx = title.lastIndexOf(":");
        if (idx >= 0 && title.length() > idx + 1) {
            title = title.substring(0, idx + 1);
        }
        String value = getValueInUnits();
        if (value != null) {
            title += " " + value +  " " + (isUnitConversionSelected() ? "mmol/l" : "mg/dl");
        }
        return title;
    }

    private String getValueInUnits() {
        String value = super.getText();
        if (value == null || value.isEmpty()) {
            value = getDefaultValue();
        }

        if (isUnitConversionSelected()) {
            Double doubleValue = Double.parseDouble(value);
            value = String.valueOf(BgUtils.convertGlucoseToMmolL(doubleValue));
        }
        return value;
    }

    private String setValueInDefaultUnits(String value) {
        if (value == null || value.isEmpty()) {
            value = getDefaultValue();
        } else if (isUnitConversionSelected()) {
            Double doubleValue = Double.parseDouble(value);
            value = String.valueOf(BgUtils.convertGlucoseToMgDl(doubleValue).intValue());
        }
        super.setText(value);
        return value;
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
            Double value = Double.parseDouble(text);
            if (isUnitConversionSelected()) {
                value = BgUtils.convertGlucoseToMgDl(value);
            } else if (value.intValue() != value / 10d * 10d) {
                throw new ValidationException(R.string.gl_validation_err_no_decimals);
            }

            Double limit = getLowerLimit();
            if (limit != null && value < limit) {
                throw new ValidationException(R.string.gl_validation_err_value_too_small);
            }

            limit = getUpperLimit();
            if (limit != null && limit < value) {
                throw new ValidationException(R.string.gl_validation_err_value_too_big);
            }
            return 0;
        } catch (ValidationException e) {
            return e.getMessageId();
        } catch (Exception e) {
            if (validationMessageId != null && validationMessageId >= 0) {
                return validationMessageId;
            } else {
                return R.string.edit_preference_validation_error;
            }
        }
    }

    private Double getLimit(String preferenceKey, Double defValue) {
        Double limit = null;
        if (preferenceKey != null) {
            Preference pref = findPreferenceInHierarchy(preferenceKey);
            if (pref != null && pref instanceof GlucoseLevelPreference) {
                limit = parseDoubleValue(((GlucoseLevelPreference) pref).getRawValue());
            }
        }
        return (limit != null) ? limit : defValue;
    }

    private Double getUpperLimit() {
        Double limit = getLimit(maxPreferenceKey, maxValue);
        if (limit == null) {
            return null;
        }
        return maxValue == null ? limit : Math.min(limit, maxValue);
    }

    private Double getLowerLimit() {
        Double limit = getLimit(minPreferenceKey, minValue);
        if (limit == null) {
            return null;
        }
        return minValue == null ? limit : Math.max(limit, minValue);
    }

    private Double parseDoubleValue(String strValue) {
        try {
            return Double.parseDouble(strValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isUnitConversionSelected() {
        SharedPreferences sharedPrefs = getSharedPreferences();
        if (sharedPrefs != null && sharedPrefs.contains(unitsPreferenceKey)) {
            return sharedPrefs.getBoolean(unitsPreferenceKey, false);
        }
        return false;
    }

    public String getRawValue() {
        String strValue = super.getText();
        // this is a workaround for invalid thresholds issue
        Double doubleValue = parseDoubleValue(strValue);
        if (doubleValue == null) {
            return strValue;
        }
        // fix the mis-conversion
        if (doubleValue > MAX_THRESHOLD_MGDL_VALUE) {
            Log.e(GWatchApplication.LOG_TAG, "Invalid glucose threshold value: " + getKey() + ", performing downscale: " + strValue);
            return String.valueOf(BgUtils.convertGlucoseToMmolL(doubleValue).intValue());
        } else if (!isDeltaValue && doubleValue < MIN_THRESHOLD_MGDL_VALUE) {
            Log.e(GWatchApplication.LOG_TAG, "Invalid glucose threshold value: " + getKey() + ", performing upscale: " + strValue);
            return String.valueOf(BgUtils.convertGlucoseToMgDl(doubleValue).intValue());
        }
        return strValue;
    }

    public void setRawValue(String value) {
        super.setText(value);
    }

    @Override
    public String getText() {
        return getValueInUnits();
    }

    @Override
    public void setText(String text) {
        setValueInDefaultUnits(text);
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

    static class ValidationException extends RuntimeException {
        final private int messageId;

        public ValidationException(int messageId) {
            super(GWatchApplication.getAppContext().getResources().getString(messageId));
            this.messageId = messageId;
        }

        public int getMessageId() {
            return messageId;
        }
    }
}
