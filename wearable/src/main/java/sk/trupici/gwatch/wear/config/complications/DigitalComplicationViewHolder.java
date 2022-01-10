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

import android.view.View;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;

/**
 * Displays Digital watch face preview with complication locations.
 */
public class DigitalComplicationViewHolder extends ComplicationViewHolder {

    private final static String LOG_TAG = DigitalComplicationViewHolder.class.getSimpleName();

    private ComplicationViews top_left;
    private ComplicationViews top_right;
    private ComplicationViews bottom_left;
    private ComplicationViews bottom_right;

    public DigitalComplicationViewHolder(WatchfaceConfig watchfaceConfig, ComplicationsConfigAdapter complicationAdapter, final View view) {
        super(watchfaceConfig, complicationAdapter, view);
    }

    @Override
    protected void initComplicationViews(View view) {
        top_left = new ComplicationViews(view.findViewById(R.id.top_left_complication), this, this);
        top_right = new ComplicationViews(view.findViewById(R.id.top_right_complication), this, this);
        bottom_left = new ComplicationViews(view.findViewById(R.id.bottom_left_complication), this, this);
        bottom_right = new ComplicationViews(view.findViewById(R.id.bottom_right_complication), this, this);
    }

    @Override
    protected ComplicationViews getComplicationViews(ComplicationId complicationId) {
        switch (complicationId) {
            case TOP_LEFT:
                return top_left;
            case TOP_RIGHT:
                return top_right;
            case BOTTOM_LEFT:
                return bottom_left;
            case BOTTOM_RIGHT:
                return bottom_right;
            default:
                return null;
        }
    }

    @Override
    protected ComplicationId getComplicationId(View button) {
        if (button.equals(top_left.complication)) {
            return ComplicationId.TOP_LEFT;
        } else if (button.equals(top_right.complication)) {
            return ComplicationId.TOP_RIGHT;
        } else if (button.equals(bottom_left.complication)) {
            return ComplicationId.BOTTOM_LEFT;
        } else if (button.equals(bottom_right.complication)) {
            return ComplicationId.BOTTOM_RIGHT;
        } else {
            return null;
        }
    }
}
