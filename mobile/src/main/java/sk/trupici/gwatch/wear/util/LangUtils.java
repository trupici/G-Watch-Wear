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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;

import java.util.Locale;

import androidx.preference.PreferenceManager;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;

public class LangUtils {

    public static final String DEF_LANG = "system";

    @SuppressLint("ConstantLocale")
    private static final String systemDefault = Locale.getDefault().getLanguage();

    public static Context createLangContext(Context context) {
        String langCode = getSavedLang(context);
        if (DEF_LANG.equals(langCode)) {
            langCode = systemDefault;
        }

        return updateLocale(context, langCode);
    }

    public static Context updateLocale(Context context, String localeCode) {
        if (BuildConfig.DEBUG) {
            Log.w(GWatchApplication.LOG_TAG, "Setting locale: " + localeCode);
        }

        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        setLocale(config, locale);
        return context.createConfigurationContext(config);
    }

    public static String getCurrentLang(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } else{
            return getLocaleOld(context).getLanguage();
        }
    }

    public static String getSavedLang(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PreferenceUtils.PREF_LOCALE, DEF_LANG);
    }

    public static void setLocale(Configuration configuration, Locale locale) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            setLocaleConfigOld(configuration, locale);
        } else {
            configuration.setLocales(new LocaleList(locale));
        }
    }

    @SuppressWarnings("deprecation")
    private static void setLocaleConfigOld(Configuration configuration, Locale locale) {
        configuration.setLocale(locale);
    }

    @SuppressWarnings("deprecation")
    private static Locale getLocaleOld(Context context) {
        return context.getResources().getConfiguration().locale;
    }

    @SuppressWarnings("deprecation")
    private static Context updateConfigOld(Context context, Configuration config) {
        Resources resources = context.getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        return context;
    }
}