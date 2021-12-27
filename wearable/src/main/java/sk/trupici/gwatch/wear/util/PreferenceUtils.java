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

import androidx.preference.PreferenceManager;

public class PreferenceUtils {
    final private static String LOG_TAG = DumpUtils.class.getSimpleName();


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
}
