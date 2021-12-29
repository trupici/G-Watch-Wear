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

package sk.trupici.gwatch.wear.settings;

import java.util.HashMap;
import java.util.Map;

import static sk.trupici.gwatch.wear.settings.ConfigData.*;

public class PreferenceMap {

    public final static Map<String, ConfigData> data = new HashMap<String, ConfigData>() {{
        // cfg_... simple configuration / direct mapping
        // prefs_... require some logic to get/create configuration

        // glucose levels
        put("cfg_glucose_level_hypo", new ConfigData(TAG_GL_THRESHOLD_HYPO, ConfigType.BYTE));
        put("cfg_glucose_level_low", new ConfigData(TAG_GL_THRESHOLD_LOW, ConfigType.BYTE));
        put("cfg_glucose_level_high", new ConfigData(TAG_GL_THRESHOLD_HIGH, ConfigType.BYTE));
        put("cfg_glucose_level_hyper", new ConfigData(TAG_GL_THRESHOLD_HYPER, ConfigType.WORD));
        put("cfg_glucose_units_conversion", new ConfigData(TAG_GL_UNIT_CONVERSION, ConfigType.BOOLEAN));


//        // nightscout config
//        put("cfg_nightscout_url", new ConfigData((byte)0xD0, PreferenceType.STRING));
//        put("cfg_nightscout_api_secret", new ConfigData((byte)0xD1, PreferenceType.STRING));
//        put("cfg_nightscout_token", new ConfigData((byte)0xD2, PreferenceType.STRING));
//        put("cfg_nightscout_latency", new ConfigData((byte)0xD3, PreferenceType.BYTE));
//
//        // dexcom share config
//        put("cfg_dexcom_share_account", new ConfigData((byte)0xD5, PreferenceType.STRING));
//        put("cfg_dexcom_share_secret", new ConfigData((byte)0xD6, PreferenceType.STRING));
//        put("cfg_dexcom_share_us_account", new ConfigData((byte)0xD7, PreferenceType.BOOLEAN));
//        put("cfg_dexcom_share_latency", new ConfigData((byte)0xD8, PreferenceType.BYTE));
//
//        // 0xFF - reserved - 2-byte tag indication
    }};
}
