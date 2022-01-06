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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.annotation.NonNull;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BorderType;
import sk.trupici.gwatch.wear.config.complications.ComplicationConfig;
import sk.trupici.gwatch.wear.util.BorderUtils;
import sk.trupici.gwatch.wear.util.StringUtils;

public class ComplicationAttrs {

    private int dataColor;
    private int backgroundColor;
    private int borderColor;
    private BorderType borderType;
//    private int fontSize; // [sp]

    public ComplicationAttrs() {
        init();
    }

    public void init() {
        borderType = BorderType.NONE;
        borderColor = Color.TRANSPARENT;
        dataColor = Color.WHITE;
        backgroundColor = Color.TRANSPARENT;
//        fontSize = -1;
    }

    public void load(Context context, SharedPreferences prefs, String prefix) {
        Resources res = context.getResources();
        borderType = BorderType.getByNameOrDefault(prefs.getString(prefix + ComplicationConfig.PREF_COMPL_BORDER_SHAPE, context.getString(R.string.def_compl_border_type)));
        borderColor = prefs.getInt(prefix + ComplicationConfig.PREF_COMPL_BORDER_COLOR, res.getColor(R.color.def_compl_border_color, null));
        dataColor = prefs.getInt(prefix + ComplicationConfig.PREF_COMPL_DATA_COLOR, res.getColor(R.color.def_compl_data_color, null));
        backgroundColor = prefs.getInt(prefix + ComplicationConfig.PREF_COMPL_BKG_COLOR, res.getColor(R.color.def_compl_background_color, null));
//        fontSize = prefs.getInt(prefix + ComplicationConfig.PREF_COMPL_TEXT_SIZE, -1);
    }

    public int getBorderDrawableStyle() {
        return BorderUtils.getBorderDrawableStyle(borderType);
    }

    public boolean isBorderDotted() {
        return BorderUtils.isBorderDotted(borderType);
    }

    public boolean isBorderRounded() {
        return BorderUtils.isBorderRounded(borderType);
    }

    public boolean isBorderRing() {
        return BorderUtils.isBorderRing(borderType);
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

//    public int getFontSize() {
//        return fontSize;
//    }
//
//    public void setFontSize(int fontSize) {
//        this.fontSize = fontSize;
//    }

    @NonNull
    @Override
    public String toString() {
        return "\nData color: " + StringUtils.formatColorStr(dataColor)
                + "\nBackground color: " + StringUtils.formatColorStr((backgroundColor))
                + "\nBorder type: " + (borderType == null ? "null" : borderType.name())
                + "\nBorder color: " + StringUtils.formatColorStr(borderColor);
//                + "\nFont size: " + fontSize;
    }
}
