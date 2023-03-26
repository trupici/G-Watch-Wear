package sk.trupici.gwatch.wear.util;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

public class AidexUtils {
    public static final String AIDEX_PERMISSION = "com.dexcom.cgm.EXTERNAL_PERMISSION";

    private static final String AIDEX_PACKAGE_PREFIX = "com.microtechmd.cgms.";

    public static boolean checkAndRequestAppPermission(AppCompatActivity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // on devices with Android 10 and lower perform check whether Aidex app is installed
            if (!checkIfAppInstalled()) {
                UiUtils.showToast(GWatchApplication.getAppContext(), R.string.aidex_app_not_installed);
                return false;
            }
        }

        UiUtils.requestPermission(activity, AIDEX_PERMISSION, requestCode);
        return true; // on devices with Android 11 and above, the result is unknown
    }

    public static boolean checkIfAppInstalled() {
        return getInstalledAppPackage() != null;
    }

    public static String getInstalledAppPackage() {
        try {
            PackageManager packageManager = GWatchApplication.getAppContext().getPackageManager();
            int disabledFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? getDisabledComponentsOld() : PackageManager.MATCH_DISABLED_COMPONENTS;
            for (PackageInfo packageInfo : packageManager.getInstalledPackages(disabledFlag)) {
                if (packageInfo.packageName.startsWith(AIDEX_PACKAGE_PREFIX)) {
                    Log.d(LOG_TAG, "Found AiDEX package: " + packageInfo.packageName);
                    return packageInfo.packageName;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static int getDisabledComponentsOld() {
        //no inspection
        return PackageManager.GET_DISABLED_COMPONENTS;
    }

}
