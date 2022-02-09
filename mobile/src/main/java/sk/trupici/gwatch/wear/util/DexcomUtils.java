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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.Trend;

public class DexcomUtils {
    public static final String DEXCOM_PERMISSION = "com.dexcom.cgm.EXTERNAL_PERMISSION";

    private static final String DEXCOM_PACKAGE_PREFIX = "com.dexcom.";
    private static final String DEXCOM_FOLLOWER_PACKAGE_PREFIX = "com.dexcom.follow.";

    public static boolean checkAndRequestDexcomPermission(AppCompatActivity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // on devices with Android 10 and lower perform check whether Dexcom app is installed
            if (!checkIfDexcomInstalled()) {
                UiUtils.showToast(GWatchApplication.getAppContext(), R.string.dexcom_app_not_installed);
                return false;
            }
        }

        UiUtils.requestPermission(activity, DEXCOM_PERMISSION, requestCode);
        return true; // on devices with Android 11 and above, the result is unknown
    }

    public static boolean checkIfDexcomInstalled() {
        return getInstalledDexcomAppPackage() != null;
    }

    public static String getInstalledDexcomAppPackage() {
        try {
            PackageManager packageManager = GWatchApplication.getAppContext().getPackageManager();
            int disabledFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? getDisabledComponentsOld() : PackageManager.MATCH_DISABLED_COMPONENTS;
            for (PackageInfo packageInfo : packageManager.getInstalledPackages(disabledFlag)) {
                if (packageInfo.packageName.startsWith(DEXCOM_PACKAGE_PREFIX) && !packageInfo.packageName.startsWith(DEXCOM_FOLLOWER_PACKAGE_PREFIX)) {
                    return packageInfo.packageName;
                }
            }
        } catch (Exception e) {
            Log.e(GWatchApplication.LOG_TAG, e.toString(), e);
        }
        return null;
    }

    public static String getInstalledDexcomFollowAppPackage() {
        try {
            PackageManager packageManager = GWatchApplication.getAppContext().getPackageManager();
            int disabledFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? getDisabledComponentsOld() : PackageManager.MATCH_DISABLED_COMPONENTS;
            for (PackageInfo packageInfo : packageManager.getInstalledPackages(disabledFlag)) {
                if (packageInfo.packageName.startsWith(DEXCOM_FOLLOWER_PACKAGE_PREFIX)) {
                    return packageInfo.packageName;
                }
            }
        } catch (Exception e) {
            Log.e(GWatchApplication.LOG_TAG, e.toString(), e);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static int getDisabledComponentsOld() {
        //no inspection
        return PackageManager.GET_DISABLED_COMPONENTS;
    }


    /**
     * Translates DexCom trend value to G-Watch internal trend representation
     */
    public static Trend toTrend(String value) {
        if (value == null) {
            return null;
        } else if ("DoubleUp".equals(value)) {
            return Trend.UP_FAST;
        } else if ("SingleUp".equals(value)) {
            return Trend.UP;
        } else if ("FortyFiveUp".equals(value)) {
            return Trend.UP_SLOW;
        } else if ("Flat".equals(value)) {
            return Trend.FLAT;
        } else if ("FortyFiveDown".equals(value)) {
            return Trend.DOWN_SLOW;
        } else if ("SingleDown".equals(value)) {
            return Trend.DOWN;
        } else if ("DoubleDown".equals(value)) {
            return Trend.DOWN_FAST;
        } else {
            return Trend.UNKNOWN;
        }
    }

}
