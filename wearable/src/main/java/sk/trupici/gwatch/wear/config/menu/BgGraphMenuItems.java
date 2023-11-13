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
import sk.trupici.gwatch.wear.components.BgGraphPanel;
import sk.trupici.gwatch.wear.config.item.BasicConfigItem;
import sk.trupici.gwatch.wear.config.item.BoolConfigItem;
import sk.trupici.gwatch.wear.config.item.ConfigItem;

public class BgGraphMenuItems {
    final public static ConfigItem[] items = {
            new BoolConfigItem(
                    R.string.config_item_graph_enable_dynamic_range,
                    BgGraphPanel.PREF_ENABLE_DYNAMIC_RANGE,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_graph_enable_dynamic_range),
            new BoolConfigItem(
                    R.string.config_item_graph_draw_dots,
                    BgGraphPanel.PREF_TYPE_DOTS,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_graph_type_draw_dots),
            new BoolConfigItem(
                    R.string.config_item_graph_draw_line,
                    BgGraphPanel.PREF_TYPE_LINE,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_graph_type_draw_line),
            new BasicConfigItem(
                    R.string.config_item_bg_hypo_color_label,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_HYPO_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_hypo_color),
            new BasicConfigItem(
                    R.string.config_item_bg_low_color_label,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_LOW_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_low_color),
            new BasicConfigItem(
                    R.string.config_item_bg_in_range_color_label,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_IN_RANGE_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_in_range_color),
            new BasicConfigItem(
                    R.string.config_item_bg_high_color_label,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_HIGH_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_high_color),
            new BasicConfigItem(
                    R.string.config_item_bg_hyper_color_label,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_HYPER_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_bg_hyper_color),
//                new BasicConfigItem(
//                        R.string.config_item_bkg_color_label,
//                        R.drawable.config_color_edit_24,
//                        SimpleBgChart.PREF_BKG_COLOR,
//                        ConfigItem.Type.TYPE_COLOR,
//                        R.color.def_bg_background_color),
            new BoolConfigItem(
                    R.string.config_item_graph_show_vert_lines,
                    BgGraphPanel.PREF_ENABLE_VERT_LINES,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_graph_enable_vert_lines),
            new BasicConfigItem(
                    R.string.config_item_graph_vert_lines_color,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_VERT_LINE_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_graph_color_vert_line),
            new BoolConfigItem(
                    R.string.config_item_graph_show_critical_lines,
                    BgGraphPanel.PREF_ENABLE_CRITICAL_LINES,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_graph_enable_critical_lines),
            new BasicConfigItem(
                    R.string.config_item_graph_critical_lines_color,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_CRITICAL_LINE_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_graph_color_critical_line),
            new BoolConfigItem(
                    R.string.config_item_graph_show_high_line,
                    BgGraphPanel.PREF_ENABLE_HIGH_LINE,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_graph_enable_high_line),
            new BasicConfigItem(
                    R.string.config_item_graph_high_line_color,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_HIGH_LINE_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_graph_color_high_line),
            new BoolConfigItem(
                    R.string.config_item_graph_show_low_line,
                    BgGraphPanel.PREF_ENABLE_LOW_LINE,
                    ConfigItem.Type.TYPE_SWITCH,
                    R.bool.def_graph_enable_low_line),
            new BasicConfigItem(
                    R.string.config_item_graph_low_line_color,
                    R.drawable.config_color_edit_24,
                    BgGraphPanel.PREF_LOW_LINE_COLOR,
                    ConfigItem.Type.TYPE_COLOR,
                    R.color.def_graph_color_low_line)
    };
}
