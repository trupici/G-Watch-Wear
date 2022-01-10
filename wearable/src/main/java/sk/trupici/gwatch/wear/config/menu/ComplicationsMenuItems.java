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
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfigItem;
import sk.trupici.gwatch.wear.config.item.BasicConfigItem;
import sk.trupici.gwatch.wear.config.item.ConfigItem;
import sk.trupici.gwatch.wear.config.item.PaddingConfigItem;

public class ComplicationsMenuItems {
    final public static ConfigItem[] items = {
            new ComplicationConfigItem(),
            new BasicConfigItem(
                    R.string.config_item_border_shape_label,
                    R.drawable.config_border_rect_24,
                    ComplicationConfig.PREF_COMPL_BORDER_SHAPE,
                    ConfigItem.Type.TYPE_BORDER_TYPE,
                    R.string.def_compl_border_type),
            new BasicConfigItem(
                    R.string.config_item_border_color_label,
                    R.drawable.config_color_edit_24,
                    ComplicationConfig.PREF_COMPL_BORDER_COLOR,
                    ConfigItem.Type.TYPE_BORDER_COLOR,
                    R.color.def_compl_border_color),
            new BasicConfigItem(
                    R.string.config_item_text_color_label,
                    R.drawable.config_color_edit_24,
                    ComplicationConfig.PREF_COMPL_DATA_COLOR,
                    ConfigItem.Type.TYPE_DATA_COLOR,
                    R.color.def_compl_data_color),
            new BasicConfigItem(
                    R.string.config_item_bkg_color_label,
                    R.drawable.config_color_edit_24,
                    ComplicationConfig.PREF_COMPL_BKG_COLOR,
                    ConfigItem.Type.TYPE_BKG_COLOR,
                    R.color.def_compl_background_color),
            new PaddingConfigItem()
    };
}
