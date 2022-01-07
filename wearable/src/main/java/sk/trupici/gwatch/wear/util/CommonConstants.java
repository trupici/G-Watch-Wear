/*
 * Copyright (C) 2021 Juraj Antal
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

package sk.trupici.gwatch.wear.util;

import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;

public interface CommonConstants {
    String BG_RECEIVER_ACTION = "sk.trupici.gwatch.wear.BG_RECEIVER_ACTION";

    long SECOND_IN_MILLIS = 1000L;
    int MINUTE_IN_SECONDS = 60;
    long MINUTE_IN_MILLIS = 60000L; // 60 * 1000
    int HOUR_IN_MINUTES = 60;
    long HOUR_IN_MILLIS = 3600000L; // 60 * 60 * 1000
    int DAY_IN_MINUTES = 1440; // 24 * 60
    long DAY_IN_MILLIS = 86400000L; // 24 * 60 * 60 * 1000

    String PREF_IS_UNIT_CONVERSION = AnalogWatchfaceConfig.PREF_PREFIX + "bg_is_unit_conversion";

    String PREF_HYPER_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_hyper";
    String PREF_HIGH_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_high";
    String PREF_LOW_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_low";
    String PREF_HYPO_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_hypo";
    String PREF_NO_DATA_THRESHOLD = AnalogWatchfaceConfig.PREF_PREFIX + "bg_threshold_no_data";

    String PREF_CONFIG_CHANGED = AnalogWatchfaceConfig.PREF_PREFIX + "config_changed";

}
