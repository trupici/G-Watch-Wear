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

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.Locale;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;

public class LangUtils {

    public static final String DEF_LANG = "system";


    public static boolean updateLocale(Context context, String localeCode) {
        if (BuildConfig.DEBUG) {
            Log.w(GWatchApplication.LOG_TAG, "Setting locale: " + localeCode);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager.class).setApplicationLocales(new LocaleList(Locale.forLanguageTag(localeCode)));
            return false;
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeCode));
            return true;
        }
    }

    public static LocaleList getPreferredLocaleList(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.getSystemService(LocaleManager.class).getApplicationLocales();
        } else {
            LocaleListCompat compat = AppCompatDelegate.getApplicationLocales();
            return LocaleList.forLanguageTags(compat.toLanguageTags());
        }
    }

    public static String getCurrentLang(Context context) {
        LocaleList localeList = getPreferredLocaleList(context);
        return localeList.isEmpty() ? Locale.getDefault().getLanguage() : localeList.get(0).getLanguage();
    }

    public static void syncLangPreference(Context context) {
        LocaleList localeList = getPreferredLocaleList(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String langCode;
        if (localeList.isEmpty()) {
            langCode = DEF_LANG;
        } else {
            langCode = localeList.get(0).getLanguage();
            String[] supportedLangs = context.getResources().getStringArray(R.array.language_codes);
            if (supportedLangs == null || !Arrays.asList(supportedLangs).contains(langCode)) {
                langCode = DEF_LANG;
            }
        }
        prefs.edit().putString(PreferenceUtils.PREF_LANG, langCode).apply();
    }
}