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

import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.ConfigPageData;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

/**
 * Displays Analog watch face preview with complication locations.
 */
public class AnalogComplicationViewHolder extends ComplicationViewHolder {

    private final static String LOG_TAG = AnalogComplicationViewHolder.class.getSimpleName();

    private ComplicationViews left;
    private ComplicationViews right;

    public AnalogComplicationViewHolder(WatchfaceConfig watchfaceConfig, ComplicationsConfigAdapter complicationAdapter, final View view) {
        super(watchfaceConfig, complicationAdapter, view);
    }

    @Override
    protected void initComplicationViews(View view) {
        left = new ComplicationViews(view.findViewById(R.id.left_complication), this, this);
        right = new ComplicationViews(view.findViewById(R.id.right_complication), this, this);
    }

    @Override
    protected ComplicationViews getComplicationViews(ComplicationId complicationId) {
        switch (complicationId) {
            case LEFT:
                return left;
            case RIGHT:
                return right;
            default:
                return null;
        }
    }

    @Override
    protected ComplicationId getComplicationId(View button) {
        if (button.equals(left.complication)) {
            return ComplicationId.LEFT;
        } else if (button.equals(right.complication)) {
            return ComplicationId.RIGHT;
        } else {
            return null;
        }
    }
}
