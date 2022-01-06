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

import sk.trupici.gwatch.wear.config.BorderType;

public class BorderConfigItem {

    final private BorderType id;
    final private int labelResourceId;
    final private int iconResourceId;

    public BorderConfigItem(BorderType id, int labelResourceId, int iconResourceId) {
        this.id = id;
        this.labelResourceId = labelResourceId;
        this.iconResourceId = iconResourceId;
    }

    public BorderType getId() {
        return id;
    }

    public int getLabelResourceId() {
        return labelResourceId;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }
}
