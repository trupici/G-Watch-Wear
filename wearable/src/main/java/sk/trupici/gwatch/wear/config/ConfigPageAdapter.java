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

import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;

/**
 * {@code Adapter} for the main vertically scrolled view pager handling watch face config pages
 */
public class ConfigPageAdapter extends WearableRecyclerView.Adapter<ConfigPageAdapter.ViewHolder> {
    private static final String LOG_TAG = ConfigPageAdapter.class.getSimpleName();

    final private WatchfaceConfig watchfaceConfig;
    final private ConfigPageData pageData;
    final private ConfigItemData[] items;
    final private ViewPager2 pager;

    final private ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onPageSelected: " + position);
            }
            watchfaceConfig.setSelectedIdx(pager.getContext(), pageData.getType(), position);
        }
    };

    public ConfigPageAdapter(ConfigPageData pageData, ConfigItemData[] items, WatchfaceConfig watchfaceConfig, ViewPager2 pager, SharedPreferences prefs) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "WatchfaceDataAdapter created");
        }

        this.items = items;
        this.pageData = pageData;
        this.watchfaceConfig = watchfaceConfig;
        this.pager = pager;

        this.pager.registerOnPageChangeCallback(pageChangeCallback);
        this.pager.setCurrentItem(watchfaceConfig.getSelectedIdx(pager.getContext(), pageData.getType()));
    }

    public void destroy() {
        this.pager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onCreateViewHolder: " + parent + ", " + viewType + ", " + parent.getChildCount());
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_item_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onBindViewHolder: " + holder + ", " + position);
        }
        ConfigItemData itemData = items[position];
        holder.label.setText(itemData.getLabel());
        holder.image.setImageDrawable(holder.image.getContext().getDrawable(itemData.getResourceId()));
    }

    static class ViewHolder extends WearableRecyclerView.ViewHolder {
        TextView label;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.label);
            image = itemView.findViewById(R.id.image);
        }
    }
}
