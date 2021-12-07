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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.console.PacketConsole;

public class UiUtils {

    public static final Double GLUCOSE_CONV_FACTOR = 18.018018;
    public static final String GLUCOSE_UNITS_MGDL = "mg/dl";
    public static final String GLUCOSE_UNITS_MMOLL = "mmol/l";

    public static final String NO_DATA_STR = "-";

    public static void showToast(final Context context, final int msgId) {
        Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show();
    }

    public static boolean requestPermission(AppCompatActivity activity, String permission, int permRequestId) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // No explanation; request the permission
            // permRequestId is an app-defined int constant. The callback method gets the result of the request.
            ActivityCompat.requestPermissions(activity, new String[]{permission}, permRequestId);
            return false;
        } else {
            // Permission has already been granted
            return true;
        }
    }

    public static String formatDateTime(Date date) {
        return java.text.DateFormat.getDateTimeInstance().format(date);
    }

    public static String formatTime(Date date) {
        return java.text.DateFormat.getTimeInstance().format(date);
    }

    public static String formatTimeOrNoData(long timestamp) {
        return timestamp == 0 ? NO_DATA_STR : java.text.DateFormat.getTimeInstance().format(new Date(timestamp));
    }

    public static String formatDoubleOrNoData(Double value) {
        return (value == null || value == -1) ? UiUtils.NO_DATA_STR : String.format("%.2f", value);
    }

    public static String getStringOrNoData(String str) {
        return (str == null) ? UiUtils.NO_DATA_STR : str;
    }

    public static Double convertGlucoseToMmolL(double glucoseValue) {
        return Math.round(glucoseValue / GLUCOSE_CONV_FACTOR * 10d) / 10d;
    }

    public static Double convertGlucoseToMmolL2(double glucoseValue) {
        return Math.round(glucoseValue / GLUCOSE_CONV_FACTOR * 100d) / 100d;
    }

    public static Double convertGlucoseToMgDl(double glucoseValue) {
        return (double) Math.round(glucoseValue * GLUCOSE_CONV_FACTOR);
    }

    private static char getDefaultDecimalSeparator() {
        return ((DecimalFormat)DecimalFormat.getInstance(Locale.getDefault())).getDecimalFormatSymbols().getDecimalSeparator();
    }

    public static String convertGlucoseToMmolLStr(double glucoseValue) {
        char decimalSeparator = getDefaultDecimalSeparator();
        return String.valueOf(convertGlucoseToMmolL(glucoseValue)).replace('.', decimalSeparator);
    }

    public static String convertGlucoseToMmolL2Str(double glucoseValue) {
        char decimalSeparator = getDefaultDecimalSeparator();
        double value = convertGlucoseToMmolL2(glucoseValue);
        return String.valueOf(value).replace('.', decimalSeparator);
    }

    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(runnable);
        } else {
            runnable.run();
        }
    }

    public static boolean showAlertDialog(final Context context, final String message, final String title) {
        if (context == null) {
            return false;
        }
        try {
            runOnUiThread(() -> {
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setIcon(android.R.drawable.ic_dialog_alert)

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            });
            return true;
        } catch (Throwable t) {
            Log.e(GWatchApplication.LOG_TAG, "showAlertDialog: ", t);
            return false;
        }
    }

    public static String getGlucoseUnitsStr(boolean isUnitConv) {
        return isUnitConv ? GLUCOSE_UNITS_MMOLL : GLUCOSE_UNITS_MGDL;
    }

    public static boolean isHighDensityDisplay(Context context) {
        return context.getResources().getDisplayMetrics().density > 2f;
    }

    public static void showMessage(Context context, String message) {
        PacketConsole packetConsole = GWatchApplication.getPacketConsole();
        if (packetConsole != null) {
            packetConsole.showText(message);
        }
    }

}
