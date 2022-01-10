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

package sk.trupici.gwatch.wear.config;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.R;

/**
 * ViewHolder holding a page with image list. It also supports layouts with background layer.
 */
public class ImageSetPageViewHolder extends WearableRecyclerView.ViewHolder implements BackgroundChangeAware {
    private final WatchfaceConfig watchfaceConfig;
    private final TextView title;
    private final ViewPager2 verticalPager;
    private final ViewGroup bkgViewGroup;
    private final ConfigPageData.ConfigType dataType;

    public ImageSetPageViewHolder(WatchfaceConfig watchfaceConfig, @NonNull View itemView, ConfigPageData.ConfigType dataType) {
        super(itemView);
        this.watchfaceConfig = watchfaceConfig;
        title = itemView.findViewById(R.id.page_title);
        verticalPager = itemView.findViewById(R.id.vertical_pager);
        bkgViewGroup = itemView.findViewById(R.id.backgrounds);
        this.dataType = dataType;
    }

    public TextView getTitle() {
        return title;
    }

    public ViewPager2 getVerticalPager() {
        return verticalPager;
    }

    public ConfigPageData.ConfigType getDataType() {
        return dataType;
    }

    @Override
    public void onBackgroundChanged() {
        if (dataType != ConfigPageData.ConfigType.BACKGROUND) {
            bkgViewGroup.removeAllViews();
            ConfigItemData itemData = watchfaceConfig.getSelectedItem(bkgViewGroup.getContext(), ConfigPageData.ConfigType.BACKGROUND);
            if (itemData != null && itemData.getResourceId() != 0) {
                ViewGroup view = (ViewGroup) LayoutInflater.from(bkgViewGroup.getContext()).inflate(R.layout.layout_config_item_page, bkgViewGroup, false);
                ImageView bkgView = view.findViewById(R.id.image);
                bkgView.setImageDrawable(bkgViewGroup.getContext().getDrawable(itemData.getResourceId()));
                bkgViewGroup.addView(view);
            }
        }
    }
}
