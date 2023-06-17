package sk.trupici.gwatch.wear.util;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

public class AndroidUtils {

    public static int getMutableFlag(boolean mutable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return 0;
        } else {
            return getMutableFlagValue(mutable);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @SuppressLint("WrongConstant")
    private static int getMutableFlagValue(boolean  mutable) {
        return mutable ? PendingIntent.FLAG_MUTABLE : PendingIntent.FLAG_IMMUTABLE;
    }

    @SuppressWarnings("deprecation")
    public static Object getBundleObject(Bundle bundle, String key) {
        return bundle.get(key);
    }
}
