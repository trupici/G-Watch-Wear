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
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.ViewGroup;

import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.config.complications.ComplicationId;
import sk.trupici.gwatch.wear.config.complications.ComplicationViewHolder;
import sk.trupici.gwatch.wear.config.complications.ComplicationsConfigAdapter;

public interface WatchfaceConfig {

    String PREF_BACKGROUND_IDX = "background_idx";

    /**
     * Get number of configuration pages available in config
     * @return number of {@ConfigPageData configuration pages} available
     */
    int getItemCount();

    /**
     * Get configuration data for configuration page positioned in view
     * on specified index
     * @param index view position of the page. 0-based index
     * @return {@ConfigPageData} for config page on the specified view position
     */
    ConfigPageData getPageData(int index);

    /**
     * Get configuration data for configuration page of specified type
     * @param type {@link ConfigPageData.ConfigType configuration data type}
     * @return {@ConfigPageData} for config page of specified type
     */
    ConfigPageData getPageData(ConfigPageData.ConfigType type);

    /**
     * Get prefix of all preferences belonging to this configuration
     * @return prefix of all preferences belonging to this configuration
     */
    String getPrefsPrefix();

    /**
     * Creates {@code ComplicationViewHolder} for the watch face complication layout
     * @param adapter {@code Adapter} handling the new view holder
     * @param parent parent view group
     */
    ComplicationViewHolder createComplicationsViewHolder(ComplicationsConfigAdapter adapter, ViewGroup parent);

    /**
     * Get all items of specified type configured for this watch face
     * @param type item type
     * @return array of configured items or an empty arrays
     */
    ConfigItemData[] getItems(ConfigPageData.ConfigType type);

    /**
     * Get selected item of specified type
     * @param context context
     * @param type item type
     * @return selected item or an item with default index if nothing is selected
     * or null if there is no item of such type configured
     */
    ConfigItemData getSelectedItem(Context context, ConfigPageData.ConfigType type);

    /**
     * Get the selected index to array of items of specified type
     * @param context context
     * @param type item type
     * @return index of selected item or default index if nothing is selected
     */
    int getSelectedIdx(Context context, ConfigPageData.ConfigType type);

    /**
     * set the selected index to array of items of specified type
     * @param context context
     * @param type item type
     * @param idx selected item index to set
     */
    void setSelectedIdx(Context context, ConfigPageData.ConfigType type, int idx);

    /**
     * @return array of configured complication ids or empty array if no complication is configured
     */
    int[] getComplicationIds();

    /**
     * @return {@code ComplicationConfig} for the complication with specified id
     */
    ComplicationConfig getComplicationConfig(ComplicationId id);

    /**
     * @return array of all {@code ComplicationConfig}s configured for this watch face
     */
    ComplicationConfig[] getComplicationConfigs();

    /**
     * @return id of complication to preselect on configuration page
     */
    ComplicationId getDefaultComplicationId();

    /**
     * Get specific boolean preference default value, e.g. dependant on context
     * @param prefName preference name
     * @return specific boolean preference default value
     */
    boolean getBoolPrefDefaultValue(Context context, String prefName);

    /**
     * Get watch face specific BG Panel layout attributes
     */
    RectF getBgPanelBounds(Context context);
    float getBgPanelTopOffset(Context context);

    /**
     * Get watch face specific BG Graph layout attributes
     */
    RectF getBgGraphBounds(Context context);
    Rect getBgGraphPadding(Context context);
}
