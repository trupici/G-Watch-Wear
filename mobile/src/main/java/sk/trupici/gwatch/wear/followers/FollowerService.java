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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.receivers.AlarmReceiver;
import sk.trupici.gwatch.wear.service.NotificationService;
import sk.trupici.gwatch.wear.util.AlarmUtils;
import sk.trupici.gwatch.wear.util.HttpUtils;
import sk.trupici.gwatch.wear.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;

public abstract class FollowerService extends Service {

    private static final String WAKE_LOCK_TAG = "gwatch.wear:" + FollowerService.class.getSimpleName() + ".wake_lock";
    private static final long WAKE_LOCK_TIMEOUT_MS = 60000; // 1 min

    public static final String ACTION_START = "FollowerService.START";
    public static final String ACTION_REQUEST_NEW = "FollowerService.REQUEST_NEW";
    public static final String ACTION_RELOAD_SETTINGS = "FollowerService.RELOAD_SETTINGS";

    protected static final long DEF_SAMPLE_PERIOD_MS = 300000; // 5min
    protected static final long MISSED_SAMPLE_PERIOD_MS = 60000; // 1min
    protected static final long DEF_SAMPLE_LATENCY_MS = 15000; // 15s

    private static OkHttpClient httpClient;
    private static Long lastSampleTime;
    private static String sessionData;

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    abstract protected boolean isServiceEnabled(Context context);
    abstract protected List<GlucosePacket> getServerValues(Context context);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, getClass().getSimpleName() + ": onStartCommand: " + intent);
        }
        Context context = GWatchApplication.getAppContext();

        startForeground(NotificationService.NOTIFICATION_ID, NotificationService.getOrCreateNotification(context));

        if (ACTION_START.equals(intent.getAction())) {
            init();
        } else if (ACTION_RELOAD_SETTINGS.equals(intent.getAction())) {
            init();
            if (!isServiceEnabled(context)) {
                stopSelf();
                return START_NOT_STICKY;
            }
        } else { // ACTION_REQUEST_NEW
            Bundle alarmBundle = intent.getExtras();
            AlarmUtils.evaluateSchedule(alarmBundle);
            if (getLastSampleTime() == null) {
                initLastSampleTime();
            }
        }

        executor.execute(() -> {
            PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

            try {
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
                } catch (Throwable t) {
                    Log.e(LOG_TAG, t.getLocalizedMessage(), t);
                }
                scheduleNewRequest(context, getNextRequestDelay(lastPacket));
            } finally {
                wakeLock.release();
            }
        });

        return START_NOT_STICKY;
    }

    protected void init() {
        httpClient = null;
        sessionData = null;
        initLastSampleTime();
    }

    private void initLastSampleTime() {
        setLastSampleTime(System.currentTimeMillis() - DEF_SAMPLE_PERIOD_MS - getSampleToRequestDelay());
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
     * Returns delay in ms for scheduling next request to NS server
     * @param packet last received glucose packet
     * @return delay in milliseconds (from now) when to request next value from NS server
     */
    private long getNextRequestDelay(GlucosePacket packet) {
        long now = System.currentTimeMillis();
        Long sampleTime = (packet != null) ? Long.valueOf(packet.getTimestamp()) : getLastSampleTime();
        if (sampleTime == null || sampleTime > now) { // no valid info about last sample
            return DEF_SAMPLE_PERIOD_MS;
        } else if (sampleTime < now - 2 * DEF_SAMPLE_PERIOD_MS) { // sample older than fast polling period
            return DEF_SAMPLE_PERIOD_MS;
        } else if (sampleTime < now - DEF_SAMPLE_PERIOD_MS - getSampleToRequestDelay()) { // sample missed -> fast polling
            return MISSED_SAMPLE_PERIOD_MS;
        } else { // fresh sample received - schedule next regular request
            return sampleTime + DEF_SAMPLE_PERIOD_MS + getSampleToRequestDelay() - now;
        }
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

    public String getSessionData() {
        return sessionData;
    }

    public void setSessionData(String sessionData) {
        FollowerService.sessionData = sessionData;
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
        Intent startIntent = new Intent(context, cls);
        startIntent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void requestNewValue(Context context, Intent intent, Class<? extends FollowerService> cls) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, cls.getSimpleName() + ": Requesting new value");
        }
        Intent startIntent = new Intent(context, cls);
        startIntent.setAction(ACTION_REQUEST_NEW);
        startIntent.putExtras(intent);
        ContextCompat.startForegroundService(context, startIntent);
    }

    public static void reloadSettings(Context context, Class<? extends FollowerService> cls) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, cls.getSimpleName() + ": reload settings request");
        }
        Intent startIntent = new Intent(context, cls);
        startIntent.setAction(ACTION_RELOAD_SETTINGS);
        ContextCompat.startForegroundService(context, startIntent);
    }
}
