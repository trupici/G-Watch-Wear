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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.components.BackgroundPanel;
import sk.trupici.gwatch.wear.components.BgAlarmController;
import sk.trupici.gwatch.wear.components.BgGraph;
import sk.trupici.gwatch.wear.components.BgPanel;
import sk.trupici.gwatch.wear.components.DigitalTimePanel;
import sk.trupici.gwatch.wear.config.complications.ComplicationsConfigAdapter;
import sk.trupici.gwatch.wear.config.complications.ComplicationsConfigViewHolder;
import sk.trupici.gwatch.wear.config.menu.AlarmsMenuItems;
import sk.trupici.gwatch.wear.config.menu.BgGraphMenuItems;
import sk.trupici.gwatch.wear.config.menu.BgPanelMenuItems;
import sk.trupici.gwatch.wear.config.menu.ComplicationsMenuItems;
import sk.trupici.gwatch.wear.config.menu.DigitalTimePanelMenuItems;
import sk.trupici.gwatch.wear.watchface.DigitalWatchfaceService;

/**
 * Main {@code Adapter} for Digital watch face configuration
 */
public class DigitalWatchfaceConfigViewAdapter extends WearableRecyclerView.Adapter<WearableRecyclerView.ViewHolder> {

    private final static String LOG_TAG = DigitalWatchfaceConfigViewAdapter.class.getSimpleName();
    private static final String TAG_VERTICAL_SCROLLABLE = "Pager";

    int position = 0;

    final private Context context;
    final private ViewPager2 pager;
    final private DigitalWatchfaceConfig watchfaceConfig;

    final private SharedPreferences prefs;

    // ComponentName associated with watch face service (service that renders watch face). Used
    // to retrieve complication information.
    final private ComponentName componentName;

    final private ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "onPageSelected: " + position);
            }
            DigitalWatchfaceConfigViewAdapter.this.position = position;

            // notify all view holders about page change
            View child = pager.getChildAt(0); // get pager's RecycleView
            if (child instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) child;
                for (int i = 1; i < watchfaceConfig.getItemCount(); i++) { // skip background config
                    View view = recyclerView.getChildAt(i);
                    if (view != null) {
                        RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
                        if (holder instanceof BackgroundChangeAware) {
                            ((BackgroundChangeAware)holder).onBackgroundChanged();
                        }
                    }
                }
            }
        }
    };

    public DigitalWatchfaceConfigViewAdapter(Context context, DigitalWatchfaceConfig watchfaceConfig, ViewPager2 pager, SharedPreferences prefs) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "MainConfigViewAdapter: ");
        }
        this.context = context;
        this.watchfaceConfig = watchfaceConfig;
        this.pager = pager;

        this.componentName = new ComponentName(context, DigitalWatchfaceService.class);

        this.prefs = prefs;

        this.pager.registerOnPageChangeCallback(pageChangeCallback);
    }

    public void destroy() {
        this.pager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        View child = pager.findViewWithTag(TAG_VERTICAL_SCROLLABLE + position);
        if (child != null) {
            if (child instanceof RecyclerView) {
                return child.onGenericMotionEvent(event);
            } else if (child instanceof ViewPager2) {
                if (event.getAction() == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)) {
                    ViewPager2 verticalPager = (ViewPager2) child;
                    float delta = -event.getAxisValue(MotionEventCompat.AXIS_SCROLL);
                    delta = -Math.signum(delta);
                    try {
                        verticalPager.beginFakeDrag();
                        verticalPager.fakeDragBy(delta);
                        return true;
                    } finally {
                        verticalPager.endFakeDrag();
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return watchfaceConfig.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "getItemViewType: " + position);
        }
        return watchfaceConfig.getPageData(position).getType().ordinal();
    }

    @NonNull
    @Override
    public WearableRecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onCreateViewHolder: " + parent.getClass().getSimpleName() + ", " + viewType + ", " + position);
        }

        View view;
        ConfigPageData.ConfigType type = ConfigPageData.ConfigType.valueOf(viewType);
        switch (type) {
            case BACKGROUND:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page, parent, false);
                view.setId(BackgroundPanel.CONFIG_ID);
                return new ImageSetPageViewHolder(watchfaceConfig, view, type);
            case COMPLICATIONS:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                return new ComplicationsConfigViewHolder(view);
            case BG_PANEL:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(BgPanel.CONFIG_ID);
                return new ConfigItemListPageViewHolder(view);
            case BG_GRAPH:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(BgGraph.CONFIG_ID);
                return new ConfigItemListPageViewHolder(view);
            case ALARMS:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(BgAlarmController.CONFIG_ID);
                return new ConfigItemListPageViewHolder(view);
            case TIME_PANEL:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(DigitalTimePanel.CONFIG_ID);
                return new ConfigItemListPageViewHolder(view);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public void onBindViewHolder(@NonNull WearableRecyclerView.ViewHolder holder, int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onBindViewHolder: " + holder + ", " + position);
        }
        if (holder instanceof ImageSetPageViewHolder) {
            onBindBackgroundViewHolder((ImageSetPageViewHolder)holder, position);
        } else if (holder instanceof ComplicationsConfigViewHolder) {
            onBindComplicationViewHolder((ComplicationsConfigViewHolder) holder, position);
        } else if (holder instanceof ConfigItemListPageViewHolder) {
            ConfigItemListPageViewHolder listHolder = (ConfigItemListPageViewHolder) holder;
            if (BgPanel.CONFIG_ID == listHolder.getViewId()) {
                onBindBgPanelConfigViewHolder(listHolder, position);
            } else if (BgGraph.CONFIG_ID == listHolder.getViewId()) {
                onBindBgGraphConfigViewHolder(listHolder, position);
            } else if (BgAlarmController.CONFIG_ID == listHolder.getViewId()) {
                onBindAlarmsConfigViewHolder(listHolder, position);
            } else if (DigitalTimePanel.CONFIG_ID == listHolder.getViewId()) {
                onBindTimePanelConfigViewHolder(listHolder, position);
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.pager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    private void onBindBackgroundViewHolder(ImageSetPageViewHolder holder, int position) {
        ConfigPageData pageData = watchfaceConfig.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getVerticalPager().setAdapter(new ConfigPageAdapter(
                pageData,
                watchfaceConfig.getItems(ConfigPageData.ConfigType.BACKGROUND),
                watchfaceConfig,
                holder.getVerticalPager(),
                prefs));

        holder.getVerticalPager().setCurrentItem(watchfaceConfig.getSelectedIdx(context, ConfigPageData.ConfigType.BACKGROUND));
        holder.getVerticalPager().setTag(TAG_VERTICAL_SCROLLABLE + position);
        holder.getVerticalPager().requestFocus();
    }

    private void onBindComplicationViewHolder(ComplicationsConfigViewHolder holder, int position) {
        ComplicationsConfigAdapter adapter = new ComplicationsConfigAdapter(
                context,
                componentName,
                ComplicationsMenuItems.items,
                watchfaceConfig,
                prefs);

        ConfigPageData pageData = watchfaceConfig.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
        holder.onBackgroundChanged();
    }

    private void onBindBgPanelConfigViewHolder(ConfigItemListPageViewHolder holder, int position) {
        ConfigItemListAdapter adapter = new ConfigItemListAdapter(
                context,
                holder.getRecyclerView(),
                BgPanel.CONFIG_ID,
                BgPanelMenuItems.items,
                prefs,
                watchfaceConfig);

        ConfigPageData pageData = watchfaceConfig.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }

    private void onBindBgGraphConfigViewHolder(ConfigItemListPageViewHolder holder, int position) {
        ConfigItemListAdapter adapter = new ConfigItemListAdapter(
                context,
                holder.getRecyclerView(),
                BgGraph.CONFIG_ID,
                BgGraphMenuItems.items,
                prefs,
                watchfaceConfig);

        ConfigPageData pageData = watchfaceConfig.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }

    private void onBindAlarmsConfigViewHolder(ConfigItemListPageViewHolder holder, int position) {
        ConfigItemListAdapter adapter = new ConfigItemListAdapter(
                context,
                holder.getRecyclerView(),
                BgAlarmController.CONFIG_ID,
                AlarmsMenuItems.items,
                prefs,
                watchfaceConfig);

        ConfigPageData pageData = watchfaceConfig.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }

    private void onBindTimePanelConfigViewHolder(ConfigItemListPageViewHolder holder, int position) {
        ConfigItemListAdapter adapter = new ConfigItemListAdapter(
                context,
                holder.getRecyclerView(),
                DigitalTimePanel.CONFIG_ID,
                DigitalTimePanelMenuItems.items,
                prefs,
                watchfaceConfig);

        ConfigPageData pageData = watchfaceConfig.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull WearableRecyclerView.ViewHolder holder) {
        if (holder instanceof ComplicationsConfigViewHolder) {
            ((ComplicationsConfigAdapter)((ComplicationsConfigViewHolder) holder).getRecyclerView().getAdapter()).destroy();
        } else if (holder instanceof ImageSetPageViewHolder) {
            ((ConfigPageAdapter)((ImageSetPageViewHolder) holder).getVerticalPager().getAdapter()).destroy();
        }
        super.onViewDetachedFromWindow(holder);
    }

    public void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        View view = pager.getChildAt(0/*position*/);
        if (view instanceof RecyclerView) {
            View child = view.findViewWithTag(TAG_VERTICAL_SCROLLABLE + position);
            if (child instanceof RecyclerView) {
                RecyclerView.Adapter<?> adapter = ((RecyclerView) child).getAdapter();
                if (adapter instanceof ActivityResultAware) {
                    if (((ActivityResultAware) adapter).onActivityResult(requestCode, resultCode, data)) {
                        ((WearableRecyclerView)child).smoothScrollToPosition(0);
                    }
                }
            }
        }
    }
}
