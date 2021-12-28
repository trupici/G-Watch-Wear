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

package sk.trupici.gwatch.wear.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.stream.Collectors;

import androidx.preference.PreferenceManager;

public class PreferenceUtils {
    final private static String LOG_TAG = PreferenceUtils.class.getSimpleName();


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

    public static void setLongValue(Context context, String prefName, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(prefName, value).commit();
    }

    public static long getLongValue(Context context, String prefName, long defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(prefName, defValue);
    }

    public static int[] getIntArrayValue(Context context, String prefName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String serialized = prefs.getString(prefName, null);
        if (serialized == null) {
            Log.w(LOG_TAG, "getIntArrayValue: no data to restore");
            return null;
        }

        try {
            return Arrays.stream(serialized.split(":"))
                    .mapToInt(Integer::valueOf)
                    .toArray();
        } catch (Exception e) {
            Log.e(LOG_TAG, "getIntArrayValue: failed to restore data: " + e.getLocalizedMessage());
            return null;
        }
    }

    public static boolean setIntArrayValue(Context context, String prefName, int[] value) {
        try {
            String serialized = Arrays.stream(value)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining(":"));

            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            edit.putString(prefName, serialized);
            edit.commit();
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "getIntArrayValue: failed to store data: " + e.getLocalizedMessage());
            return false;
        }
    }
}
