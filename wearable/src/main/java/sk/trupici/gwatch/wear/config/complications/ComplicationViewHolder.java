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

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BackgroundChangeAware;
import sk.trupici.gwatch.wear.util.StringUtils;

import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BKG_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BORDER_COLOR;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_BORDER_SHAPE;
import static sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig.PREF_COMPL_DATA_COLOR;

public class ComplicationViewHolder extends WearableRecyclerView.ViewHolder implements BackgroundChangeAware {
    final private TextView title;
    final private WearableRecyclerView recyclerView;

    public ComplicationViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.page_title);
        recyclerView = itemView.findViewById(R.id.wearable_recycler_view);
    }

    @Override
    public void onBackgroundChanged() {
        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter instanceof BackgroundChangeAware) {
            ((BackgroundChangeAware) adapter).onBackgroundChanged();
        }
    }

    public TextView getTitle() {
        return title;
    }

    public WearableRecyclerView getRecyclerView() {
        return recyclerView;
    }

    public ShapeConfigItem createBorderTypeItem(Context context, String prefPrefix) {
        return new ShapeConfigItem(
                context.getString(R.string.config_item_border_shape_label),
                R.drawable.config_border_rect_24,
                StringUtils.notNullString(prefPrefix) + PREF_COMPL_BORDER_SHAPE,
                ConfigItem.Type.TYPE_BORDER_TYPE);
    }

    public ColorConfigItem createBorderColorItem(Context context, String prefPrefix) {
        return new ColorConfigItem(
                context.getString(R.string.config_item_border_color_label),
                R.drawable.config_color_edit,
                StringUtils.notNullString(prefPrefix) + PREF_COMPL_BORDER_COLOR,
                ConfigItem.Type.TYPE_BORDER_COLOR);
    }

    public ColorConfigItem createDataColorItem(Context context, String prefPrefix) {
        return new ColorConfigItem(
                context.getString(R.string.config_item_text_color_label),
                R.drawable.config_color_edit,
                StringUtils.notNullString(prefPrefix) + PREF_COMPL_DATA_COLOR,
                ConfigItem.Type.TYPE_DATA_COLOR);
    }

    public ColorConfigItem createBkgColorItem(Context context, String prefPrefix) {
        return new ColorConfigItem(
                context.getString(R.string.config_item_bkg_color_label),
                R.drawable.config_color_edit,
                StringUtils.notNullString(prefPrefix) + PREF_COMPL_BKG_COLOR,
                ConfigItem.Type.TYPE_BKG_COLOR);
    }
}
