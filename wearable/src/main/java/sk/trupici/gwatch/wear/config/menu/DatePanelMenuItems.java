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
import sk.trupici.gwatch.wear.components.DatePanel;
import sk.trupici.gwatch.wear.config.item.BasicConfigItem;
import sk.trupici.gwatch.wear.config.item.BoolConfigItem;
import sk.trupici.gwatch.wear.config.item.ConfigItem;
import sk.trupici.gwatch.wear.config.item.PaddingConfigItem;

public class DatePanelMenuItems {
    final public static ConfigItem[] items = {
            new BoolConfigItem(
                    R.string.config_item_date_show_month,
                    DatePanel.PREF_SHOW_MONTH,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_date_show_month),
            new BasicConfigItem(
                    R.string.config_item_date_day_of_month_color,
                    R.drawable.config_color_edit_24,
                    DatePanel.PREF_DAY_OF_MONTH_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_date_day_of_month_color),
            new BasicConfigItem(
                    R.string.config_item_date_day_of_week_color,
                    R.drawable.config_color_edit_24,
                    DatePanel.PREF_DAY_OF_WEEK_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_date_day_of_week_color),
            new BasicConfigItem(
                    R.string.config_item_date_month_color,
                    R.drawable.config_color_edit_24,
                    DatePanel.PREF_MONTH_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_date_month_color),
            new BasicConfigItem(
                    R.string.config_item_bkg_color_label,
                    R.drawable.config_color_edit_24,
                    DatePanel.PREF_BKG_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_date_background_color),
            new BasicConfigItem(
                    R.string.config_item_border_shape_label,
                    R.drawable.config_border_rect_24,
                    DatePanel.PREF_BORDER_TYPE,
                    ConfigItem.Type.TYPE_BORDER_TYPE,
                    R.string.def_date_border_type),
            new BasicConfigItem(
                    R.string.config_item_border_color_label,
                    R.drawable.config_color_edit_24,
                    DatePanel.PREF_BORDER_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_date_border_color),
            new PaddingConfigItem()
    };
}
