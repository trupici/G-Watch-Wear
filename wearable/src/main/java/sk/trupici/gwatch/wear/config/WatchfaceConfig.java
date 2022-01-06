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

public interface WatchfaceConfig {

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
     * Get configuration data of specified type at given position
     * @param type {@link ConfigPageData.ConfigType configuration data type}
     * @param index item index (view position)
     * @return
     */
    ConfigPageData.ConfigItemData getConfigItemData(ConfigPageData.ConfigType type, int index);

    /**
     * Get configuration data shared preference name
     * @param type {@link ConfigPageData.ConfigType configuration data type}
     * @return shared preference name assigned to this config data type
     */
    String getPrefName(ConfigPageData.ConfigType type);
}
