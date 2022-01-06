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

package sk.trupici.gwatch.wear.config.menu;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.BgAlarmController;
import sk.trupici.gwatch.wear.config.item.BoolConfigItem;
import sk.trupici.gwatch.wear.config.item.ConfigItem;
import sk.trupici.gwatch.wear.config.item.PaddingConfigItem;

public class AlarmsMenuItems {
    final public static ConfigItem[] items = {
            new BoolConfigItem(
                    R.string.config_item_alarms_enable,
                    BgAlarmController.PREF_ENABLE_ALARMS,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_time_range,
                    BgAlarmController.PREF_ENABLE_TIME_RANGE,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_time_range_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_sound,
                    BgAlarmController.PREF_ENABLE_SOUND,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_sound_time_range,
                    BgAlarmController.PREF_ENABLE_SOUND_TIME_RANGE,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_time_range_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_hyper_alarm,
                    BgAlarmController.PREF_HYPER_ALARM_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_hyper_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_hyper_sound,
                    BgAlarmController.PREF_HYPER_ALARM_SOUND_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_high_alarm,
                    BgAlarmController.PREF_HIGH_ALARM_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_high_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_high_sound,
                    BgAlarmController.PREF_HIGH_ALARM_SOUND_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_low_alarm,
                    BgAlarmController.PREF_LOW_ALARM_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_low_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_low_sound,
                    BgAlarmController.PREF_LOW_ALARM_SOUND_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_hypo_alarm,
                    BgAlarmController.PREF_HYPO_ALARM_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_hypo_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_hypo_sound,
                    BgAlarmController.PREF_HYPO_ALARM_SOUND_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_fast_drop_alarm,
                    BgAlarmController.PREF_FAST_DROP_ALARM_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_fats_drop_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_fast_drop_sound,
                    BgAlarmController.PREF_FAST_DROP_ALARM_SOUND_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_no_data_alarm,
                    BgAlarmController.PREF_NO_DATA_ALARM_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_no_data_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_no_data_sound,
                    BgAlarmController.PREF_NO_DATA_ALARM_SOUND_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_alarms_sound_enabled),
            new BoolConfigItem(
                    R.string.config_item_alarms_enable_new_data_notifications,
                    BgAlarmController.PREF_NEW_VALUE_NOTIFICATION_ENABLED,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_notification_new_value_enabled),
            new PaddingConfigItem()
    };
}
