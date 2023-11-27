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
import sk.trupici.gwatch.wear.components.BgPanel;
import sk.trupici.gwatch.wear.config.item.BasicConfigItem;
import sk.trupici.gwatch.wear.config.item.BoolConfigItem;
import sk.trupici.gwatch.wear.config.item.ConfigItem;
import sk.trupici.gwatch.wear.providers.BgDataProviderService;
import sk.trupici.gwatch.wear.util.CommonConstants;

public class BgPanelMenuItems {
    final public static ConfigItem[] items = {
            new BoolConfigItem(
                    R.string.config_item_is_unit_conversion,
                    CommonConstants.PREF_IS_UNIT_CONVERSION,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_bg_is_unit_conversion,
                    true),
            new BasicConfigItem(
                    R.string.config_item_bg_hypo_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_HYPO_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_hypo_color),
            new BasicConfigItem(
                    R.string.config_item_bg_low_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_LOW_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_low_color),
            new BasicConfigItem(
                    R.string.config_item_bg_in_range_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_IN_RANGE_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_in_range_color),
            new BasicConfigItem(
                    R.string.config_item_bg_high_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_HIGH_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_high_color),
            new BasicConfigItem(
                    R.string.config_item_bg_hyper_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_HYPER_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_hyper_color),
            new BasicConfigItem(
                    R.string.config_item_no_data_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_NO_DATA_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_no_data_color),
            new BasicConfigItem(
                    R.string.config_item_bkg_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_BKG_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_background_color),
            new BasicConfigItem(
                    R.string.config_item_border_shape_label,
                    R.drawable.config_border_rect_24,
                    BgPanel.PREF_BORDER_TYPE,
                    ConfigItem.Type.TYPE_BORDER_TYPE,
                    R.string.def_bg_border_type),
            new BasicConfigItem(
                    R.string.config_item_border_color_label,
                    R.drawable.config_color_edit_24,
                    BgPanel.PREF_BORDER_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_border_color),
            new BoolConfigItem(
                    R.string.config_item_swap_complication_text,
                    BgDataProviderService.PREF_SWAP_COMPLICATION_TEXT,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_bg_complication_swap_text,
                    true),
    };
}
