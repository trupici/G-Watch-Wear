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

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.Trend;
import sk.trupici.gwatch.wear.util.BgUtils;
import sk.trupici.gwatch.wear.util.DexcomUtils;
import sk.trupici.gwatch.wear.util.PreferenceUtils;
import sk.trupici.gwatch.wear.util.StringUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

import static sk.trupici.gwatch.wear.GWatchApplication.LOG_TAG;
import static sk.trupici.gwatch.wear.util.StringUtils.EMPTY_STRING;

/**
 * DexCom Cloud Follower Service
 *
 * API V1 as described here: https://gist.github.com/StephenBlackWasAlreadyTaken/adb0525344bedade1e25
 */
public class DexcomShareFollowerService extends FollowerService {

    private static final String SRC_LABEL = "Dexcom Share";
    private static final String SRC_LABEL_SHORT = "Dexcom";

    private static final int DEF_DEXCOM_SAMPLE_LATENCY_MS = 30;

    public static final String PREF_DEXCOM_ENABLED = "pref_data_source_dexcom_share_enable";
    public static final String PREF_DEXCOM_US_ACCOUNT = "cfg_dexcom_share_us_account";
    private static final String PREF_DEXCOM_ACCOUNT = "cfg_dexcom_share_account";
    private static final String PREF_DEXCOM_SECRET = "cfg_dexcom_share_secret";
    private static final String PREF_DEXCOM_REQUEST_LATENCY = "cfg_dexcom_share_latency";

    public static final String DEXCOM_US_URL = "https://share2.dexcom.com";
    public static final String DEXCOM_NON_US_URL = "https://shareous1.dexcom.com";

    public static final String DEXCOM_PATH_GET_VALUE = "/ShareWebServices/Services/Publisher/ReadPublisherLatestGlucoseValues";
    public static final String DEXCOM_PATH_GET_SESSION_ID = "/ShareWebServices/Services/General/LoginPublisherAccountById";
    public static final String DEXCOM_PATH_AUTHENTICATE = "/ShareWebServices/Services/General/AuthenticatePublisherAccount";

    public static final String USER_AGENT = "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0";


    private static final String INVALID_ID = "00000000-0000-0000-0000-000000000000";
    private static final String[] DEXCOM_APP_IDS = new String[] {
            "d8665ade-9673-4e27-9ff6-92db4ce13d13",
            "d89443d2-327c-4a6f-89e5-496bbb0317db" // xdrip
    };

    private static final int MAX_SAMPLE_HISTORY_TIME_MIN = 60; // 1hr
    private static final int DEXCOM_MAX_SAMPLE_COUNT = 6; // 30m

    public static String secret;
    private static String account;
    private static String serverUrl;

    @Override
    protected void init() {
        super.init();
        account = null;
        secret = null;
        serverUrl = null;
    }

    @Override
    protected boolean isServiceEnabled(Context context) {
        return PreferenceUtils.isConfigured(context, PREF_DEXCOM_ENABLED, false);
    }

    @Override
    protected List<GlucosePacket> getServerValues(Context context) {
        return getDexcomValue(context, false);
    }


    @Override
    protected long getSampleToRequestDelay() {
        int delay = PreferenceUtils.getStringValueAsInt(GWatchApplication.getAppContext(), PREF_DEXCOM_REQUEST_LATENCY, DEF_DEXCOM_SAMPLE_LATENCY_MS);
        return delay * 1000;
    }

    private List<GlucosePacket> getDexcomValue(Context context, boolean recursion) {
        Request.Builder builder = new Request.Builder();
        try {
            String url = getServerUrl(context) + DEXCOM_PATH_GET_VALUE;
            builder = builder.url(url);
            String sessionId = authenticate(context);
            if (sessionId == null) {
                return null;
            }

            UiUtils.showMessage(context, context.getString(R.string.follower_data_request, SRC_LABEL));
            String minutes = String.valueOf(getLastSampleTime() == null ? MAX_SAMPLE_HISTORY_TIME_MIN
                    : (System.currentTimeMillis() - getLastSampleTime() ) / 60000);

            HttpUrl httpUrl = HttpUrl.parse(url).newBuilder()
                    .addQueryParameter("sessionId", sessionId)
                    .addQueryParameter("minutes", minutes)
                    .addQueryParameter("maxCount", String.valueOf(DEXCOM_MAX_SAMPLE_COUNT))
                    .build();
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "DSFService: httpUrl: " + httpUrl.toString());
            }

            builder = builder
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Length", "0")
                    .addHeader("Cache-Control",  "no-cache")
//                    .addHeader("Connection", "close")
                    .url(httpUrl)
                    .post(RequestBody.create(StringUtils.EMPTY_STRING, MediaType.get("application/json")))
                    ;
        } catch (Exception e) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", e);
            UiUtils.showMessage(context, e.getLocalizedMessage());
            return null;
        }

        try (Response response = getHttpClient(context).newCall(builder.build()).execute()) {
            if (response.isSuccessful()) {
                String receivedData = getResponseBodyAsString(response);
                if (receivedData != null && receivedData.length() > 0) {
                    Log.i(GWatchApplication.LOG_TAG, "Dexcom Share data received: " + receivedData);
                    return parseDexcomValue(receivedData);
                }
            } else {
                handleErrorIgnoreInvalidSession(response, Arrays.asList("SessionIdNotFound", "SessionNotValid"));
                if (!recursion) {
                    return getDexcomValue(context, true);
                }
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", t);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, t.getLocalizedMessage()));
        }
        return null;
    }

    private String getSessionId(Context context, String accountId, String appId) {
        String sessionId = getSessionData();
        if (sessionId != null) {
            return sessionId;
        }

        Log.e(LOG_TAG, "DSFService: getting session id...");
        UiUtils.showMessage(context, context.getString(R.string.follower_session_request, SRC_LABEL));

        Request.Builder builder = new Request.Builder();
        try {
            String url = getServerUrl(context) + DEXCOM_PATH_GET_SESSION_ID;
            builder = builder.url(url);
            JSONObject json = new JSONObject();
            json.put("accountId", accountId);
            json.put("password", getSecret(context));
            json.put("applicationId", appId);

            Request request = builder
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .url(url)
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();
        } catch (Exception e) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", e);
            UiUtils.showMessage(context, e.getLocalizedMessage());
            return null;
        }

        try (Response response = getHttpClient(context).newCall(builder.build()).execute()) {
            if (response.isSuccessful()) {
                String receivedData = getResponseBodyAsString(response);
                if (BuildConfig.DEBUG) {
                    Log.d(GWatchApplication.LOG_TAG, "Dexcom Share session id received: " + receivedData);
                }
                if (receivedData == null || receivedData.length() == 0) {
                    throw new CommunicationException(context.getString(R.string.follower_err_no_session));
                }
                sessionId = receivedData.replaceAll("\"", StringUtils.EMPTY_STRING);
                if (INVALID_ID.equals(sessionId)) {
                    Log.e(LOG_TAG, getClass().getSimpleName() + " failed: Invalid session id");
                    UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, context.getString(R.string.follower_err_no_session)));
                    return null;
                }
                setSessionData(sessionId);
                UiUtils.showMessage(context, context.getString(R.string.follower_rsp_ok));
                return sessionId;
            } else {
                handleErrorIgnoreInvalidSession(response, null);
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", t);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, t.getLocalizedMessage()));
        }
        return null;
    }

    private String authenticate(Context context) {
        String sessionId = getSessionData();
        if (sessionId != null) {
            return sessionId;
        }

        Log.e(LOG_TAG, "DSFService: authenticating...");
        UiUtils.showMessage(context, context.getString(R.string.follower_auth_request, SRC_LABEL));

        String appId = getApplicationId();
        Request.Builder builder = new Request.Builder();
        try {
            String url = getServerUrl(context) + DEXCOM_PATH_AUTHENTICATE;
            builder = builder.url(url);
            String account = getAccount(context);
            if (account == null) {
                return null;
            }
            JSONObject json = new JSONObject();
            json.put("accountName", account);
            json.put("password", getSecret(context));
            json.put("applicationId", appId);

            Request request = builder
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .url(url)
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();
        } catch (Exception e) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", e);
            UiUtils.showMessage(context, e.getLocalizedMessage());
            return null;
        }

        try (Response response = getHttpClient(context).newCall(builder.build()).execute()) {
            if (response.isSuccessful()) {
                String receivedData = getResponseBodyAsString(response);
                if (BuildConfig.DEBUG) {
                    Log.d(GWatchApplication.LOG_TAG, "Dexcom Share auth received: " + receivedData);
                }
                if (receivedData == null || receivedData.length() == 0) {
                    throw new CommunicationException(context.getString(R.string.follower_err_no_session));
                }
                String accountId = receivedData.replaceAll("\"", StringUtils.EMPTY_STRING);
                if (INVALID_ID.equals(accountId)) {
                    Log.e(LOG_TAG, getClass().getSimpleName() + " failed: Invalid account id");
                    UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, context.getString(R.string.follower_err_no_account_id)));
                    return null;
                }
                UiUtils.showMessage(context, context.getString(R.string.follower_rsp_ok));
                return getSessionId(context, accountId, appId);
            } else {
                handleErrorIgnoreInvalidSession(response, null);
            }
        } catch (Throwable t) {
            Log.e(LOG_TAG, getClass().getSimpleName() + " failed", t);
            UiUtils.showMessage(context, context.getString(R.string.follower_rsp_err_message, t.getLocalizedMessage()));
        }
        return null;
    }

    /**
     * @return one of known application ids
     */
    private String getApplicationId() {
        int count = DEXCOM_APP_IDS.length;
        int index = (int) (SystemClock.elapsedRealtime() % count);
        if (index < 0) {
            Log.e(LOG_TAG, "Negative clock time (" + index + ")???");
            index = 0;
        }
        return DEXCOM_APP_IDS[index];
    }

    private void handleErrorIgnoreInvalidSession(Response response, List<String> ignoreList) {
        setSessionData(null); // force session renegotiation
        try {
            String receivedData = getResponseBodyAsString(response);
            if (BuildConfig.DEBUG) {
                Log.d(GWatchApplication.LOG_TAG, "Dexcom Share failed: " + receivedData);
            }
            if (receivedData != null && receivedData.length() > 0) {
                JSONObject obj = new JSONObject(receivedData);
                String code = obj.optString("Code");
                String message = obj.optString("Message");
                if (code.length() > 0) {
                    if (ignoreList != null && ignoreList.contains(code)) {
                        if (BuildConfig.DEBUG) {
                            Log.w(LOG_TAG, "SessionId error:" + code + " - " + message);
                        }
                        return; // ignore error
                    }
                    throw new CommunicationException(code);
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(LOG_TAG, e.toString(), e);
        }
        throw new CommunicationException("HTTP " + response.code() + " - " + response.message());
    }

    private List<GlucosePacket> parseDexcomValue(String dexValue) throws JSONException {
        /*
            Array of:
             String DT; // device time
             String ST; // system (share) time
             String WT; //  World time / GMT
             double Trend; // 1-7
             double Value; // mg/dL
         */
        /*
            [
                {
                    "DT":"/Date(1638392742000)/",
                    "ST":"/Date(1638392742000)/",
                    "Trend":4,
                    "Value":193
                }
            ]
         */

        /*
            new format (Trend as String):
            [
                {
                    "WT":"Date(1638448498000)",
                    "ST":"Date(1638448498000)",
                    "DT":"Date(1638448498000+0000)",
                    "Value":235,
                    "Trend":"Flat"
                }
            ]
         */
        List<GlucosePacket> packets = new ArrayList<>();
        if (dexValue == null) {
            return packets;
        }


        JSONArray jsonArray = new JSONArray(dexValue);
        for (int i=0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);

            double glucoseValue = json.optDouble("Value", 0);
            String wt = json.optString("WT").replaceAll("[^0-9]", StringUtils.EMPTY_STRING);
            long timestamp = Long.valueOf(wt);

            Trend trend = null;
            Integer trendInt = null;
            try {
                trendInt = json.optInt("Trend");
                trend = toTrend(trendInt);
            } catch (Exception e) {
                Log.e(LOG_TAG, "parseDexcomValue: " + e.getLocalizedMessage());
            }

            String trendStr = null;
            if (trendInt == null) {
                trendStr = json.optString("Trend", null);
                trend = DexcomUtils.toTrend(trendStr);
            }

            if (BuildConfig.DEBUG) {
                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + BgUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                Log.w(GWatchApplication.LOG_TAG, "Trend: " + (trendInt != null ? trendInt : trendStr) + " -> " + trend);
                Log.w(GWatchApplication.LOG_TAG, "Timestanp: " + new Date(timestamp));
            }

            short glucose = (short) Math.round(glucoseValue);
            packets.add(new GlucosePacket(glucose, timestamp, (byte) 0, trend, (trendInt != null ? trendInt.toString() : trendStr), SRC_LABEL_SHORT));
        }
        return packets;
    }


    /**
     * Translates DexCom trend value to G-Watch internal trend representation
     */
    private static Trend toTrend(Integer value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case 0:
                return null; // from optString() if not found
            case 1:
                return Trend.UP_FAST;
            case 2:
                return Trend.UP;
            case 3:
                return Trend.UP_SLOW;
            case 4:
                return Trend.FLAT;
            case 5:
                return Trend.DOWN_SLOW;
            case 6:
                return Trend.DOWN;
            case 7:
                return Trend.DOWN_FAST;
            default:
                return Trend.UNKNOWN;
        }
    }

    private String getAccount(Context context) {
        if (account != null) {
            if (account.length() == 0) {
                return null;
            } else {
                return account;
            }
        }

        account = PreferenceUtils.getStringValue(context, PREF_DEXCOM_ACCOUNT, null);
        if (account == null) {
            account = EMPTY_STRING; // avoid evaluation on next requests
            return null;
        }
        return account;
    }

    /**
     * Returns secret for account
     * or null if no secret was configured
     */
    private String getSecret(Context context) {
        if (secret != null) {
            if (secret.length() == 0) {
                return null;
            } else {
                return secret;
            }
        }

        secret = PreferenceUtils.getStringValue(context, PREF_DEXCOM_SECRET, null);
        if (secret == null) {
            secret = EMPTY_STRING;
            return null;
        }
        return secret;
    }

    private String getServerUrl(Context context) {
        Boolean isUsAccount = PreferenceUtils.isConfigured(context, PREF_DEXCOM_US_ACCOUNT, false);

        serverUrl = isUsAccount ? DEXCOM_US_URL : DEXCOM_NON_US_URL;
        return serverUrl;
    }
}
