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

package sk.trupici.gwatch.wear.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.GWatchApplication;
import sk.trupici.gwatch.wear.data.GlucosePacket;
import sk.trupici.gwatch.wear.data.Packet;
import sk.trupici.gwatch.wear.util.UiUtils;

/**
 * Modify method sendIntent in ThirdPartyIntegration.smali to send data to any app:
 * .method private static sendIntent(Landroid/content/Intent;)V
 *     .registers 2
 *     .param p0, "intent"    # Landroid/content/Intent;
 *
 *     .prologue
 *
 *     # send implicit broadcast to all apps
 *     sget-object v0, Lcom/librelink/app/ThirdPartyIntegration;->context:Landroid/content/Context;
 *
 *     invoke-virtual {v0, p0}, Landroid/content/Context;->sendBroadcast(Landroid/content/Intent;)V
 *
 *
 *     # make new intent instance for xDrip
 *     new-instance v0, Landroid/content/Intent;
 *
 *     invoke-direct {v0, p0}, Landroid/content/Intent;-><init>(Landroid/content/Intent;)V
 *
 *     const-string p0, "com.eveningoutpost.dexdrip"
 *
 *     invoke-virtual {v0, p0}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;
 *
 *     # send broadcast to xDrip
 *     sget-object p0, Lcom/librelink/app/ThirdPartyIntegration;->context:Landroid/content/Context;
 *
 *     invoke-virtual {p0, v0}, Landroid/content/Context;->sendBroadcast(Landroid/content/Intent;)V
 *
 *     return-void
 * .end method
 *
 * INTENT DATA DESCRIPTION:
 * SMALI method fragment:
 * .method public static sendGlucoseBroadcast(Lcom/librelink/app/core/BleManager;Lcom/abbottdiabetescare/flashglucose/sensorabstractionservice/CurrentGlucose;)V
 *     .registers 8
 *     .param p0, "bleManager"    # Lcom/librelink/app/core/BleManager;
 *     .param p1, "currentGlucose"    # Lcom/abbottdiabetescare/flashglucose/sensorabstractionservice/CurrentGlucose;
 *     .prologue
 *
 *     new-instance v0, Landroid/content/Intent;
 *     const-string v1, "com.librelink.app.ThirdPartyIntegration.GLUCOSE_READING"
 *     invoke-direct {v0, v1}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
 *
 *     .local v0, "intent":Landroid/content/Intent;
 *     const-string v1, "glucose"
 *     invoke-virtual {p1}, Lcom/abbottdiabetescare/flashglucose/sensorabstractionservice/CurrentGlucose;->getGlucoseValue()D
 *     move-result-wide v2
 *     invoke-virtual {v0, v1, v2, v3}, Landroid/content/Intent;->putExtra(Ljava/lang/String;D)Landroid/content/Intent;
 *
 *     const-string v2, "timestamp"
 *     invoke-virtual {p1}, Lcom/abbottdiabetescare/flashglucose/sensorabstractionservice/CurrentGlucose;->getTimestampUTC()Ljava/lang/Object;
 *     move-result-object v1
 *     check-cast v1, Lorg/joda/time/DateTime;
 *     invoke-virtual {v1}, Lorg/joda/time/DateTime;->toDate()Ljava/util/Date;
 *     move-result-object v1
 *     invoke-virtual {v1}, Ljava/util/Date;->getTime()J
 *     move-result-wide v4
 *     invoke-virtual {v0, v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;J)Landroid/content/Intent;
 *
 *     const-string v1, "bleManager"
 *     invoke-static {p0}, Lcom/librelink/app/ThirdPartyIntegration;->getBLEManagerFields(Lcom/librelink/app/core/BleManager;)Landroid/os/Bundle;
 *     move-result-object v2
 *     invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Landroid/os/Bundle;)Landroid/content/Intent;
 *
 *     const-string v1, "sas"
 *     invoke-virtual {p0}, Lcom/librelink/app/core/BleManager;->getSAS()Lcom/librelink/app/types/SAS;
 *     move-result-object v2
 *     invoke-static {v2}, Lcom/librelink/app/ThirdPartyIntegration;->getSASFields(Lcom/librelink/app/types/SAS;)Landroid/os/Bundle;
 *     move-result-object v2
 *     invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Landroid/os/Bundle;)Landroid/content/Intent;
 *
 *     invoke-static {v0}, Lcom/librelink/app/ThirdPartyIntegration;->sendIntent(Landroid/content/Intent;)V
 *     return-void
 * .end method
 */
public class LibreLinkReceiver extends BGReceiver {
    private final static String ACTION = "com.librelink.app.ThirdPartyIntegration.GLUCOSE_READING";
    private final static String EXTRA_GLUCOSE = "glucose";
    private final static String EXTRA_TIMESTAMP = "timestamp";
    private final static String SRC_LABEL = "LibreLink";
    private final static String PREFERENCE_KEY = "pref_data_source_libre";

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
            Log.i(GWatchApplication.LOG_TAG, "Bundle: " + extras.toString());
            long timestamp = extras.getLong(EXTRA_TIMESTAMP);
            double glucoseValue = extras.getDouble(EXTRA_GLUCOSE);
            if (BuildConfig.DEBUG) {
                Log.w(GWatchApplication.LOG_TAG, "Glucose: " + glucoseValue + " mg/dl / " + UiUtils.convertGlucoseToMmolL(glucoseValue) + " mmol/l");
                Log.w(GWatchApplication.LOG_TAG, "Timestanp: " + timestamp);
            }
            short glucose = (short)Math.round(glucoseValue);
            if (glucose > 0) {
                return new GlucosePacket(glucose, timestamp, (byte) 0, null, null, getSourceLabel());
            }
        }
        return null;
    }
}
