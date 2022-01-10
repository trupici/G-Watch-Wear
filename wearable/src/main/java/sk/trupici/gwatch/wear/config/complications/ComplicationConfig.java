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

import androidx.annotation.NonNull;

public class ComplicationConfig {

    final public static String PREF_COMPL_BORDER_SHAPE = "border_shape";
    final public static String PREF_COMPL_BORDER_COLOR = "border_color";
    final public static String PREF_COMPL_DATA_COLOR = "data_color";
    final public static String PREF_COMPL_BKG_COLOR = "bkg_color";
    final public static String PREF_COMPL_TEXT_SIZE = "text_size";

    final public static String LEFT_PREFIX = "left_";
    final public static String RIGHT_PREFIX = "right_";
    final public static String TOP_LEFT_PREFIX = "top_left_";
    final public static String TOP_RIGHT_PREFIX = "top_right_";
    final public static String BOTTOM_LEFT_PREFIX = "bottom_left_";
    final public static String BOTTOM_RIGHT_PREFIX = "bottom_right_";
    final public static String TOP_PREFIX = "top_";
    final public static String CENTER_PREFIX = "center_";
    final public static String BOTTOM_PREFIX = "bottom_";

    final public static String NO_DATA_TEXT = "--";

    private final ComplicationId id;
    private final int[] supportedTypes;
    private ComplicationDrawable complicationDrawable;

    public static String getComplicationPrefix(ComplicationId complicationId) {
        switch (complicationId) {
            case LEFT:
                return LEFT_PREFIX;
            case RIGHT:
                return RIGHT_PREFIX;
            case TOP_LEFT:
                return TOP_LEFT_PREFIX;
            case TOP_RIGHT:
                return TOP_RIGHT_PREFIX;
            case BOTTOM_LEFT:
                return BOTTOM_LEFT_PREFIX;
            case BOTTOM_RIGHT:
                return BOTTOM_RIGHT_PREFIX;
            case TOP:
                return TOP_PREFIX;
            case CENTER:
                return CENTER_PREFIX;
            case BOTTOM:
                return BOTTOM_PREFIX;
            default:
                return "";
        }
    }

    public ComplicationConfig(ComplicationId id, @NonNull int[] supportedTypes) {
        this.id = id;
        this.supportedTypes = supportedTypes;
    }

    public int getId() {
        return id.ordinal();
    }

    public ComplicationId getComplicationId() {
        return id;
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
