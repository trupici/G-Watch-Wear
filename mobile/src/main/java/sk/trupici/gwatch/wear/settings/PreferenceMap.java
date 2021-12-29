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

import android.util.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferenceMap {

    public enum PreferenceType {
        BYTE,
        WORD,
        DWORD,
        COLOR,
        BOOLEAN,
        STRING
    }

    public final static Map<String, Pair<Byte, PreferenceType>> data = new HashMap<String, Pair<Byte, PreferenceType>>() {{
        // cfg_... simple configuration / direct mapping
        // prefs_... require some logic to get/create configuration

        // glucose levels
        put("cfg_glucose_level_hypo", new Pair<>((byte)0x01, PreferenceType.BYTE));
        put("cfg_glucose_level_low", new Pair<>((byte)0x02, PreferenceType.BYTE));
        put("cfg_glucose_level_high", new Pair<>((byte)0x03, PreferenceType.BYTE));
        put("cfg_glucose_level_hyper", new Pair<>((byte)0x04, PreferenceType.WORD));

        put("cfg_glucose_units_conversion", new Pair<>((byte)0x10, PreferenceType.BOOLEAN));

//        // standalone mode
////        put("cfg_standalone_type", new Pair<>((byte)0xC8, PreferenceType.BYTE));
////        put("cfg_standalone_source", new Pair<>((byte)0xC9, PreferenceType.BYTE));
//        put("cfg_standalone_menu_active", new Pair<>((byte)0xCA, PreferenceType.BOOLEAN));
//        put("cfg_standalone_color", new Pair<>((byte)0xCB, PreferenceType.COLOR));
//        put("cfg_standalone_bkg_color", new Pair<>((byte)0xCC, PreferenceType.COLOR));
//        put("cfg_standalone_bkg_error_color", new Pair<>((byte)0xCD, PreferenceType.COLOR));
//
//        // nightscout config
//        put("cfg_nightscout_url", new Pair<>((byte)0xD0, PreferenceType.STRING));
//        put("cfg_nightscout_api_secret", new Pair<>((byte)0xD1, PreferenceType.STRING));
//        put("cfg_nightscout_token", new Pair<>((byte)0xD2, PreferenceType.STRING));
//        put("cfg_nightscout_latency", new Pair<>((byte)0xD3, PreferenceType.BYTE));
//
//        // dexcom share config
//        put("cfg_dexcom_share_account", new Pair<>((byte)0xD5, PreferenceType.STRING));
//        put("cfg_dexcom_share_secret", new Pair<>((byte)0xD6, PreferenceType.STRING));
//        put("cfg_dexcom_share_us_account", new Pair<>((byte)0xD7, PreferenceType.BOOLEAN));
//        put("cfg_dexcom_share_latency", new Pair<>((byte)0xD8, PreferenceType.BYTE));
//
//        // 0xFF - reserved - 2-byte tag indication
    }};

    public static final List mappedOnClickPrefs = Arrays.asList(
    );

}
