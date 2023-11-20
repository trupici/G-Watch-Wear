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

package sk.trupici.gwatch.wear.followers;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.util.CommonConstants;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.receivers.AlarmReceiver;
import sk.trupici.gwatch.wear.util.AndroidUtils;
import sk.trupici.gwatch.wear.util.HttpUtils;
import sk.trupici.gwatch.wear.util.UiUtils;
import sk.trupici.gwatch.wear.view.MainActivity;

public abstract class FollowerService extends Worker {

    public static final String EXTRA_TIMESTAMP = "FollowerService.EXTRA_TIMESTAMP";

    private static final String CHANNEL_ID = "GWatchFollowerNotificationChannel";
    private static final int REQUEST_CODE = 2024;
    public static final int NOTIFICATION_ID = 76801415;

    protected static final long DEF_SAMPLE_PERIOD_MS = 300000; // 5min
    protected static final long MISSED_SAMPLE_PERIOD_MS = 60000; // 1min
    protected static final long DEF_SAMPLE_LATENCY_MS = 15000; // 15s

    private static OkHttpClient httpClient;
    private static Long lastSampleTime;


    abstract protected boolean isServiceEnabled(Context context);
    abstract protected List<GlucosePacket> getServerValues(Context context);
    abstract protected String getServiceLabel();

    public FollowerService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {

        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, getClass().getSimpleName() + ": doWork");
        }
        Context context = GWatchApplication.getAppContext();

        if (!isServiceEnabled(context)) {
            return Result.success();
        }
        if (getLastSampleTime() == null) {
            initLastSampleTime();
        }

        final long processingTime = getInputData().getLong(EXTRA_TIMESTAMP, System.currentTimeMillis());

        GlucosePacket lastPacket = null;
        try {
            List<GlucosePacket> packets = getServerValues(context);
            if (packets != null) {
                lastPacket = (packets.size() > 0) ? packets.get(0) : null;

                String entries = context.getResources().getQuantityString(R.plurals.entries, packets.size());
                UiUtils.showMessage(context,
                        context.getString(
                                R.string.follower_entries_received,
                                packets.size(),
                                entries,
                                StringUtils.formatTime(new Date())));

                if (lastPacket != null) {
                    if (lastPacket.getTimestamp() != 0) {
                        setLastSampleTime(lastPacket.getTimestamp());
                    }

                    for (int i = packets.size() - 1; i >= 0; i--) {
                        GWatchApplication.getDispatcher().dispatch(packets.get(i));
                    }
                }
            }
        } catch (TooManyRequestsException e) {
            // process reschedule
            String retryAfter = e.getRetryAfter();
            Log.w(LOG_TAG, "Request rejected with HTTP 429, Retry-After: " + retryAfter);
            long nextRequestDelay = 0;
            if (retryAfter == null || retryAfter.length() == 0) {
                nextRequestDelay = getSamplePeriodMs();
            } else {
                try {
                    nextRequestDelay = Integer.parseInt(retryAfter) * CommonConstants.SECOND_IN_MILLIS;
                } catch (Exception ex) {
                    Log.e(LOG_TAG, "Failed to parse Retry-After value: " + retryAfter, ex);
                }
                if (nextRequestDelay <= 0) {
                    nextRequestDelay = getSamplePeriodMs();
                }
            }
            scheduleNewRequest(context, nextRequestDelay);
            return Result.success();
        } catch (Throwable t) {
            Log.e(LOG_TAG, t.getLocalizedMessage(), t);
        }
        scheduleNewRequest(context, getNextRequestDelay(lastPacket, processingTime));

        return Result.success();
    }

    @NonNull
    @Override
    public ForegroundInfo getForegroundInfo() {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "getForegroundInfo");
        }

        Context context = GWatchApplication.getAppContext();

        createNotificationChannel(context);
        Notification notification = createNotification(context, context.getString(R.string.follower_request_notification, getServiceLabel()));

        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? new ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                : new ForegroundInfo(NOTIFICATION_ID, notification);
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
                PendingIntent.FLAG_UPDATE_CURRENT | AndroidUtils.getMutableFlag(true));

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_watch)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setSilent(true)
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
                    "G-Watch Wear Follower Notification Channel",
                    NotificationManager.IMPORTANCE_LOW);
            serviceChannel.setShowBadge(false);
            serviceChannel.enableLights(false);
            serviceChannel.enableVibration(false);
            serviceChannel.setSound(null, null);
            serviceChannel.setImportance(NotificationManager.IMPORTANCE_LOW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                serviceChannel.setAllowBubbles(false);
            }
            manager.createNotificationChannel(serviceChannel);
        }
    }


    protected static void reset() {
        httpClient = null;
        lastSampleTime = null;
    }

    protected void init() {
        FollowerService.reset();
        initLastSampleTime();
    }

    protected void initLastSampleTime() {
        setLastSampleTime(System.currentTimeMillis() - getSamplePeriodMs() - getSampleToRequestDelay());
    }

    protected void scheduleNewRequest(Context context, long delayMs) {
        AlarmReceiver.scheduleNextAlarm(context, delayMs);
    }

    protected boolean useExplicitSslTrust(Context context) {
        return false;
    }

    /**
     * Round-trip latency from sample time to request in milliseconds
     */
    protected long getSampleToRequestDelay() {
        return DEF_SAMPLE_LATENCY_MS;
    }

    /**
     * Returns a period for server requests to get a new BG value.
     * @see #DEF_SAMPLE_PERIOD_MS
     */
    protected long getSamplePeriodMs() {
        return DEF_SAMPLE_PERIOD_MS;
    }

    /**
     * Returns a period for the next server requests when missed sample is detected,
     * or 0 if no special handling for missed sample is required.
     * @see #MISSED_SAMPLE_PERIOD_MS
     */
    protected long getMissedSamplePeriodMs() {
        return MISSED_SAMPLE_PERIOD_MS;
    }

    /**
     * Returns delay in ms for scheduling next request to NS server
     * @param packet last received glucose packet
     * @param processingTime timestamp of this run of processing loop
     * @return delay in milliseconds (from now) when to request next value from NS server
     */
    private long getNextRequestDelay(GlucosePacket packet, long processingTime) {
        Long sampleTime = (packet != null) ? Long.valueOf(packet.getTimestamp()) : getLastSampleTime();
        if (sampleTime != null && sampleTime < processingTime) {
            // received sample with valid timestamp
            if (processingTime - sampleTime > getSamplePeriodMs() + getSampleToRequestDelay()) {
                // sample is old - use fast sampling to get the next value
                // if sample is not too old and fast sampling is configured
                if (getMissedSamplePeriodMs() > 0 && processingTime - sampleTime < 2 * getSamplePeriodMs() + getSampleToRequestDelay()) {
                    return getMissedSamplePeriodMs() - (System.currentTimeMillis() - processingTime);
                }
            }
        }
        // use default period
        return getSamplePeriodMs() - (System.currentTimeMillis() - processingTime);
    }
    /**
     * Returns a {@code OkHttpClient} instance to use for NS server requests.
     * A new {@code OkHttpClient} is created if no instance is available.
     */
    protected OkHttpClient getHttpClient(Context context) {
        OkHttpClient httpClient = FollowerService.httpClient;
        if (httpClient != null) {
            return httpClient;
        }

        OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS);
        if (useExplicitSslTrust(context)) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{HttpUtils.trustAllCertManager}, new java.security.SecureRandom());
                builder = builder.hostnameVerifier(HttpUtils.trustAllhostnameVerifier)
                        .sslSocketFactory(sslContext.getSocketFactory(), HttpUtils.trustAllCertManager);
            } catch (GeneralSecurityException e) {
                Log.e(LOG_TAG, getClass().getSimpleName() + " failed to create SSLSocketFactory", e);
            }
        }
        httpClient = builder.build();
        setHttpClient(httpClient);
        return httpClient;
    }

    public void setHttpClient(OkHttpClient httpClient) {
        FollowerService.httpClient = httpClient;
    }

    public Long getLastSampleTime() {
        return lastSampleTime;
    }

    public void setLastSampleTime(Long lastSampleTime) {
        FollowerService.lastSampleTime = lastSampleTime;
    }

    protected String getResponseBodyAsString(Response response) throws IOException  {
        ResponseBody body = response.body();
        if (body != null) {
            String value = body.string();
            try {
                body.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, getClass().getSimpleName() + " failed to close response body", e);
            }
            return value;
        }
        return null;
    }

    public static void startService(Context context, Class<? extends FollowerService> cls) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, cls.getSimpleName() + ": start request");
        }

        scheduleFollowerWork(context, 0, cls);
    }

    public static void reloadSettings(Context context, Class<? extends FollowerService> cls) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, cls.getSimpleName() + ": reload settings request");
        }

        try {
            cls.getDeclaredMethod("reset").invoke(null);
        } catch (Exception e) {
            Log.d(LOG_TAG, cls.getSimpleName() + ": initialization failed: " + e.getLocalizedMessage());
        }

        scheduleFollowerWork(context, 0, cls);
    }

    public static void scheduleFollowerWork(Context context, long processingTime, Class<? extends FollowerService> cls) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, cls.getSimpleName() + ": schedule request");
        }
        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(cls)
                        .setConstraints(new Constraints.Builder()
                                .setTriggerContentMaxDelay(Duration.ofMillis(100))
                                .build()
                        ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
        if (processingTime > 0) {
            builder.setInputData(new Data.Builder().putLong(FollowerService.EXTRA_TIMESTAMP, processingTime).build());
        }

        WorkManager workManager = WorkManager.getInstance(context);
        workManager.enqueue(builder.build());
    }
}
