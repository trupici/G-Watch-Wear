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
import sk.trupici.gwatch.wear.components.DigitalTimePanel;
import sk.trupici.gwatch.wear.config.item.BasicConfigItem;
import sk.trupici.gwatch.wear.config.item.BoolConfigItem;
import sk.trupici.gwatch.wear.config.item.ConfigItem;

public class DigitalTimePanelMenuItems {
    final public static ConfigItem[] items = {
            new BoolConfigItem(
                    R.string.config_item_time_show_seconds,
                    DigitalTimePanel.PREF_SHOW_SECS,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_time_show_seconds),
            new BoolConfigItem(
                    R.string.config_item_time_24hr_time,
                    DigitalTimePanel.PREF_IS_24_HR_TIME,
                    ConfigItem.Type.TYPE_SWITCH,
                    -1),
            new BasicConfigItem(
                    R.string.config_item_text_color_label,
                    R.drawable.config_color_edit_24,
                    DigitalTimePanel.PREF_TEXT_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_time_text_color),
            new BasicConfigItem(
                    R.string.config_item_bkg_color_label,
                    R.drawable.config_color_edit_24,
                    DigitalTimePanel.PREF_BKG_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_time_background_color),
            new BasicConfigItem(
                    R.string.config_item_border_shape_label,
                    R.drawable.config_border_rect_24,
                    DigitalTimePanel.PREF_BORDER_TYPE,
                    ConfigItem.Type.TYPE_BORDER_TYPE,
                    R.string.def_time_border_type),
            new BasicConfigItem(
                    R.string.config_item_border_color_label,
                    R.drawable.config_color_edit_24,
                    DigitalTimePanel.PREF_BORDER_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_time_border_color)
    };
}
