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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import kotlin.text.Charsets;
import okhttp3.Request;
import okhttp3.Response;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.data.Trend;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.util.HttpUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

/**
 * NightScout Cloud Follower Service
 */
public class NightScoutFollowerService extends FollowerService {

    private static final String SRC_LABEL_SHORT = "NS";
    private static final String SRC_LABEL = "NightScout";

    private static final int DEF_NS_SAMPLE_LATENCY_MS = 15;
    private static final int DEF_NS_SAMPLE_FAST_PERIOD_MS = 60000;
    private static final int DEF_NS_MISSED_SAMPLE_FAST_PERIOD_MS = 0; // disable missed sample feature

    public static final String PREF_NS_ENABLED = "pref_data_source_nightscout_enable";
    private static final String PREF_NS_URL = "cfg_nightscout_url";
    private static final String PREF_NS_API_SECRET = "cfg_nightscout_secret";
    private static final String PREF_NS_EXPLICIT_TRUST = "cfg_nightscout_explicit_trust";
    private static final String PREF_NS_TOKEN = "cfg_nightscout_token";
    private static final String PREF_NS_REQUEST_LATENCY = "cfg_nightscout_latency";
    private static final String PREF_NS_FAST_SAMPLE_PERIOD = "cfg_nightscout_fast_period";

    private static String apiSecret;
    private static String nsToken;
    private static String serverUrl;

    private static long sampleToRequestDelay = DEF_NS_SAMPLE_LATENCY_MS * 1000L;
    private static boolean isFastSamplingEnabled = false;

    public NightScoutFollowerService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected static void reset() {
        FollowerService.reset();
        Context context = GWatchApplication.getAppContext();
        nsToken = getNsToken(context);
        apiSecret = getApiSecret(context);
        serverUrl = getServerUrl(context);
        sampleToRequestDelay = PreferenceUtils.getStringValueAsInt(context, PREF_NS_REQUEST_LATENCY, DEF_NS_SAMPLE_LATENCY_MS) * 1000L;
        isFastSamplingEnabled = PreferenceUtils.isConfigured(context, PREF_NS_FAST_SAMPLE_PERIOD, false);
    }

    @Override
    protected void init() {
        super.init();
        NightScoutFollowerService.reset();
    }

    @Override
    protected boolean isServiceEnabled(Context context) {
        return PreferenceUtils.isConfigured(context, PREF_NS_ENABLED, false);
    }

    @Override
    protected boolean useExplicitSslTrust(Context context) {
        return PreferenceUtils.isConfigured(context, PREF_NS_EXPLICIT_TRUST, false);
    }

    @Override
    protected long getSampleToRequestDelay() {
        return sampleToRequestDelay;
    }

    @Override
    protected long getSamplePeriodMs() {
        return isFastSamplingEnabled ? DEF_NS_SAMPLE_FAST_PERIOD_MS : super.getSamplePeriodMs();
    }

    @Override
    protected long getMissedSamplePeriodMs() {
        return isFastSamplingEnabled ? DEF_NS_MISSED_SAMPLE_FAST_PERIOD_MS : super.getMissedSamplePeriodMs();
    }

    @Override
    protected List<GlucosePacket> getServerValues(Context context) {
        Request.Builder builder = new Request.Builder();
        try {
            if (serverUrl == null) {
                init();
                return null;
            }

            UiUtils.showMessage(context, context.getString(R.string.follower_data_request, SRC_LABEL));
            builder = builder.url(serverUrl);
            if (apiSecret != null) {
                builder = builder.addHeader("api-secret", apiSecret);
            }
            if (getLastSampleTime() != null) {
                builder.addHeader("If-Modified-Since", HttpUtils.formatHttpDate(getModifiedSince()));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", e);
            UiUtils.showMessage(context, e.getLocalizedMessage());
            return null;
        }

        try (Response response = getHttpClient(context).newCall(builder.build()).execute()) {
            if (response.isSuccessful()) {
//                setLastSampleTime(HttpUtils.parseHttpDate(response.header("Last-Modified")));
                String receivedData = getResponseBodyAsString(response);
                Log.i(GWatchApplication.LOG_TAG, "NightScout data received: " + receivedData);
                return parseValues(receivedData);
            } else if (response.code() == 304) {
                return parseValues(null);
            } else {
                throw new CommunicationException("HTTP " + response.code() + " - " + response.message());
            }
        } catch (SSLHandshakeException e) {
            Log.e(LOG_TAG, "SSL failed. Use 'Explicit certificate trust' option", e);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, e.getLocalizedMessage()));
        } catch (Throwable t) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", t);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, t.getLocalizedMessage()));
        }
        return null;
    }

    @Override
    protected String getServiceLabel() {
        return SRC_LABEL;
    }

    List<GlucosePacket> parseValues(String nsValue) {
        List<GlucosePacket> packets = new ArrayList<>();
        if (nsValue == null) {
            return packets;
        }

        GlucosePacket packet = parseNSValue(nsValue);
        if (packet != null) {
            packets.add(packet);
        }
        return packets;
    }

    /**
     * Parses NS short glucose value and returns its {@code GlucosePacket} representation on success,
     * otherwise it returns null
     */
    private GlucosePacket parseNSValue(String nsValue) {

        String[] parts = nsValue.split("\t");
        if (parts.length < 4) {
            return null;
        }

        Double glucoseValue = Double.valueOf(parts[2]);
        Long timestamp = Long.valueOf(parts[1]);
        String trend = parts[3];

        if (BuildConfig.DEBUG) {
            Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
            Log.w(GWatchApplication.LOG_TAG, "Trend: " + trend);
            Log.w(GWatchApplication.LOG_TAG, "Timestanp: " + new Date(timestamp));
        }

        short glucose = (short)Math.round(glucoseValue);
        return new GlucosePacket(glucose, timestamp, (byte)0, toTrend(trend), trend, SRC_LABEL_SHORT);
    }

    /**
     * Get date for If-Modified-Since http header
     */
    private Date getModifiedSince() {
        long since = getLastSampleTime() != null
                ? getLastSampleTime()
                : System.currentTimeMillis() - getSamplePeriodMs() - getSampleToRequestDelay();
        return new Date(since);
    }

    /**
     * Returns hashed NS api secret value to be sent in http header in requests to NS server
     * or null if no NS API secret was configured
     */
    private static String getApiSecret(Context context) {
        try {
            String plain = PreferenceUtils.getStringValue(context, PREF_NS_API_SECRET, null);
            if (plain == null) {
                return null;
            }
            return StringUtils.toHexString(MessageDigest.getInstance("SHA-1").digest(plain.trim().getBytes(Charsets.UTF_8)));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to secure NS secret", e);
            return null;
        }
    }

    /**
     * Returns NS server URL string or null if no NS url was configured
     */
    private static String getServerUrl(Context context) {
        String url = PreferenceUtils.getStringValue(context, PREF_NS_URL, null);
        if (url == null) {
            return null;
        } else if (url.endsWith("/")) {
            url = url.substring(0, url.length()-1);
        }
        if (!url.endsWith("api/v1")) {
            url += "/api/v1";
        }
        url += "/entries/current";

        if (nsToken != null) {
            url += "?token=" + nsToken;
        }
        return url;
    }

    private static String getNsToken(Context context) {
        String token = PreferenceUtils.getStringValue(context, PREF_NS_TOKEN, null);
        return token != null ? token.trim() : null;

    }

    /**
     * Translates NS trend value to G-Watch internal trend representation
     */
    private static Trend toTrend(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if ("DoubleUp".equals(value)) {
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
