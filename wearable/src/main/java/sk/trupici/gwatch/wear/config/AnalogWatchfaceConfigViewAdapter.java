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

import java.util.ArrayList;
import java.util.List;

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
import sk.trupici.gwatch.wear.components.BgPanel;
import sk.trupici.gwatch.wear.components.DatePanel;
import sk.trupici.gwatch.wear.components.WatchHands;
import sk.trupici.gwatch.wear.components.BgGraph;
import sk.trupici.gwatch.wear.config.complications.ComplicationAdapter;
import sk.trupici.gwatch.wear.config.complications.ComplicationViewHolder;
import sk.trupici.gwatch.wear.config.menu.AlarmsMenuItems;
import sk.trupici.gwatch.wear.config.menu.BgGraphMenuItems;
import sk.trupici.gwatch.wear.config.menu.BgPanelMenuItems;
import sk.trupici.gwatch.wear.config.menu.ComplicationsMenuItems;
import sk.trupici.gwatch.wear.config.menu.DatePanelMenuItems;
import sk.trupici.gwatch.wear.watchface.AnalogWatchfaceService;

public class AnalogWatchfaceConfigViewAdapter extends WearableRecyclerView.Adapter<WearableRecyclerView.ViewHolder> {

    private final static String LOG_TAG = AnalogWatchfaceConfigViewAdapter.class.getSimpleName();
    private static final String TAG_VERTICAL_SCROLLABLE = "Pager";

    int position = 0;

    final private Context context;
    final private ViewPager2 pager;
    final private AnalogWatchfaceConfig config;

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
            AnalogWatchfaceConfigViewAdapter.this.position = position;

            // notify all view holders about page change
            View child = pager.getChildAt(0); // get pager's RecycleView
            if (child instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) child;
                for (int i = 1; i < config.getItemCount(); i++) { // skip background config
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

    public AnalogWatchfaceConfigViewAdapter(Context context, AnalogWatchfaceConfig config, ViewPager2 pager, SharedPreferences prefs) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "MainConfigViewAdapter: ");
        }
        this.context = context;
        this.config = config;
        this.pager = pager;

        this.componentName = new ComponentName(context, AnalogWatchfaceService.class);

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
        return config.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "getItemViewType: " + position);
        }
        return config.getPageData(position).getType().ordinal();
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
                return new WatchFaceViewHolder(this, view, type);
            case HANDS:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page, parent, false);
                view.setId(WatchHands.CONFIG_ID);
                return new WatchFaceViewHolder(this, view, type);
            case COMPLICATIONS:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                return new ComplicationViewHolder(view);
            case BG_PANEL:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(BgPanel.CONFIG_ID);
                return new ListConfigViewHolder(view);
            case BG_GRAPH:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(BgGraph.CONFIG_ID);
                return new ListConfigViewHolder(view);
            case DATE_PANEL:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(DatePanel.CONFIG_ID);
                return new ListConfigViewHolder(view);
            case ALARMS:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_config_page2, parent, false);
                view.findViewById(R.id.page_title).setVisibility(View.VISIBLE);
                view.setId(BgAlarmController.CONFIG_ID);
                return new ListConfigViewHolder(view);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public void onBindViewHolder(@NonNull WearableRecyclerView.ViewHolder holder, int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onBindViewHolder: " + holder + ", " + position);
        }
        if (holder instanceof WatchFaceViewHolder) {
            onBindWatchFaceViewHolder((WatchFaceViewHolder)holder, position);
        } else if (holder instanceof ComplicationViewHolder) {
            onBindComplicationViewHolder((ComplicationViewHolder) holder, position);
        } else if (holder instanceof ListConfigViewHolder) {
            ListConfigViewHolder listHolder = (ListConfigViewHolder) holder;
            if (BgPanel.CONFIG_ID == listHolder.getViewId()) {
                onBindBgPanelConfigViewHolder(listHolder, position);
            } else if (BgGraph.CONFIG_ID == listHolder.getViewId()) {
                onBindBgGraphConfigViewHolder(listHolder, position);
            } else if (DatePanel.CONFIG_ID == listHolder.getViewId()) {
                onBindDatePanelConfigViewHolder(listHolder, position);
            } else if (BgAlarmController.CONFIG_ID == listHolder.getViewId()) {
                onBindAlarmsConfigViewHolder(listHolder, position);
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.pager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    private void onBindWatchFaceViewHolder(WatchFaceViewHolder holder, int position) {
        ConfigPageData pageData = config.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getVerticalPager().setAdapter(new WatchfaceDataAdapter(pageData, config, holder.getVerticalPager(), prefs));
        if (pageData.getType() != ConfigPageData.ConfigType.BACKGROUND) {
            holder.onBackgroundChanged();
        }

        holder.getVerticalPager().setCurrentItem(prefs.getInt(config.getPrefName(pageData.getType()), 0));
        holder.getVerticalPager().setTag(TAG_VERTICAL_SCROLLABLE + position);
        holder.getVerticalPager().requestFocus();
    }

    private void onBindComplicationViewHolder(ComplicationViewHolder holder, int position) {
        ComplicationAdapter adapter = new ComplicationAdapter(
                context,
                componentName,
                ComplicationsMenuItems.items,
                config,
                prefs);

        ConfigPageData pageData = config.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
        holder.onBackgroundChanged();
    }

    private void onBindBgPanelConfigViewHolder(ListConfigViewHolder holder, int position) {
        ListConfigAdapter adapter = new ListConfigAdapter(
                context,
                holder.getRecyclerView(),
                BgPanel.CONFIG_ID,
                BgPanelMenuItems.items,
                prefs);

        ConfigPageData pageData = config.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }


    private void onBindBgGraphConfigViewHolder(ListConfigViewHolder holder, int position) {
        ListConfigAdapter adapter = new ListConfigAdapter(
                context,
                holder.getRecyclerView(),
                BgGraph.CONFIG_ID,
                BgGraphMenuItems.items,
                prefs);

        ConfigPageData pageData = config.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }

    private void onBindDatePanelConfigViewHolder(ListConfigViewHolder holder, int position) {
        ListConfigAdapter adapter = new ListConfigAdapter(
                context,
                holder.getRecyclerView(),
                DatePanel.CONFIG_ID,
                DatePanelMenuItems.items,
                prefs);

        ConfigPageData pageData = config.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }

    private void onBindAlarmsConfigViewHolder(ListConfigViewHolder holder, int position) {
        ListConfigAdapter adapter = new ListConfigAdapter(
                context,
                holder.getRecyclerView(),
                BgAlarmController.CONFIG_ID,
                AlarmsMenuItems.items,
                prefs);

        ConfigPageData pageData = config.getPageData(position);
        holder.getTitle().setText(context.getString(pageData.getTitleId()));

        holder.getRecyclerView().setLayoutManager(new LinearLayoutManager(context));
        holder.getRecyclerView().setHasFixedSize(true);
        holder.getRecyclerView().setAdapter(adapter);
        holder.getRecyclerView().setTag(TAG_VERTICAL_SCROLLABLE + position);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull WearableRecyclerView.ViewHolder holder) {
        if (holder instanceof ComplicationViewHolder) {
            ((ComplicationAdapter)((ComplicationViewHolder) holder).getRecyclerView().getAdapter()).destroy();
        } else if (holder instanceof WatchFaceViewHolder) {
            ((WatchfaceDataAdapter)((WatchFaceViewHolder) holder).getVerticalPager().getAdapter()).destroy();
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

    public List<Integer> getBackgroundResourceIds(ConfigPageData.ConfigType type) {
        List<Integer> ids = new ArrayList<>();
        if (type == ConfigPageData.ConfigType.HANDS) {
            ids.add(config.getConfigItemData(ConfigPageData.ConfigType.BACKGROUND,
                    prefs.getInt(AnalogWatchfaceConfig.PREF_BACKGROUND_IDX, AnalogWatchfaceConfig.DEF_BACKGROUND_IDX)
            ).getResourceId());
        }
        return ids;
    }
}
