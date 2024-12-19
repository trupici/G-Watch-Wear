/*
 * Copyright (C) 2022 Juraj Antal
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.data.Trend;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;


/**
 * LibreLinkUp Cloud Follower Service
 *
 * Description taken from here: https://github.com/timoschlueter/nightscout-librelink-up
 */
public class LibreLinkUpFollowerService extends FollowerService {

    private static final String SRC_LABEL_SHORT = "LLU";
    private static final String SRC_LABEL = "LibreLinkUp";

    public static final String PREF_LLU_ENABLED = "pref_data_source_librelinkup_enable";
    private static final String PREF_LLU_USERNAME = "cfg_librelinkup_account";
    private static final String PREF_LLU_PASSWORD = "cfg_librelinkup_password";
    private static final String PREF_LLU_REQUEST_LATENCY = "cfg_librelinkup_latency";

    private static final String LLU_SERVER_URL = "https://api.libreview.io";
    private static final String LLU_SERVER_URL_PATTERN = "https://api-%s.libreview.io";

    public static final String USER_AGENT = "LibreLinkUp/4.7.0 CFNetwork/711.2.23 Darwin/14.0.0";

    private static final int DEF_LLU_SAMPLE_LATENCY_MS = 15;
    private static final int DEF_LLU_SAMPLE_PERIOD_MS = 60000;
    private static final int DEF_LLU_MISSED_SAMPLE_PERIOD_MS = 0; // disable missed sample feature


    private static long sampleToRequestDelay = DEF_LLU_SAMPLE_LATENCY_MS;
    private static String serverUrl;
    private static AuthResult authResult;

    private static String connectionId;

    public LibreLinkUpFollowerService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected static void reset() {
        FollowerService.reset();
        connectionId = null;
        authResult = null;
        serverUrl = null;
        sampleToRequestDelay = PreferenceUtils.getStringValueAsInt(GWatchApplication.getAppContext(), PREF_LLU_REQUEST_LATENCY, DEF_LLU_SAMPLE_LATENCY_MS) * 1000L;
    }

    @Override
    public void init() {
        super.init();
        LibreLinkUpFollowerService.reset();
    }

    @Override
    protected void initLastSampleTime() {
        setLastSampleTime(0L);
    }

    @Override
    protected boolean isServiceEnabled(Context context) {
        return PreferenceUtils.isConfigured(context, PREF_LLU_ENABLED, false);
    }

    @Override
    protected long getSampleToRequestDelay() {
        return sampleToRequestDelay;
    }

    @Override
    protected long getSamplePeriodMs() {
        return DEF_LLU_SAMPLE_PERIOD_MS;
    }

    @Override
    protected long getMissedSamplePeriodMs() {
        return DEF_LLU_MISSED_SAMPLE_PERIOD_MS;
    }

    @Override
    protected boolean useExplicitSslTrust(Context context) {
        return true;
    }

    @Override
    protected List<GlucosePacket> getServerValues(Context context) {
        if (authResult == null) {
            authResult = authenticate(context);
        }
        if (authResult != null) {
            if (connectionId == null) {
                connectionId = getConnectionId(context);
            }
            if (connectionId != null) {
                List<GlucosePacket> packets = getBgData(context);
                if (packets != null) {
                    return packets;
                }
            }
        }
        // something went wrong...
        init();
        return null;
    }

    @Override
    protected String getServiceLabel() {
        return SRC_LABEL;
    }

    private static class AuthResult {
        public final String token;
        public final String accountId;

        public AuthResult(String token, String accountId) {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            byte[] digest = md.digest(accountId.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            this.token = token;
            this.accountId = sb.toString();
        }
    }

    private Request.Builder createRequestBuilder() {
        return new Request.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("product", "llu.ios")
                .addHeader("version", "4.12.0")
                .addHeader("Accept", "application/json")
                .addHeader("Pragma", "no-cache")
                ;
    }

    private AuthResult authenticate(Context context) {
        Request request;
        try {
            String url = getServerUrl() + "/auth/login";

            UiUtils.showMessage(context, context.getString(R.string.follower_auth_request, SRC_LABEL));
            String username = getProperty(context, PREF_LLU_USERNAME, "Invalid Username");
            String password = getProperty(context, PREF_LLU_PASSWORD, "Invalid password");
            if (username == null || password == null) {
                return null;
            }

            JSONObject json = new JSONObject();
            json.put("email", username);
            json.put("password", password);

            request = createRequestBuilder()
                    .url(url)
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();
        } catch (Exception e) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", e);
            UiUtils.showMessage(context, e.getLocalizedMessage());
            return null;
        }

        try (Response response = getHttpClient(context).newCall(request).execute()) {
            if (response.isSuccessful()) {
                String receivedData = getResponseBodyAsString(response);
                Log.i(GWatchApplication.LOG_TAG, "LibreLinkUp data received: " + receivedData);
                AuthResult authResult = extractToken(receivedData);
                if (authResult == null) {
                    if (parseRedirect(receivedData)) {
                        return authenticate(context);
                    }
                }
                if (authResult != null) {
                    UiUtils.showMessage(context, context.getString(R.string.status_ok));
                    return authResult;
                }
                Log.e(LOG_TAG, getClass().getSimpleName() + " failed");
                UiUtils.showMessage(context, context.getString(R.string.status_failed));
            } else if (response.code() == 429) {
                String retryAfter = response.header("Retry-After");
                throw new TooManyRequestsException(retryAfter, "HTTP " + response.code() + " - Retry-After: " + retryAfter);
            } else {
                throw new CommunicationException("HTTP " + response.code() + " - " + response.message());
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", t);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, t.getLocalizedMessage()));
            if (t instanceof TooManyRequestsException) {
                throw (TooManyRequestsException) t;
            }
        }
        return null;
    }

    private String getConnectionId(Context context) {
        Request request;
        try {
            String url = getServerUrl() + "/llu/connections";

            UiUtils.showMessage(context, context.getString(R.string.follower_session_request, SRC_LABEL));

            request = createRequestBuilder()
                    .addHeader("Authorization", "Bearer " + authResult.token)
                    .addHeader("Account-Id", authResult.accountId)
                    .url(url)
                    .build();
        } catch (Exception e) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", e);
            UiUtils.showMessage(context, e.getLocalizedMessage());
            return null;
        }

        try (Response response = getHttpClient(context).newCall(request).execute()) {
            if (response.isSuccessful()) {
                String receivedData = getResponseBodyAsString(response);
                Log.i(GWatchApplication.LOG_TAG, "LibreLinkUp data received: " + receivedData);
                String connectionId = extractPatientId(receivedData);
                if (connectionId == null) {
                    Log.e(LOG_TAG, getClass().getSimpleName() + " failed");
                    UiUtils.showMessage(context, context.getString(R.string.status_failed));
                } else {
                    UiUtils.showMessage(context, context.getString(R.string.status_ok));
                    return connectionId;
                }
            } else if (response.code() == 429) {
                String retryAfter = response.header("Retry-After");
                throw new TooManyRequestsException(retryAfter, "HTTP " + response.code() + " - Retry-After: " + retryAfter);
            } else {
                throw new CommunicationException("HTTP " + response.code() + " - " + response.message());
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", t);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, t.getLocalizedMessage()));
            if (t instanceof TooManyRequestsException) {
                throw (TooManyRequestsException) t;
            }
        }
        return null;
    }


    private List<GlucosePacket> getBgData(Context context) {
        Request request;

        try {
            String url = getServerUrl() + "/llu/connections/" + connectionId + "/graph";

            UiUtils.showMessage(context, context.getString(R.string.follower_data_request, SRC_LABEL));

            request = createRequestBuilder()
                    .addHeader("Authorization", "Bearer " + authResult.token)
                    .addHeader("Account-Id", authResult.accountId)
                    .url(url)
                    .build();
        } catch (Exception e) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", e);
            UiUtils.showMessage(context, e.getLocalizedMessage());
            return null;
        }

        try (Response response = getHttpClient(context).newCall(request).execute()) {
            if (response.isSuccessful()) {
                String receivedData = getResponseBodyAsString(response);
                if (BuildConfig.DEBUG) {
                    Log.i(GWatchApplication.LOG_TAG, "LibreLinkUp data received: " + receivedData);
                }
                return parseLastBgValue(receivedData);
            } else if (response.code() == 429) {
                String retryAfter = response.header("Retry-After");
                throw new TooManyRequestsException(retryAfter, "HTTP " + response.code() + " - Retry-After: " + retryAfter);
            } else {
                throw new CommunicationException("HTTP " + response.code() + " - " + response.message());
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", t);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, t.getLocalizedMessage()));
            if (t instanceof TooManyRequestsException) {
                throw (TooManyRequestsException) t;
            }
        }
        return null;
    }

    private List<GlucosePacket> parseLastBgValue(String rsp) {
        List<GlucosePacket> packets = new ArrayList<>();
        try {
            if (rsp != null && rsp.length() > 0) {
                JSONObject obj = new JSONObject(rsp);
                JSONObject data = obj.optJSONObject("data");
                if (data != null) {
                    JSONObject connection = data.optJSONObject("connection");
                    if (connection != null) {
                        JSONObject measurement = connection.optJSONObject("glucoseMeasurement");
                        if (measurement != null) {
                            if (BuildConfig.DEBUG) {
                                Log.i(LOG_TAG, "Measurement: " + measurement);
                            }

                            int glucoseValue = measurement.optInt("ValueInMgPerDl");

                            String timestampStr = measurement.optString("Timestamp");
                            SimpleDateFormat format = new SimpleDateFormat("M/d/y h:m:s a", Locale.ENGLISH);
                            long timestamp = 0;
                            try {
                                timestamp = format.parse(timestampStr).getTime();
                                if (getLastSampleTime() == null || timestamp <= getLastSampleTime()) {
                                    Log.w(LOG_TAG, "Timestamp same or older than previous: " + getLastSampleTime() + " -> " + timestamp);
                                    return packets;
                                }
                            } catch (ParseException e) {
                                Log.e(LOG_TAG, "Failed to parse timestamp: " + timestampStr);
                                timestamp = System.currentTimeMillis();
                            }

                            int trendInt = measurement.optInt("TrendArrow");
                            String trendStr = Integer.toString(trendInt);
                            Trend trend = toTrend(trendInt);

                            if (BuildConfig.DEBUG) {
                                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                                Log.w(GWatchApplication.LOG_TAG, "Trend: " + trendStr + " -> " + trend);
                                Log.w(GWatchApplication.LOG_TAG, "Timestanp: " + new Date(timestamp));
                            }

                            short glucose = (short) Math.round(glucoseValue);
                            packets.add(new GlucosePacket(glucose, timestamp, (byte) 0, trend, trendStr, SRC_LABEL_SHORT));
                            return packets;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
        return packets;
    }


    private String getProperty(Context context, String pref, String errMsg) {
        String value = PreferenceUtils.getStringValue(context, pref, StringUtils.EMPTY_STRING).trim();
        if (value.length() == 0) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed: " + errMsg);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, errMsg));
            return null;
        }
        return value;
    }

    private boolean parseRedirect(String rsp) {
        /*
            if( body.data.redirect ) { // redirect was received
                // { country: "CA", redirect: true, region: "eu", uiLanguage: "en-US" }
               ...
         */
        try {
            if (rsp != null && rsp.length() > 0) {
                JSONObject obj = new JSONObject(rsp);
                JSONObject data = obj.optJSONObject("data");
                if (data != null) {
                    boolean redirect = data.optBoolean("redirect");
                    if (redirect) {
                        if (BuildConfig.DEBUG) {
                            Log.i(LOG_TAG, "Redirect: " + redirect);
                        }
                        String region = data.optString("region");
                        serverUrl = resolveRegionalUrl(region);
                        return serverUrl != null;
                    }
                    return false;
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
        if (BuildConfig.DEBUG) {
            Log.e(LOG_TAG, "Redirect failed: " + rsp);
        }
        return false;
    }

    private String resolveRegionalUrl(String region) {
        if (region == null || region.length() < 2 || region.length() > 4) {
            return null;
        } else {
            return String.format(LLU_SERVER_URL_PATTERN, region);
        }
    }

    private String getServerUrl() {
        return serverUrl != null ? serverUrl : LLU_SERVER_URL;
    }

    private AuthResult extractToken(String rsp) {
        try {
            if (rsp != null && rsp.length() > 0) {
                JSONObject obj = new JSONObject(rsp);
                JSONObject data = obj.optJSONObject("data");
                if (data != null) {
                    JSONObject authTicket = data.optJSONObject("authTicket");
                    JSONObject user = data.optJSONObject("user");
                    if (authTicket != null && user != null) {
                        String token = authTicket.optString("token");
                        String accountId = user.optString("id");
                        if (BuildConfig.DEBUG) {
                            Log.i(LOG_TAG, "Auth token received: " + token);
                            Log.i(LOG_TAG, "Account id received: " + accountId);
                        }

                        return new AuthResult(token, accountId);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
        if (BuildConfig.DEBUG) {
            Log.e(LOG_TAG, "Auth token not received: " + rsp);
        }
        return null;
    }

    private String extractPatientId(String rsp) {
        try {
            if (rsp != null && rsp.length() > 0) {
                JSONObject obj = new JSONObject(rsp);
                JSONArray data = obj.optJSONArray("data");
                if (data != null) {
                    JSONObject patientData = data.optJSONObject(0);
                    if (patientData != null) {
                        String patientId = patientData.optString("patientId");
                        if (BuildConfig.DEBUG) {
                            Log.i(LOG_TAG, "Patient data received: " + patientData);
                        }
                        return patientId;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
        if (BuildConfig.DEBUG) {
            Log.e(LOG_TAG, "Patient data not received: " + rsp);
        }
        return null;
    }

    /**
     * Translates LibreLinkUp trend value to G-Watch internal trend representation
     */
    private static Trend toTrend(int value) {
        switch (value) {
            case 1:
                return Trend.DOWN;
            case 2:
                return Trend.DOWN_SLOW;
            case 3:
                return Trend.FLAT;
            case 4:
                return Trend.UP_SLOW;
            case 5:
                return Trend.UP;
            default:
                return Trend.UNKNOWN;
        }
    }

}

