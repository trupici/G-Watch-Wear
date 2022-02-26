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
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.util.CommonConstants;
import sk.trupici.gwatch.wear.view.MainActivity;

public class NotificationService extends Service {

    private static final String LOG_TAG = NotificationService.class.getSimpleName();

    public static final int NOTIFICATION_ID = 801415;

    private static final String CHANNEL_ID = "GWatchNotificationChannel";
    private static final int REQUEST_CODE = 2022;

    private static final String ACTION_START = "NotificationService.START";
    private static final String ACTION_BG_VALUE = "NotificationService.BG_VALUE";
    private static final String ACTION_TEXT = "NotificationService.TEXT";

    private static final String NO_DATA = "--";

    private static Notification currentNotification;

    public static void startService(Context context) {
        Intent startIntent = new Intent(context, NotificationService.class);
        startIntent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void updateBgData(Context context, GlucosePacket packet) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "updateBgPacket: " + packet.toText(context, null));
        }
        Intent startIntent = new Intent(context, NotificationService.class);
        startIntent.setAction(ACTION_BG_VALUE);
        startIntent.putExtra("data", packet.getData());
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void updateText(Context context, String text) {
        Intent startIntent = new Intent(context, NotificationService.class);
        startIntent.setAction(ACTION_TEXT);
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
            mNotificationManager.notify(NOTIFICATION_ID, createUpdateNotification(context, NO_DATA));
        } else if (ACTION_BG_VALUE.equals(intent.getAction())) {
            GlucosePacket packet = GlucosePacket.of(intent.getByteArrayExtra("data"));
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, createUpdateNotification(context, packet));
        } else if (ACTION_TEXT.equals(intent.getAction())) {
            String text = intent.getStringExtra("text");
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, createUpdateNotification(context, text == null || text.length() == 0 ? NO_DATA : text));
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static Notification createNotification(Context context, String text) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "createNotification: " + text);
        }

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
                currentNotification = createNotification(context, NO_DATA);
            }
            return currentNotification;
        }
    }

    public static Notification createUpdateNotification(Context context, GlucosePacket packet) {
        String text = NO_DATA;
        if (packet != null && packet.getGlucoseValue() != 0) {
            boolean isUnitConversion = PreferenceUtils.isConfigured(context, CommonConstants.PREF_IS_UNIT_CONVERSION, false);
            String source = StringUtils.notNullString(packet.getSource()).trim();
            text = (source.length() > 0 ? source + ": " : "")
                + BgUtils.formatBgValueString(packet.getGlucoseValue(), packet.getTrend(), isUnitConversion);
        } else {
            Log.w(LOG_TAG, "Invalid packet: " + (packet == null ? null : packet.toText(context, null)));
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
