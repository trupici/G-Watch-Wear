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

package sk.trupici.gwatch.wear.config.item;

/**
 * Data for color picker item in RecyclerView.
 */
public class BasicConfigItem implements ConfigItem {

    final private int labelResourceId;
    final private int iconResourceId;
    final private String preferenceName;
    final private Type type;
    final private int defaultValueResourceId;
    final private boolean isGlobal;

    public BasicConfigItem(int labelResourceId, int iconResourceId, String preferenceName, Type type, int defaultValueResourceId) {
        this(labelResourceId, iconResourceId, preferenceName, type, defaultValueResourceId, false);
    }

    public BasicConfigItem(int labelResourceId, int iconResourceId, String preferenceName, Type type, int defaultValueResourceId, boolean isGlobal) {
        this.labelResourceId = labelResourceId;
        this.iconResourceId = iconResourceId;
        this.preferenceName = preferenceName;
        this.type = type;
        this.defaultValueResourceId = defaultValueResourceId;
        this.isGlobal = isGlobal;
    }

    public int getLabelResourceId() {
        return labelResourceId;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public String getPreferenceName() {
        return preferenceName;
    }

    @Override
    public Type getConfigType() {
        return type;
    }

    public int getDefaultValueResourceId() {
        return defaultValueResourceId;
    }

    public boolean isGlobal() {
        return isGlobal;
    }
}
