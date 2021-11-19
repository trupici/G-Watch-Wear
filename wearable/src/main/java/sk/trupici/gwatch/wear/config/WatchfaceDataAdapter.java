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
import sk.trupici.gwatch.wear.R;

public class WatchfaceDataAdapter extends WearableRecyclerView.Adapter<WatchfaceDataAdapter.ViewHolder> {
    private static final String LOG_TAG = WatchfaceDataAdapter.class.getSimpleName();

    final private AnalogWatchfaceConfig config;
    final private ConfigPageData pageData;
    final private ViewPager2 pager;

    final private SharedPreferences prefs;

    final private ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            Log.d(LOG_TAG, "onPageSelected: " + position);
            prefs.edit().putInt(config.getPrefName(pageData.getType()), position).commit();
        }
    };

    public WatchfaceDataAdapter(ConfigPageData pageData, AnalogWatchfaceConfig config, ViewPager2 pager, SharedPreferences prefs) {
        Log.d(LOG_TAG, "ConfigDataAdapter created");
        this.pageData = pageData;
        this.config = config;
        this.pager = pager;

        this.prefs = prefs;

        this.pager.registerOnPageChangeCallback(pageChangeCallback);
        this.pager.setCurrentItem(prefs.getInt(config.getPrefName(pageData.getType()), 0));
    }

    public void destroy() {
        this.pager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    @Override
    public int getItemCount() {
        return pageData.getItems().length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(LOG_TAG, "onCreateViewHolder: " + parent + ", " + viewType + ", " + parent.getChildCount());
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_item_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(LOG_TAG, "onBindViewHolder: " + holder + ", " + position);

        ConfigPageData.ConfigItemData itemData = pageData.getItems()[position];
        holder.label.setText(itemData.label);
        holder.image.setImageDrawable(holder.image.getContext().getDrawable(itemData.resourceId));
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
