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

package sk.trupici.gwatch.wear.util;

import android.support.wearable.complications.rendering.ComplicationDrawable;

import sk.trupici.gwatch.wear.config.complications.BorderType;

public class BorderUtils {

    final public static int BORDER_WIDTH = 2;
    final public static float BORDER_DASH_LEN = 6f;
    final public static float BORDER_GAP_LEN = 2f;
    final public static float BORDER_DOT_LEN = BORDER_WIDTH;
    final public static float BORDER_ROUND_RECT_RADIUS = 15f;
    final public static float BORDER_RING_RADIUS = 100f;

    public static int getBorderDrawableStyle(BorderType borderType) {
        switch (borderType) {
            case RECT:
            case ROUNDED_RECT:
            case RING:
                return ComplicationDrawable.BORDER_STYLE_SOLID;
            case DASHED_RECT:
            case DASHED_ROUNDED_RECT:
            case DASHED_RING:
            case DOTTED_RECT:
            case DOTTED_ROUNDED_RECT:
            case DOTTED_RING:
                return ComplicationDrawable.BORDER_STYLE_DASHED;
            default:
                return ComplicationDrawable.BORDER_STYLE_NONE;
        }
    }

    public static boolean isBorderDotted(BorderType borderType) {
        switch (borderType) {
            case DOTTED_RECT:
            case DOTTED_ROUNDED_RECT:
            case DOTTED_RING:
                return true;
            default:
                return false;
        }
    }

    public static boolean isBorderRounded(BorderType borderType) {
        switch (borderType) {
            case ROUNDED_RECT:
            case DOTTED_ROUNDED_RECT:
            case DASHED_ROUNDED_RECT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isBorderRing(BorderType borderType) {
        switch (borderType) {
            case RING:
            case DOTTED_RING:
            case DASHED_RING:
                return true;
            default:
                return false;
        }
    }
}
