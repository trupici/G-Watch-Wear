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

package sk.trupici.gwatch.wear.config;

import sk.trupici.gwatch.wear.R;

/**
 * Configuration data for Default analog watch face direct customization
 */
public class AnalogWatchfaceConfig implements WatchfaceConfig {

    final public static String PREF_PREFIX = "analog_";

    final public static String PREF_BACKGROUND_IDX = PREF_PREFIX + "background_idx";
    final public static String PREF_HANDS_SET_IDX = PREF_PREFIX + "hands_set_idx";

    final public static int DEF_BACKGROUND_IDX = 0;
    final public static int DEF_HANDS_SET_IDX = 0;

    private final static ConfigPageData[] CONFIG = {
            new ConfigPageData(
                    ConfigPageData.ConfigType.BACKGROUND,
                    new ConfigPageData.ConfigItemData[] {
                            new ConfigPageData.ConfigItemData(0, "Stripes" , R.drawable.analog_background_default),
                            new ConfigPageData.ConfigItemData(1, "Circuit Board", R.drawable.analog_background_1),
                            new ConfigPageData.ConfigItemData(2, "Classic Silver", R.drawable.analog_classic_background_1),
                            new ConfigPageData.ConfigItemData(3, "Classic Coral", R.drawable.analog_classic_background_2),
                    },
                    R.string.config_page_title_bkg
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.HANDS,
                    new ConfigPageData.ConfigItemData[] {
                            new ConfigPageData.HandsConfigData(0, "", R.drawable.analog_hands_preview_default,
                                    R.drawable.hours_default, R.drawable.hours_shadow_default,
                                    R.drawable.minutes_default, R.drawable.minutes_shadow_default,
                                    R.drawable.seconds_default, R.drawable.seconds_shadow_default),

                    },
                    R.string.config_page_title_hands
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.COMPLICATIONS,
                    new ConfigPageData.ConfigItemData[] {},
                    R.string.config_page_title_complications
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.ALARMS,
                    new ConfigPageData.ConfigItemData[] {},
                    R.string.config_page_title_alarms
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.BG_GRAPH,
                    new ConfigPageData.ConfigItemData[] {},
                    R.string.config_page_title_bg_graph
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.BG_PANEL,
                    new ConfigPageData.ConfigItemData[] {},
                    R.string.config_page_title_bg_panel
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.DATE_PANEL,
                    new ConfigPageData.ConfigItemData[] {},
                    R.string.config_page_title_date_panel
            ),
    };

    @Override
    public int getItemCount() {
        return CONFIG.length;
    }

    @Override
    public ConfigPageData getPageData(ConfigPageData.ConfigType type) {
        switch (type) {
            case BACKGROUND:
                return CONFIG[0];
            case HANDS:
                return CONFIG[1];
            case COMPLICATIONS:
                return CONFIG[2];
            case ALARMS:
                return CONFIG[3];
            case BG_GRAPH:
                return CONFIG[4];
            case BG_PANEL:
                return CONFIG[5];
            case DATE_PANEL:
                return CONFIG[6];
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public ConfigPageData getPageData(int index) {
        if (index < 0 || CONFIG.length <= index) {
            index = 0; // throw new IllegalArgumentException()?
        }
        return CONFIG[index];
    }

    private ConfigPageData.ConfigItemData getConfigItemData(ConfigPageData.ConfigItemData[] items, int index) {
        if (index < 0 || items.length <= index) {
            index = 0; // throw new IllegalArgumentException()?
        }
        return items[index];
    }

    @Override
    public ConfigPageData.ConfigItemData getConfigItemData(ConfigPageData.ConfigType type, int index) {
        switch (type) {
            case BACKGROUND:
                return getConfigItemData(CONFIG[0].getItems(), index);
            case HANDS:
                return getConfigItemData(CONFIG[1].getItems(), index);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String getPrefName(ConfigPageData.ConfigType type) {
        switch (type) {
            case BACKGROUND:
                return PREF_BACKGROUND_IDX;
            case HANDS:
                return PREF_HANDS_SET_IDX;
            default:
                throw new IllegalArgumentException();
        }
    }
}
