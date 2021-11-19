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

import android.support.wearable.complications.rendering.ComplicationDrawable;

import org.jetbrains.annotations.NotNull;

public class ComplicationConfig {

    final public static String LEFT_PREFIX = "left_";
    final public static String RIGHT_PREFIX = "right_";
    final public static String CENTER_PREFIX = "center_";
    final public static String BOTTOM_PREFIX = "bottom_";
    final public static String TOP_PREFIX = "top_";

    final public static String NO_DATA_TEXT = "--";

    private final ComplicationId id;
    private final ComplicationLocation location;
    private final int[] supportedTypes;
    private ComplicationDrawable complicationDrawable;

    public ComplicationConfig(ComplicationId id, ComplicationLocation location, @NotNull int[] supportedTypes) {
        this.id = id;
        this.location = location;
        this.supportedTypes = supportedTypes;
    }

    public int getId() {
        return id.ordinal();
    }

    public ComplicationId getComplicationId() {
        return id;
    }

    public ComplicationLocation getLocation() {
        return location;
    }

    public int[] getSupportedTypes() {
        return supportedTypes;
    }

    public void setComplicationDrawable(ComplicationDrawable complicationDrawable) {
        this.complicationDrawable = complicationDrawable;
    }

    public ComplicationDrawable getComplicationDrawable() {
        return complicationDrawable;
    }
}
