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

import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.R;

public class WatchFaceViewHolder extends WearableRecyclerView.ViewHolder implements BackgroundChangeAware {
    private final MainConfigViewAdapter parentViewAdapter;
    private final TextView title;
    private final ViewPager2 verticalPager;
    private final ViewGroup bkgViewGroup;
    private final ConfigPageData.ConfigType dataType;

    public WatchFaceViewHolder(MainConfigViewAdapter parentViewAdapter, @NonNull View itemView, ConfigPageData.ConfigType dataType) {
        super(itemView);
        this.parentViewAdapter = parentViewAdapter;
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

    public ViewGroup getBkgViewGroup() {
        return bkgViewGroup;
    }

    @Override
    public void onBackgroundChanged() {
        List<Integer> resourceIds = parentViewAdapter.getBackgroundResourceIds(dataType);
        if (resourceIds.size() > 0) {
            bkgViewGroup.removeAllViews();
            for (int resourceId : resourceIds) {
                addBackgroundView(bkgViewGroup, resourceId);
            }
        }
    }

    private void addBackgroundView(ViewGroup bkgViewGroup, int resourceId) {
        if (resourceId == 0) {
            return;
        }

        ViewGroup view = (ViewGroup) LayoutInflater.from(bkgViewGroup.getContext()).inflate(R.layout.layout_config_item_page, bkgViewGroup, false);
        ImageView bkgView = view.findViewById(R.id.image);
        bkgView.setImageDrawable(bkgViewGroup.getContext().getDrawable(resourceId));
        bkgViewGroup.addView(view);
//            bkgViewGroup.invalidate();

    }
}
