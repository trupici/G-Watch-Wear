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

package sk.trupici.gwatch.wear.components;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.wearable.complications.rendering.ComplicationDrawable;

import androidx.annotation.NonNull;
import sk.trupici.gwatch.wear.config.complications.BorderType;
import sk.trupici.gwatch.wear.util.StringUtils;

import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BKG_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BORDER_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BORDER_SHAPE;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_DATA_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_TEXT_SIZE;

public class ComponentsConfig {
    private int dataColor;
    private int backgroundColor;
    private int borderColor;
    private BorderType borderType;
    private int fontSize; // [sp]

    public ComponentsConfig() {
        init();
    }

    public void init() {
        borderType = BorderType.NONE;
        borderColor = Color.TRANSPARENT;
        dataColor = Color.WHITE;
        backgroundColor = Color.TRANSPARENT;
        fontSize = -1;
    }

    public void load(SharedPreferences prefs, String prefix) {
        borderType = BorderType.getByNameOrDefault(prefs.getString(prefix + PREF_COMPL_BORDER_SHAPE, null));
        borderColor = prefs.getInt(prefix + PREF_COMPL_BORDER_COLOR, Color.TRANSPARENT);
        dataColor = prefs.getInt(prefix + PREF_COMPL_DATA_COLOR, Color.WHITE);
        backgroundColor = prefs.getInt(prefix + PREF_COMPL_BKG_COLOR, Color.TRANSPARENT);
        fontSize = prefs.getInt(prefix + PREF_COMPL_TEXT_SIZE, -1);
    }

    public int getBorderDrawableStyle() {
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

    public boolean isBorderDotted() {
        switch (borderType) {
            case DOTTED_RECT:
            case DOTTED_ROUNDED_RECT:
            case DOTTED_RING:
                return true;
            default:
                return false;
        }
    }

    public boolean isBorderRounded() {
        switch (borderType) {
            case ROUNDED_RECT:
            case DOTTED_ROUNDED_RECT:
            case DASHED_ROUNDED_RECT:
                return true;
            default:
                return false;
        }
    }

    public boolean isBorderRing() {
        switch (borderType) {
            case RING:
            case DOTTED_RING:
            case DASHED_RING:
                return true;
            default:
                return false;
        }
    }

    public int getDataColor() {
        return dataColor;
    }

    public void setDataColor(int dataColor) {
        this.dataColor = dataColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public BorderType getBorderType() {
        return borderType;
    }

    public void setBorderType(BorderType borderType) {
        this.borderType = borderType;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    @NonNull
    @Override
    public String toString() {
        return "\nData color: " + StringUtils.formatColorStr(dataColor)
                + "\nBackground color: " + StringUtils.formatColorStr((backgroundColor))
                + "\nBorder type: " + (borderType == null ? "null" : borderType.name())
                + "\nBorder color: " + StringUtils.formatColorStr(borderColor)
                + "\nFont size: " + fontSize;
    }
}
