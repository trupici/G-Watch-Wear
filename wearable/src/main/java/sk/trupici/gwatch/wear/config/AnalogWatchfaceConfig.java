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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.wearable.complications.ComplicationData;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.common.util.PreferenceUtils;
import sk.trupici.gwatch.wear.config.complications.AnalogComplicationViewHolder;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;
import sk.trupici.gwatch.wear.config.complications.ComplicationViewHolder;
import sk.trupici.gwatch.wear.config.complications.ComplicationsConfigAdapter;

/**
 * Configuration data for Analog watch face configuration
 */
public class AnalogWatchfaceConfig implements WatchfaceConfig {

    final private static String PREF_PREFIX = "analog_";

    final public static String PREF_HANDS_SET_IDX = PREF_PREFIX + "hands_set_idx";

    final public static int DEF_BACKGROUND_IDX = 0;
    final public static int DEF_HANDS_SET_IDX = 0;

    private final static ConfigItemData[] backgroundConfig = new ConfigItemData[]{
            new ConfigItemData(0, "Stripes", R.drawable.analog__active_background_default),
            new ConfigItemData(1, "Circuit Board", R.drawable.analog_active_background_1),
            new ConfigItemData(2, "Halftone", R.drawable.analog_active_background_2),
            new ConfigItemData(3, "Classic Silver", R.drawable.analog_classic_background_1),
            new ConfigItemData(4, "Classic Coral", R.drawable.analog_classic_background_2),
    };

    private final static ConfigItemData[] handsConfig = new ConfigItemData[]{
            new HandsConfigData(0, "", R.drawable.hands_default_preview,
                    R.drawable.hours_default, R.drawable.hours_shadow_default,
                    R.drawable.minutes_default, R.drawable.minutes_shadow_default,
                    R.drawable.seconds_default, R.drawable.seconds_shadow_default),
            new HandsConfigData(1, "", R.drawable.hands_1_preview,
                    R.drawable.hours_default, R.drawable.hours_shadow_default,
                    R.drawable.minutes_default, R.drawable.minutes_shadow_default,
                    R.drawable.seconds_1, R.drawable.seconds_shadow_default),
            new HandsConfigData(2, "", R.drawable.hands_classic_preview,
                    R.drawable.hours_classic, R.drawable.hours_classic_shadow,
                    R.drawable.minutes_classic, R.drawable.minutes_classic_shadow,
                    R.drawable.seconds_classic, R.drawable.seconds_classic_shadow),
    };

    private final static ConfigPageData[] CONFIG = {
            new ConfigPageData(
                    ConfigPageData.ConfigType.BACKGROUND,
                    R.string.config_page_title_bkg
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.HANDS,
                    R.string.config_page_title_hands
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.COMPLICATIONS,
                    R.string.config_page_title_complications
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.ALARMS,
                    R.string.config_page_title_alarms
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.BG_GRAPH,
                    R.string.config_page_title_bg_graph
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.BG_PANEL,
                    R.string.config_page_title_bg_panel
            ),
            new ConfigPageData(
                    ConfigPageData.ConfigType.DATE_PANEL,
                    R.string.config_page_title_date_panel
            ),
    };


    private final static ComplicationConfig[] complicationConfig = {
            new ComplicationConfig(
                    ComplicationId.LEFT,
                    new int[]{
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_LONG_TEXT,
                    }),
            new ComplicationConfig(
                    ComplicationId.RIGHT,
                    new int[]{
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_LONG_TEXT,
                    })
    };

    final static int[] complicationIds;

    static {
        complicationIds = new int[complicationConfig.length];
        for (int i = 0; i < complicationIds.length; i++) {
            complicationIds[i] = complicationConfig[i].getId();
        }
    }


    @Override
    public String getPrefsPrefix() {
        return PREF_PREFIX;
    }

    @Override
    public int getItemCount() {
        return CONFIG.length;
    }


    @Override
    public ConfigPageData getPageData(ConfigPageData.ConfigType type) {
        for (ConfigPageData pageData : CONFIG) {
            if (pageData.getType() == type) {
                return pageData;
            }
        }

        throw new IllegalArgumentException("Invalid type: " + type);
    }

    @Override
    public ConfigPageData getPageData(int index) {
        if (index < 0 || CONFIG.length <= index) {
            index = 0; // throw new IllegalArgumentException()?
        }
        return CONFIG[index];
    }

    @Override
    public ComplicationViewHolder createComplicationsViewHolder(ComplicationsConfigAdapter adapter, ViewGroup parent) {
        View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_analog_complications_item, parent, false);
        return new AnalogComplicationViewHolder(this, adapter, layout);
    }

    @Override
    public ConfigItemData[] getItems(ConfigPageData.ConfigType type) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            return backgroundConfig;
        } else if (type == ConfigPageData.ConfigType.HANDS) {
            return handsConfig;
        } else {
            return new ConfigItemData[]{};
        }
    }

    @Override
    public ConfigItemData getSelectedItem(Context context, ConfigPageData.ConfigType type) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            return backgroundConfig[getSelectedIdx(context, type)];
        } else if (type == ConfigPageData.ConfigType.HANDS) {
            return handsConfig[getSelectedIdx(context, type)];
        } else {
            return null;
        }
    }

    @Override
    public int getSelectedIdx(Context context, ConfigPageData.ConfigType type) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            return getValidIdx(backgroundConfig,
                    PreferenceUtils.getIntValue(context, PREF_PREFIX + WatchfaceConfig.PREF_BACKGROUND_IDX, DEF_BACKGROUND_IDX),
                    DEF_BACKGROUND_IDX);
        } else if (type == ConfigPageData.ConfigType.HANDS) {
            return getValidIdx(handsConfig,
                    PreferenceUtils.getIntValue(context, PREF_HANDS_SET_IDX, DEF_HANDS_SET_IDX),
                    DEF_HANDS_SET_IDX);
        } else {
            return 0;
        }
    }

    @Override
    public void setSelectedIdx(Context context, ConfigPageData.ConfigType type, int idx) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            PreferenceUtils.setIntValue(context, PREF_PREFIX + WatchfaceConfig.PREF_BACKGROUND_IDX,
                    getValidIdx(backgroundConfig, idx, DEF_BACKGROUND_IDX));
        } else if (type == ConfigPageData.ConfigType.HANDS) {
            PreferenceUtils.setIntValue(context, PREF_HANDS_SET_IDX,
                    getValidIdx(handsConfig, idx, DEF_HANDS_SET_IDX));
        }
    }

    private int getValidIdx(ConfigItemData[] items, int index, int defIndex) {
        return (index < 0 || items.length <= index) ? defIndex : index;
    }

    @Override
    public int[] getComplicationIds() {
        return complicationIds;
    }

    @Override
    public ComplicationConfig[] getComplicationConfigs() {
        return complicationConfig;
    }

    @Override
    public ComplicationConfig getComplicationConfig(ComplicationId id) {
        for (ComplicationConfig cc : complicationConfig) {
            if (cc.getComplicationId() == id) {
                return cc;
            }
        }
        return null;
    }

    @Override
    public ComplicationId getDefaultComplicationId() {
        return ComplicationId.LEFT;
    }

    @Override
    public boolean getBoolPrefDefaultValue(Context context, String prefName) {
        return false;
    }

    @Override
    public RectF getBgPanelBounds(Context context) {
        return new RectF(
                context.getResources().getDimension(R.dimen.analog_layout_bg_panel_left),
                context.getResources().getDimension(R.dimen.analog_layout_bg_panel_top),
                context.getResources().getDimension(R.dimen.analog_layout_bg_panel_right),
                context.getResources().getDimension(R.dimen.analog_layout_bg_panel_bottom)
        );
    }

    @Override
    public float getBgPanelTopOffset(Context context) {
        return context.getResources().getDimension(R.dimen.analog_layout_bg_panel_top_offset);
    }

    @Override
    public float getBgPanelBottomOffset(Context context) {
        return context.getResources().getDimension(R.dimen.analog_layout_bg_panel_bottom_offset);
    }

    @Override
    public boolean showBgPanelIndicator(Context context) {
        return context.getResources().getBoolean(R.bool.analog_layout_bg_panel_use_indicator);
    }

    @Override
    public RectF getBgPanelLowIndicatorBounds(Context context) {
        Resources res = context.getResources();
        return new RectF(
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_low_left),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_low_top),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_low_right),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_low_bottom)
        );
    }

    @Override
    public RectF getBgPanelInRangeIndicatorBounds(Context context) {
        Resources res = context.getResources();
        return new RectF(
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_in_range_left),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_in_range_top),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_in_range_right),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_in_range_bottom)
        );
    }

    @Override
    public RectF getBgPanelHighIndicatorBounds(Context context) {
        Resources res = context.getResources();
        return new RectF(
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_high_left),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_high_top),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_high_right),
                res.getDimension(R.dimen.analog_layout_bg_panel_indicator_high_bottom)
        );
    }

    @Override
    public RectF getBgGraphBounds(Context context) {
        return new RectF(
                context.getResources().getDimension(R.dimen.analog_layout_bg_graph_panel_left),
                context.getResources().getDimension(R.dimen.analog_layout_bg_graph_panel_top),
                context.getResources().getDimension(R.dimen.analog_layout_bg_graph_panel_right),
                context.getResources().getDimension(R.dimen.analog_layout_bg_graph_panel_bottom)
        );
    }

    @Override
    public Rect getBgGraphPadding(Context context) {
        return new Rect(
                context.getResources().getDimensionPixelOffset(R.dimen.analog_layout_bg_graph_left_padding),
                context.getResources().getDimensionPixelOffset(R.dimen.analog_layout_bg_graph_top_padding),
                context.getResources().getDimensionPixelOffset(R.dimen.analog_layout_bg_graph_right_padding),
                context.getResources().getDimensionPixelOffset(R.dimen.analog_layout_bg_graph_bottom_padding)
        );
    }
}
