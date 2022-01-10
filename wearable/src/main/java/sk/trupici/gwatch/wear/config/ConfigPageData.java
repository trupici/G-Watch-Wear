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

import java.util.Arrays;

public class ConfigPageData {

    public enum ConfigType {
        BACKGROUND,
        HANDS,
        COMPLICATIONS,
        BG_PANEL,
        BG_GRAPH,
        DATE_PANEL,
        ALARMS,
        TIME_PANEL
        ;

        public static ConfigType valueOf(int ordinal) {
            return Arrays.stream(values()).filter(x -> x.ordinal() == ordinal).findFirst().orElseThrow(IllegalArgumentException::new);
        }
    }

    final private ConfigType type;
    final private int titleId;

    public ConfigPageData(ConfigType type, int titleId) {
        this.type = type;
        this.titleId = titleId;
    }

    public ConfigType getType() {
        return type;
    }

    public int getTitleId() {
        return titleId;
    }


}
