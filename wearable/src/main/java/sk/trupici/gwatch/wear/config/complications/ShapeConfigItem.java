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

package sk.trupici.gwatch.wear.config.complications;

import android.app.Activity;

/**
 * Data for shape picker item in RecyclerView.
 */
public class ShapeConfigItem implements ConfigItem {

    final private String label;
    final private int iconResourceId;
    final private String sharedPrefString;
    final private Class<? extends Activity> activityToChoosePreference;
    final private Type type;

    public ShapeConfigItem(String label, int iconResourceId, String sharedPrefString, Class<? extends Activity> activity, Type type) {
        this.label = label;
        this.iconResourceId = iconResourceId;
        this.sharedPrefString = sharedPrefString;
        this.activityToChoosePreference = activity;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public String getSharedPrefString() {
        return sharedPrefString;
    }

    public Class<? extends Activity> getActivityToChoosePreference() {
        return activityToChoosePreference;
    }

    @Override
    public Type getConfigType() {
        return type;
    }
}
