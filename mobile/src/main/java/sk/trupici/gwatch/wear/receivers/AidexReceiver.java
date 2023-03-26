package sk.trupici.gwatch.wear.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Date;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.common.data.GlucosePacket;
import sk.trupici.gwatch.wear.common.data.Packet;
import sk.trupici.gwatch.wear.common.data.Trend;
import sk.trupici.gwatch.wear.common.util.BgUtils;
import sk.trupici.gwatch.wear.common.util.StringUtils;

public class AidexReceiver extends BGReceiver {

    private final static String SRC_LABEL = "AiDEX";
    private final static String PREFERENCE_KEY = "pref_data_source_aidex";
    private final static String ACTION = "com.microtechmd.cgms.aidex.action.BgEstimate";


    // BG Type: mmol/l or mg/dl */
    private final static String EXTRA_UNITS = "com.microtechmd.cgms.aidex.BgType";
    private final static String UNITS_MMOLL = "mmol/l";
    private final static String UNITS_MGDL = "mg/dl";

    private final static String EXTRA_TIMESTAMP = "com.microtechmd.cgms.aidex.Time"; // in ms
    private final static String EXTRA_VALUE = "com.microtechmd.cgms.aidex.BgValue"; // depends on bg type
    private final static String EXTRA_TREND = "com.microtechmd.cgms.aidex.BgSlopeName";


    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public String getSourceLabel() {
        return SRC_LABEL;
    }

    @Override
    public String getAction() {
        return ACTION;
    }

    @Override
    protected Packet processIntent(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String units = extras.getString(EXTRA_UNITS);
            double glucoseValue = extras.getDouble(EXTRA_VALUE);
            long timestamp = extras.getLong(EXTRA_TIMESTAMP);
            String trend = extras.getString(EXTRA_TREND);

            /* for debug only */
            if (glucoseValue < 1) {
                glucoseValue = Double.valueOf(String.valueOf(extras.getFloat(EXTRA_VALUE)));
            }

            if (BuildConfig.DEBUG) {
                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + units);
                Log.w(GWatchApplication.LOG_TAG, "Trend: " + trend);
                Log.w(GWatchApplication.LOG_TAG, "Timestamp: " + new Date(timestamp));
            }

            if (UNITS_MMOLL.equals(units)) {
                glucoseValue = BgUtils.convertGlucoseToMgDl(glucoseValue);
            } else if (!UNITS_MGDL.equals(units)) {
                Log.e(GWatchApplication.LOG_TAG, "Invalid BG TYPE value: " + units);
                return null;
            }

            short glucose = (short) Math.round(glucoseValue);
            if (glucose > 0) {
                return new GlucosePacket(glucose, timestamp, (byte) 0, toTrend(trend), trend, getSourceLabel());
            }
        }
        return null;
    }

    private static Trend toTrend(String value) {
        if (value == null) {
            return null;
        } else if ("DoubleUp".equals(value)) {
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
