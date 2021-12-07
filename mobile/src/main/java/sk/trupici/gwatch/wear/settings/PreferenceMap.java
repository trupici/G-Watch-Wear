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

//    public enum PreferenceType {
//        BYTE,
//        WORD,
//        DWORD,
//        COLOR,
//        BOOLEAN,
//        STRING
//    }

//    public final static Map<String, Pair<Byte, PreferenceType>> data = new HashMap<String, Pair<Byte, PreferenceType>>() {{
//        // cfg_... simple configuration / direct mapping
//        // prefs_... require some logic to get/create configuration
//
//        // glucose levels
//        put("cfg_glucose_level_hypo", new Pair<>((byte)0x01, PreferenceType.BYTE));
//        put("cfg_glucose_level_low", new Pair<>((byte)0x02, PreferenceType.BYTE));
//        put("cfg_glucose_level_high", new Pair<>((byte)0x03, PreferenceType.BYTE));
//        put("cfg_glucose_level_hyper", new Pair<>((byte)0x04, PreferenceType.WORD));
//
//        // watch alarms (continued 2) - sound active time
//        put("cfg_alarms_sound_active_time", new Pair<>((byte)0x05, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_active_from", new Pair<>((byte)0x06, PreferenceType.WORD));
//        put("cfg_alarms_sound_active_to", new Pair<>((byte)0x07, PreferenceType.WORD));
//
//        // connection type
//        put("cfg_connection_type", new Pair<>((byte)0x08, PreferenceType.BYTE));
//
//        // 0x09
//        // 0x0A
//
//        // graph (continued)
//        put("cfg_graph_always_on", new Pair<>((byte)0x0B, PreferenceType.BOOLEAN));
//
//        // always on display
//        put("cfg_aod_type", new Pair<>((byte)0x0C, PreferenceType.BYTE));
//        put("cfg_aod_analog_indicator_type", new Pair<>((byte)0x0D, PreferenceType.BYTE));
//        put("cfg_aod_analog_dial_type", new Pair<>((byte)0x0E, PreferenceType.BYTE));
//        put("cfg_aod_background_type", new Pair<>((byte)0x0F, PreferenceType.BYTE));
//
//        // background type
//        put("cfg_background_type", new Pair<>((byte)0x10, PreferenceType.BYTE));
//        put("cfg_background_color", new Pair<>((byte)0x11, PreferenceType.COLOR));
//
//        // logo
//        put("cfg_show_logo", new Pair<>((byte)0x12, PreferenceType.BOOLEAN));
//        put("cfg_logo_color", new Pair<>((byte)0x13, PreferenceType.COLOR));
//
//        // dial type
//        put("cfg_dial_type", new Pair<>((byte)0x14, PreferenceType.BYTE));
//
//        // 0x15
//        // 0x16
//        // 0x17
//
//        // always on display (continued)
//        put("cfg_aod_color_critical", new Pair<>((byte)0x18, PreferenceType.COLOR));
//        put("cfg_aod_color_warn", new Pair<>((byte)0x19, PreferenceType.COLOR));
//        put("cfg_aod_color_in_range", new Pair<>((byte)0x1A, PreferenceType.COLOR));
//        put("cfg_aod_color_no_data", new Pair<>((byte)0x1B, PreferenceType.COLOR));
//        put("cfg_aod_color_disconnected", new Pair<>((byte)0x1C, PreferenceType.COLOR));
//        put("cfg_aod_trend_color_steep", new Pair<>((byte)0x1D, PreferenceType.COLOR));
//        put("cfg_aod_trend_color_moderate", new Pair<>((byte)0x1E, PreferenceType.COLOR));
//        put("cfg_aod_trend_color_flat", new Pair<>((byte)0x1F, PreferenceType.COLOR));
//
//        // glucose panel
//        put("cfg_glucose_panel_color_critical", new Pair<>((byte)0x20, PreferenceType.COLOR));
//        put("cfg_glucose_panel_color_warn", new Pair<>((byte)0x21, PreferenceType.COLOR));
//        put("cfg_glucose_panel_color_in_range", new Pair<>((byte)0x22, PreferenceType.COLOR));
//        put("cfg_glucose_units", new Pair<>((byte)0x23, PreferenceType.BOOLEAN));
//
//        // trend indicator
//        put("cfg_trend_panel_color_steep", new Pair<>((byte)0x24, PreferenceType.COLOR));
//        put("cfg_trend_panel_color_moderate", new Pair<>((byte)0x25, PreferenceType.COLOR));
//        put("cfg_trend_panel_color_flat", new Pair<>((byte)0x26, PreferenceType.COLOR));
//
//        // 0x27
//
//        // calendar panel
//        put("cfg_calendar_type", new Pair<>((byte)0x28, PreferenceType.BYTE));
//        put("cfg_calendar_panel_color", new Pair<>((byte)0x29, PreferenceType.COLOR));
//        put("cfg_calendar_font_big", new Pair<>((byte)0x2A, PreferenceType.BOOLEAN));
//        put("cfg_calendar_on_click_enable", new Pair<>((byte)0x2B, PreferenceType.BOOLEAN));
//
//        // battery panel
//        put("cfg_battery_type", new Pair<>((byte)0x2C, PreferenceType.BOOLEAN));
//        put("cfg_battery_panel_color", new Pair<>((byte)0x2D, PreferenceType.COLOR));
//        put("cfg_battery_panel_color_critical", new Pair<>((byte)0x2E, PreferenceType.COLOR));
//        put("cfg_battery_font_big", new Pair<>((byte)0x2F, PreferenceType.BOOLEAN));
//
//        // status panel
//        put("cfg_status_panel_color_no_data", new Pair<>((byte)0x30, PreferenceType.COLOR));
//        put("cfg_status_panel_color_disconnected", new Pair<>((byte)0x31, PreferenceType.COLOR));
////        put("cfg_status_panel_color_info", new Pair<>((byte)0x32, PreferenceType.COLOR));
//        put("cfg_status_panel_show_time_after", new Pair<>((byte)0x33, PreferenceType.WORD));
//        put("cfg_status_panel_no_data_time", new Pair<>((byte)0x34, PreferenceType.WORD));
//        put("cfg_status_font_big", new Pair<>((byte)0x35, PreferenceType.BOOLEAN));
//
//        // 0x36
//        // 0x37
//
//        // glucose panel (continued)
//        put("cfg_glucose_font_big", new Pair<>((byte)0x38, PreferenceType.BOOLEAN));
//        put("cfg_enable_glucose_units_toggle", new Pair<>((byte)0x39, PreferenceType.BOOLEAN));
//        put("cfg_trend_font_big", new Pair<>((byte)0x3A, PreferenceType.BOOLEAN));
//        put("cfg_trend_no_units", new Pair<>((byte)0x3B, PreferenceType.BOOLEAN));
//
//        // 0x3C
//        // 0x3D
//
//        // battery panel (continued)
//        put("cfg_battery_on_click_enable", new Pair<>((byte)0x3E, PreferenceType.BOOLEAN));
//
//        // debug
//        put("cfg_debug_enabled", new Pair<>((byte)0x3F, PreferenceType.BOOLEAN));
//
//        // hour hand
//        put("cfg_hour_hand_color_critical", new Pair<>((byte)0x40, PreferenceType.COLOR));
//        put("cfg_hour_hand_color_warn", new Pair<>((byte)0x41, PreferenceType.COLOR));
//        put("cfg_hour_hand_color_in_range", new Pair<>((byte)0x42, PreferenceType.COLOR));
//        put("cfg_hour_hand_color_no_data", new Pair<>((byte)0x43, PreferenceType.COLOR));
//        put("cfg_hour_hand_color_disconnected", new Pair<>((byte)0x44, PreferenceType.COLOR));
//        put("cfg_hour_hand_width", new Pair<>((byte)0x45, PreferenceType.BYTE));
//        put("cfg_hour_hand_length", new Pair<>((byte)0x46, PreferenceType.BYTE));
//        put("cfg_hour_hand_tail", new Pair<>((byte)0x47, PreferenceType.BYTE));
//        put("cfg_hour_hand_tail_negative", new Pair<>((byte)0x48, PreferenceType.BOOLEAN));
//        put("cfg_hour_hand_round_endings", new Pair<>((byte)0x49, PreferenceType.BOOLEAN));
//        put("cfg_hour_hand_type", new Pair<>((byte)0x4A, PreferenceType.BOOLEAN));
//
//        // 0x4B
//        // 0x4C
//        // 0x4D
//        // 0x4E
//        // 0x4F
//
//        // minute hand
//        put("cfg_min_hand_color_critical", new Pair<>((byte)0x50, PreferenceType.COLOR));
//        put("cfg_min_hand_color_warn", new Pair<>((byte)0x51, PreferenceType.COLOR));
//        put("cfg_min_hand_color_in_range", new Pair<>((byte)0x52, PreferenceType.COLOR));
//        put("cfg_min_hand_color_no_data", new Pair<>((byte)0x53, PreferenceType.COLOR));
//        put("cfg_min_hand_color_disconnected", new Pair<>((byte)0x54, PreferenceType.COLOR));
//        put("cfg_min_hand_width", new Pair<>((byte)0x55, PreferenceType.BYTE));
//        put("cfg_min_hand_length", new Pair<>((byte)0x56, PreferenceType.BYTE));
//        put("cfg_min_hand_tail", new Pair<>((byte)0x57, PreferenceType.BYTE));
//        put("cfg_min_hand_tail_negative", new Pair<>((byte)0x58, PreferenceType.BOOLEAN));
//        put("cfg_min_hand_round_endings", new Pair<>((byte)0x59, PreferenceType.BOOLEAN));
//        put("cfg_min_hand_type", new Pair<>((byte)0x5A, PreferenceType.BOOLEAN));
//
//        // 0x5B
//        // 0x5C
//        // 0x5D
//        // 0x5E
//        // 0x5F
//
//        // second hand
//        put("cfg_sec_hand_color_critical", new Pair<>((byte)0x60, PreferenceType.COLOR));
//        put("cfg_sec_hand_color_warn", new Pair<>((byte)0x61, PreferenceType.COLOR));
//        put("cfg_sec_hand_color_in_range", new Pair<>((byte)0x62, PreferenceType.COLOR));
//        put("cfg_sec_hand_color_no_data", new Pair<>((byte)0x63, PreferenceType.COLOR));
//        put("cfg_sec_hand_color_disconnected", new Pair<>((byte)0x64, PreferenceType.COLOR));
//        put("cfg_sec_hand_width", new Pair<>((byte)0x65, PreferenceType.BYTE));
//        put("cfg_sec_hand_length", new Pair<>((byte)0x66, PreferenceType.BYTE));
//        put("cfg_sec_hand_tail", new Pair<>((byte)0x67, PreferenceType.BYTE));
//        put("cfg_sec_hand_tail_width", new Pair<>((byte)0x68, PreferenceType.BYTE));
//        put("cfg_sec_hand_cap", new Pair<>((byte)0x69, PreferenceType.BYTE));
//        put("cfg_sec_hand_type", new Pair<>((byte)0x6A, PreferenceType.BOOLEAN));
//        put("cfg_sec_hand_tail_negative", new Pair<>((byte)0x6B, PreferenceType.BOOLEAN));
//        put("cfg_sec_hand_round_endings", new Pair<>((byte)0x6C, PreferenceType.BOOLEAN));
//
//        // 0x6D
//        // 0x6E
//
//        // watch alarms (continued 3) - fast drop alarm
//        put("cfg_alarms_fastdrop_enable", new Pair<>((byte)0x70, PreferenceType.BOOLEAN));
//        put("cfg_alarms_fastdrop_threshold", new Pair<>((byte)0x71, PreferenceType.BYTE));
//        put("cfg_alarms_fastdrop_period", new Pair<>((byte)0x72, PreferenceType.BYTE));
//        put("cfg_alarms_fastdrop_intensity", new Pair<>((byte)0x73, PreferenceType.BYTE));
//        put("cfg_alarms_fastdrop_duration", new Pair<>((byte)0x74, PreferenceType.BYTE));
//        put("cfg_alarms_sound_fastdrop_enable", new Pair<>((byte)0x75, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_fastdrop_volume", new Pair<>((byte)0x76, PreferenceType.BYTE));
//        put("cfg_alarms_sound_custom_fastdrop", new Pair<>((byte)0x77, PreferenceType.BOOLEAN));
//        // watch alarms - no data alarms
//        put("cfg_alarms_nodata_enable", new Pair<>((byte)0x78, PreferenceType.BOOLEAN));
//        put("cfg_alarms_nodata_period", new Pair<>((byte)0x79, PreferenceType.BYTE));
//        put("cfg_alarms_nodata_snooze_duration", new Pair<>((byte)0x7A, PreferenceType.BYTE));
//        put("cfg_alarms_nodata_intensity", new Pair<>((byte)0x7B, PreferenceType.BYTE));
//        put("cfg_alarms_nodata_duration", new Pair<>((byte)0x7C, PreferenceType.BYTE));
//        put("cfg_alarms_sound_nodata_enable", new Pair<>((byte)0x7D, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_nodata_volume", new Pair<>((byte)0x7E, PreferenceType.BYTE));
//        put("cfg_alarms_sound_custom_nodata", new Pair<>((byte)0x7F, PreferenceType.BOOLEAN));
//
//        // graph
//        put("cfg_graph_type", new Pair<>((byte)0x80, PreferenceType.BYTE));
//        put("cfg_graph_horz_lines", new Pair<>((byte)0x81, PreferenceType.BOOLEAN));
//        put("cfg_graph_vert_lines", new Pair<>((byte)0x82, PreferenceType.BOOLEAN));
//        put("cfg_graph_refresh_rate", new Pair<>((byte)0x83, PreferenceType.BYTE));
//        put("cfg_graph_horz_lines_color", new Pair<>((byte)0x84, PreferenceType.COLOR));
//        put("cfg_graph_vert_lines_color", new Pair<>((byte)0x85, PreferenceType.COLOR));
//        put("cfg_graph_background_color", new Pair<>((byte)0x86, PreferenceType.COLOR));
//        put("cfg_graph_color_critical", new Pair<>((byte)0x87, PreferenceType.COLOR));
//        put("cfg_graph_color_warn", new Pair<>((byte)0x88, PreferenceType.COLOR));
//        put("cfg_graph_color_in_range", new Pair<>((byte)0x89, PreferenceType.COLOR));
//        put("cfg_graph_high_line", new Pair<>((byte)0x8A, PreferenceType.BOOLEAN));
//        put("cfg_graph_high_line_color", new Pair<>((byte)0x8B, PreferenceType.COLOR));
//        put("cfg_graph_low_line", new Pair<>((byte)0x8C, PreferenceType.BOOLEAN));
//        put("cfg_graph_low_line_color", new Pair<>((byte)0x8D, PreferenceType.COLOR));
//        put("cfg_graph_dynamic_scale", new Pair<>((byte)0x8E, PreferenceType.BOOLEAN));
//        put("cfg_graph_time_range_toggle", new Pair<>((byte)0x8F, PreferenceType.BOOLEAN));
//
//        // aaps
//        put("cfg_aaps_type", new Pair<>((byte)0x90, PreferenceType.BOOLEAN));
//        put("cfg_aaps_popup_always_active", new Pair<>((byte)0x91, PreferenceType.BOOLEAN));
//        put("cfg_aaps_cob_color", new Pair<>((byte)0x92, PreferenceType.COLOR));
//        put("cfg_aaps_iob_color", new Pair<>((byte)0x93, PreferenceType.COLOR));
//        put("cfg_aaps_tbr_color", new Pair<>((byte)0x94, PreferenceType.COLOR));
//        put("cfg_aaps_bkg_color", new Pair<>((byte)0x95, PreferenceType.COLOR));
//
//        // 0x96
//        // 0x97
//        // 0x98
//        // 0x99
//        // 0x9A
//        // 0x9B
//        // 0x9C
//        // 0x9D
//        // 0x9E
//        // 0x9F
//
//        // sensors
//        put("cfg_sensors_use_shealth", new Pair<>((byte)0xA0, PreferenceType.BOOLEAN));
//        put("cfg_sensor1_type", new Pair<>((byte)0xA1, PreferenceType.BYTE));
//        put("cfg_sensor2_type", new Pair<>((byte)0xA2, PreferenceType.BYTE));
//        put("cfg_sensors_bat_color", new Pair<>((byte)0xA3, PreferenceType.COLOR));
//        put("cfg_sensors_bat_critical_color", new Pair<>((byte)0xA4, PreferenceType.COLOR));
//        put("cfg_sensors_pdm_color", new Pair<>((byte)0xA5, PreferenceType.COLOR));
//        put("cfg_sensors_pdm_reset", new Pair<>((byte)0xA6, PreferenceType.BOOLEAN));
//        put("cfg_sensors_hrm_color", new Pair<>((byte)0xA7, PreferenceType.COLOR));
//        put("cfg_sensors_hrm_period", new Pair<>((byte)0xA8, PreferenceType.BYTE));
//        put("cfg_sensors_hrm_now", new Pair<>((byte)0xA9, PreferenceType.BOOLEAN));
//        put("cfg_sensors_phone_bat_color", new Pair<>((byte)0xAA, PreferenceType.COLOR));
//        put("cfg_sensors_phone_bat_critical_color", new Pair<>((byte)0xAB, PreferenceType.COLOR));
//
//        // 0xAC
//        // 0xAD
//        // 0xAE
//        // 0xAF
//
//        // watch alarms
//        put("cfg_alarms_type", new Pair<>((byte)0xB0, PreferenceType.BYTE));
////        put("cfg_alarms_intensity", new Pair<>((byte)0xB1, PreferenceType.BYTE));
//        put("cfg_alarms_snooze_duration", new Pair<>((byte)0xB2, PreferenceType.BYTE));
////        put("cfg_alarms_critical_only", new Pair<>((byte)0xB3, PreferenceType.BOOLEAN));
//        put("cfg_alarms_active_from", new Pair<>((byte)0xB4, PreferenceType.WORD));  // in minutes
//        put("cfg_alarms_active_to", new Pair<>((byte)0xB5, PreferenceType.WORD)); // in minutes
//        put("cfg_alarms_period_lows", new Pair<>((byte)0xB6, PreferenceType.BYTE));
//        put("cfg_alarms_period_highs", new Pair<>((byte)0xB7, PreferenceType.BYTE));
//        put("cfg_alarms_warn_enable", new Pair<>((byte)0xB8, PreferenceType.BOOLEAN));
//        put("cfg_alarms_warn_intensity", new Pair<>((byte)0xB9, PreferenceType.BYTE));
//        put("cfg_alarms_warn_duration", new Pair<>((byte)0xBA, PreferenceType.BYTE));
//        put("cfg_alarms_danger_enable", new Pair<>((byte)0xBB, PreferenceType.BOOLEAN));
//        put("cfg_alarms_danger_intensity", new Pair<>((byte)0xBC, PreferenceType.BYTE));
//        put("cfg_alarms_danger_duration", new Pair<>((byte)0xBD, PreferenceType.BYTE));
//        put("cfg_alarms_new_data", new Pair<>((byte)0xBE, PreferenceType.BOOLEAN));
//
//        // 0xBF
//
//        // digital info panel
//        put("cfg_digital_type", new Pair<>((byte)0xC0, PreferenceType.BYTE));
//        put("cfg_digital_secs", new Pair<>((byte)0xC1, PreferenceType.BOOLEAN));
//        put("cfg_digital_txt_color", new Pair<>((byte)0xC2, PreferenceType.COLOR));
//        put("cfg_digital_bkg_color", new Pair<>((byte)0xC3, PreferenceType.COLOR));
//        put("cfg_digital_time_format", new Pair<>((byte)0xC4, PreferenceType.BYTE));
//        put("cfg_digital_font_big", new Pair<>((byte)0xC5, PreferenceType.BOOLEAN));
//        put("cfg_digital_jumbo_font", new Pair<>((byte)0xC6, PreferenceType.BOOLEAN));
//
//        // 0xC7
//
//        // standalone mode
////        put("cfg_standalone_type", new Pair<>((byte)0xC8, PreferenceType.BYTE));
////        put("cfg_standalone_source", new Pair<>((byte)0xC9, PreferenceType.BYTE));
//        put("cfg_standalone_menu_active", new Pair<>((byte)0xCA, PreferenceType.BOOLEAN));
//        put("cfg_standalone_color", new Pair<>((byte)0xCB, PreferenceType.COLOR));
//        put("cfg_standalone_bkg_color", new Pair<>((byte)0xCC, PreferenceType.COLOR));
//        put("cfg_standalone_bkg_error_color", new Pair<>((byte)0xCD, PreferenceType.COLOR));
//
//        // 0xCE
//        // 0xCF
//
//        // nightscout config
//        put("cfg_nightscout_url", new Pair<>((byte)0xD0, PreferenceType.STRING));
//        put("cfg_nightscout_api_secret", new Pair<>((byte)0xD1, PreferenceType.STRING));
//        put("cfg_nightscout_token", new Pair<>((byte)0xD2, PreferenceType.STRING));
//        put("cfg_nightscout_latency", new Pair<>((byte)0xD3, PreferenceType.BYTE));
//
//        // 0xD4
//
//        // dexcom share config
//        put("cfg_dexcom_share_account", new Pair<>((byte)0xD5, PreferenceType.STRING));
//        put("cfg_dexcom_share_secret", new Pair<>((byte)0xD6, PreferenceType.STRING));
//        put("cfg_dexcom_share_us_account", new Pair<>((byte)0xD7, PreferenceType.BOOLEAN));
//        put("cfg_dexcom_share_latency", new Pair<>((byte)0xD8, PreferenceType.BYTE));
//
//        // 0xD9
//
//        // 0xDA - reserved for themes
//        // 0xDB - reserved for themes
//        // 0xDC - reserved for themes
//
//        // 0xDD
//        // 0xDE
//        // 0xDF
//
//        // watch alarms (continued 1) - sounds
//        put("cfg_alarms_sound_critical_enable", new Pair<>((byte)0xE0, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_lows_enable", new Pair<>((byte)0xE1, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_highs_enable", new Pair<>((byte)0xE2, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_critical_volume", new Pair<>((byte)0xE3, PreferenceType.BYTE));
//        put("cfg_alarms_sound_lows_volume", new Pair<>((byte)0xE4, PreferenceType.BYTE));
//        put("cfg_alarms_sound_highs_volume", new Pair<>((byte)0xE5, PreferenceType.BYTE));
//        put("cfg_alarms_sound_custom_critical", new Pair<>((byte)0xE6, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_custom_lows", new Pair<>((byte)0xE7, PreferenceType.BOOLEAN));
//        put("cfg_alarms_sound_custom_highs", new Pair<>((byte)0xE8, PreferenceType.BOOLEAN));
//
//        // 0xE9
//        // 0xEA
//        // 0xEB
//        // 0xEC
//        // 0xED
//        // 0xEE
//        // 0xEF
//
//        // 0xF0
//        // 0xF1
//        // 0xF2
//        // 0xF3
//        // 0xF4
//        // 0xF5
//        // 0xF6
//        // 0xF7
//        // 0xF8
//        // 0xF9
//        // 0xFA
//        // 0xFB
//        // 0xFC
//        // 0xFD
//        // 0xFE
//
//        // 0xFF - reserved - 2-byte tag indication
//
//
//    }};

//    public static final String[] resetablePrefs = {
//            "cfg_sensors_pdm_reset",
//            "cfg_sensors_hrm_now"
//    };

    public static final List mappedOnClickPrefs = Arrays.asList(
            "cfg_sec_hand_tail_negative",
            "cfg_aaps_type",
            "cfg_digital_type",
            "cfg_digital_secs",
            "cfg_digital_jumbo_font",
            "cfg_alarms_warn_enable",
            "cfg_alarms_danger_enable",
            "cfg_alarms_sound_active_time"
    );

}
