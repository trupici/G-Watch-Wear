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

package sk.trupici.gwatch.wear.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.BgData;
import sk.trupici.gwatch.wear.util.BgUtils;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.StringUtils;
import sk.trupici.gwatch.wear.view.MainActivity;

public class NotificationService extends Service {

    private static String LOG_TAG = NotificationService.class.getSimpleName();

    public static final int NOTIFICATION_ID = 801415;

    private static final String CHANNEL_ID = "GWatchNotificationChannel";
    private static final int REQUEST_CODE = 2022;

    private static final String ACTION_START = "NotificationService.START";
    private static final String ACTION_CONNECTION_STATUS = "NotificationService.CONNECTION_STATUS";
    private static final String ACTION_BG_VALUE = "NotificationService.BG_VALUE";
    private static final String ACTION_TEXT = "NotificationService.TEXT";

    private static Notification currentNotification;

    public static void startService(Context context) {
        Intent startIntent = new Intent(context, NotificationService.class);
        startIntent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void updateConnectionStatus(Context context, boolean isConnected) {
        Intent startIntent = new Intent(context, NotificationService.class);
        startIntent.setAction(ACTION_CONNECTION_STATUS);
        startIntent.putExtra("status", isConnected);
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void updateBgValue(Context context, BgData bgData) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "updateBgValue: " + bgData);
        }
        Intent startIntent = new Intent(context, NotificationService.class);
        startIntent.setAction(ACTION_BG_VALUE);
        startIntent.putExtra("bgValue", bgData.toBundle());
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void updateText(Context context, String text) {
        Intent startIntent = new Intent(context, NotificationService.class);
        startIntent.setAction(ACTION_BG_VALUE);
        startIntent.putExtra("text", text);
        ContextCompat.startForegroundService(context, startIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onStartCommand: " + intent);
        }

        Context context = GWatchApplication.getAppContext();
        if (ACTION_START.equals(intent.getAction())) {
            startForeground(NOTIFICATION_ID, getOrCreateNotification(context));
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, createUpdateNotification(context, StringUtils.NO_DATA_STR));
        } else if (ACTION_BG_VALUE.equals(intent.getAction())) {
            BgData bgData = BgData.fromBundle(intent.getBundleExtra("bgValue"));
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, createUpdateNotification(context, bgData));
        } else if (ACTION_TEXT.equals(intent.getAction())) {
            String text = intent.getStringExtra("text");
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, createUpdateNotification(context, text == null || text.length() == 0 ? StringUtils.NO_DATA_STR : text));
        } else if (ACTION_CONNECTION_STATUS.equals(intent.getAction())) {
            Boolean connStatus = intent.getBooleanExtra("status", false);
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, createUpdateNotification(context, connStatus));
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static Notification createNotification(Context context, String text) {

        Intent showTaskIntent = new Intent(context, MainActivity.class);
        showTaskIntent.setAction(Intent.ACTION_MAIN);
        showTaskIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        showTaskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                showTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_watch)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build();
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager.getNotificationChannel(CHANNEL_ID) != null) {
                return;
            }

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "G-Watch Wear Notification Channel",
                    NotificationManager.IMPORTANCE_MIN);
            serviceChannel.setShowBadge(false);
            serviceChannel.enableLights(false);
            serviceChannel.enableVibration(false);
            serviceChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public static Notification getOrCreateNotification(Context context) {
        synchronized (context) {
            if (currentNotification == null) {
                createNotificationChannel(context);
                currentNotification = createNotification(context, null);
            }
            return currentNotification;
        }
    }

    public static Notification createUpdateNotification(Context context, Boolean isConnected) {
        int connStatusId;
        if (isConnected == null) {
            connStatusId = R.string.conn_status_unknown;
        } else if (Boolean.TRUE.equals(isConnected)) {
            connStatusId = R.string.conn_status_connected;
        } else {
            connStatusId = R.string.conn_status_disconnected;
        }
        String text = context.getString(R.string.conn_notification_status_label, context.getText(connStatusId));

        synchronized (context) {
            currentNotification = createNotification(context, text);
            return currentNotification;
        }
    }

    public static Notification createUpdateNotification(Context context, BgData bgData) {
        String text = StringUtils.NO_DATA_STR;
        if (bgData != null) {
            boolean isUnitConversion = PreferenceUtils.isConfigured(context, CommonConstants.PREF_IS_UNIT_CONVERSION, false);
            text = BgUtils.formatBgValueString(bgData.getValue(), bgData.getTrend(), isUnitConversion)
//                    + BgUtils.formatBgDeltaForComplication(
//                    bgData.getValueDiff(),
//                    System.currentTimeMillis() - bgData.getTimestamp(),
//                    isUnitConversion,
//                    PreferenceUtils.getIntValue(context, CommonConstants.PREF_NO_DATA_THRESHOLD, 0))
            ;
        }
        synchronized (context) {
            currentNotification = createNotification(context, text);
            return currentNotification;
        }
    }

    public static Notification createUpdateNotification(Context context, String text) {
        synchronized (context) {
            currentNotification = createNotification(context, text);
            return currentNotification;
        }
    }

}
