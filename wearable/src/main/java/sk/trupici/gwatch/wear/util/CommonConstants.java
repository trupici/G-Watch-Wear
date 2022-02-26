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

public interface CommonConstants {
    String BG_RECEIVER_ACTION = "sk.trupici.gwatch.wear.BG_RECEIVER_ACTION";
    String REMOTE_CONFIG_ACTION = "sk.trupici.gwatch.wear.REMOTE_CONFIG_ACTION";

    String PREF_IS_UNIT_CONVERSION = "bg_is_unit_conversion";

    String PREF_HYPER_THRESHOLD = "bg_threshold_hyper";
    String PREF_HIGH_THRESHOLD = "bg_threshold_high";
    String PREF_LOW_THRESHOLD = "bg_threshold_low";
    String PREF_HYPO_THRESHOLD = "bg_threshold_hypo";
    String PREF_NO_DATA_THRESHOLD = "bg_threshold_no_data";

    int COMPLICATION_CONFIG_REQUEST_CODE = 101;
    int UPDATE_COLORS_CONFIG_REQUEST_CODE = 102;
    int BORDER_TYPE_CONFIG_REQUEST_CODE = 103;
}
