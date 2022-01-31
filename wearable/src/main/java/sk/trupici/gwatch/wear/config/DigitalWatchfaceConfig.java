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
import sk.trupici.gwatch.wear.components.DigitalTimePanel;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;
import sk.trupici.gwatch.wear.config.complications.ComplicationViewHolder;
import sk.trupici.gwatch.wear.config.complications.ComplicationsConfigAdapter;
import sk.trupici.gwatch.wear.config.complications.DigitalComplicationViewHolder;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

/**
 * Configuration data for Digital watch face configuration
 */
public class DigitalWatchfaceConfig implements WatchfaceConfig {

    final private static String PREF_PREFIX = "digital_";

    final public static int DEF_BACKGROUND_IDX = 0;

    private final static ConfigItemData[] backgroundConfig = new ConfigItemData[] {
            new ConfigItemData(0, "Stripes" , R.drawable.digital_background_default),
    };

    private final static ConfigPageData[] CONFIG = {
            new ConfigPageData(
                    ConfigPageData.ConfigType.BACKGROUND,
                    R.string.config_page_title_bkg
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
                    ConfigPageData.ConfigType.TIME_PANEL,
                    R.string.config_page_title_time_panel
            ),
    };

    private final static ComplicationConfig[] complicationConfig = {
            new ComplicationConfig(
                    ComplicationId.TOP_LEFT,
                    new int[] {
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_SHORT_TEXT,
                    }),
            new ComplicationConfig(
                    ComplicationId.TOP_RIGHT,
                    new int[] {
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_SHORT_TEXT,
                    }),
            new ComplicationConfig(
                    ComplicationId.BOTTOM_LEFT,
                    new int[] {
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_LONG_TEXT,
                    }),
            new ComplicationConfig(ComplicationId.BOTTOM_RIGHT,
                    new int[] {
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_LONG_TEXT,
                    })
    };

    final static int[] complicationIds;

    static {
        complicationIds = new int[complicationConfig.length];
        for (int i=0; i<complicationIds.length; i++) {
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
        View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_digital_complications_item, parent, false);
        return new DigitalComplicationViewHolder(this, adapter, layout);
    }

    @Override
    public ConfigItemData[] getItems(ConfigPageData.ConfigType type) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            return backgroundConfig;
        } else {
            return new ConfigItemData[]{};
        }
    }

    @Override
    public ConfigItemData getSelectedItem(Context context, ConfigPageData.ConfigType type) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            return backgroundConfig[getSelectedIdx(context, type)];
        } else {
            return null;
        }
    }

    @Override
    public int getSelectedIdx(Context context, ConfigPageData.ConfigType type) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            return PreferenceUtils.getIntValue(context, PREF_PREFIX + WatchfaceConfig.PREF_BACKGROUND_IDX, DEF_BACKGROUND_IDX);
        } else {
            return 0;
        }
    }

    @Override
    public void setSelectedIdx(Context context, ConfigPageData.ConfigType type, int idx) {
        if (type == ConfigPageData.ConfigType.BACKGROUND) {
            PreferenceUtils.setIntValue(context, PREF_PREFIX + WatchfaceConfig.PREF_BACKGROUND_IDX,
                    getValidIdx(backgroundConfig, idx, DEF_BACKGROUND_IDX));
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
        return ComplicationId.TOP_LEFT;
    }

    @Override
    public boolean getBoolPrefDefaultValue(Context context, String prefName) {
        return DigitalTimePanel.PREF_IS_24_HR_TIME.equals(prefName)
                ? DigitalTimePanel.getIs24HourFormatDefaultValue(context)
                : false;
    }

    @Override
    public RectF getBgPanelBounds(Context context) {
        return new RectF(
                context.getResources().getDimension(R.dimen.digital_layout_bg_pnel_left),
                context.getResources().getDimension(R.dimen.digital_layout_bg_panel_top),
                context.getResources().getDimension(R.dimen.digital_layout_bg_panel_right),
                context.getResources().getDimension(R.dimen.digital_layout_bg_panel_bottom)
        );
    }

    @Override
    public float getBgPanelTopOffset(Context context) {
        return context.getResources().getDimension(R.dimen.digital_layout_bg_panel_top_offset);
    }

    @Override
    public float getBgPanelBottomOffset(Context context) {
        return context.getResources().getDimension(R.dimen.digital_layout_bg_panel_bottom_offset);
    }

    @Override
    public boolean showBgPanelIndicator(Context context) {
        return context.getResources().getBoolean(R.bool.digital_layout_bg_panel_use_indicator);
    }

    @Override
    public RectF getBgPanelLowIndicatorBounds(Context context) {
        Resources res = context.getResources();
        return new RectF(
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_low_left),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_low_top),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_low_right),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_low_bottom)
        );
    }

    @Override
    public RectF getBgPanelInRangeIndicatorBounds(Context context) {
        Resources res = context.getResources();
        return new RectF(
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_in_range_left),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_in_range_top),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_in_range_right),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_in_range_bottom)
        );
    }

    @Override
    public RectF getBgPanelHighIndicatorBounds(Context context) {
        Resources res = context.getResources();
        return new RectF(
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_high_left),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_high_top),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_high_right),
                res.getDimension(R.dimen.digital_layout_bg_panel_indicator_high_bottom)
        );
    }

    @Override
    public RectF getBgGraphBounds(Context context) {
        return new RectF(
                context.getResources().getDimension(R.dimen.digital_layout_bg_graph_panel_left),
                context.getResources().getDimension(R.dimen.digital_layout_bg_graph_panel_top),
                context.getResources().getDimension(R.dimen.digital_layout_bg_graph_panel_right),
                context.getResources().getDimension(R.dimen.digital_layout_bg_graph_panel_bottom)
        );
    }

    @Override
    public Rect getBgGraphPadding(Context context) {
        return new Rect(
                context.getResources().getDimensionPixelOffset(R.dimen.digital_layout_bg_graph_left_padding),
                context.getResources().getDimensionPixelOffset(R.dimen.digital_layout_bg_graph_top_padding),
                context.getResources().getDimensionPixelOffset(R.dimen.digital_layout_bg_graph_right_padding),
                context.getResources().getDimensionPixelOffset(R.dimen.digital_layout_bg_graph_bottom_padding)
        );
    }
}
