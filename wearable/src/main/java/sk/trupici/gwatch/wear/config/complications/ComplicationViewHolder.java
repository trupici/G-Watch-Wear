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

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BackgroundChangeAware;
import sk.trupici.gwatch.wear.config.ConfigItemData;
import sk.trupici.gwatch.wear.config.ConfigPageData;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;

import static sk.trupici.gwatch.wear.util.CommonConstants.COMPLICATION_CONFIG_REQUEST_CODE;

/**
 * Displays watch face preview along with complication locations.
 */
public abstract class ComplicationViewHolder extends RecyclerView.ViewHolder implements BackgroundChangeAware, View.OnClickListener, View.OnLongClickListener {

    private final static String LOG_TAG = ComplicationViewHolder.class.getSimpleName();

    final private WatchfaceConfig watchfaceConfig;
    final private ComplicationsConfigAdapter complicationAdapter;

    final private ViewGroup bkgViewGroup;

    final private Drawable defaultComplicationDrawable;

    protected abstract void initComplicationViews(View view);
    protected abstract ComplicationViews getComplicationViews(ComplicationId complicationId);
    protected abstract ComplicationId getComplicationId(View button);

    public ComplicationViewHolder(WatchfaceConfig watchfaceConfig, ComplicationsConfigAdapter complicationAdapter, final View view) {
        super(view);

        this.watchfaceConfig = watchfaceConfig;
        this.complicationAdapter = complicationAdapter;

        defaultComplicationDrawable = complicationAdapter.getDefaultComplicationDrawable();

        bkgViewGroup = view.findViewById(R.id.backgrounds);
        initComplicationViews(view);

        // show selector for complication marked as selected in adapter
        ComplicationId selected = complicationAdapter.getSelectedComplicationId();
        if (selected != null) {
            View selector = getComplicationViews(selected).selector;
            if (selector != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(LOG_TAG, "ComplicationsViewHolder: selecting " + selected);
                }
                selector.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onClick: " + view);
        }
        ComplicationId complicationId = getComplicationId(view);
        if (complicationId != null) {
            selectComplication(complicationId);
        }
    }

    public void selectComplication(ComplicationId complicationId) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "selectComplication: " + complicationId);
        }
        ComplicationId prevComplicationId = complicationAdapter.getSelectedComplicationId();
        if (prevComplicationId == complicationId) {
            return;
        } else if (prevComplicationId != null) {
            unselectComplication(prevComplicationId);
        }

        complicationAdapter.setSelectedComplicationId(complicationId);
        
        View selector = getComplicationViews(complicationId).selector;
        if (selector != null) {
            selector.setVisibility(View.VISIBLE);
        }
    }

    public void unselectComplication(ComplicationId complicationId) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "unselectComplication: " + complicationId.name());
        }
        View selector = getComplicationViews(complicationId).selector;
        if (selector != null) {
            selector.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        ComplicationId complicationId = getComplicationId(view);
        if (complicationId == null) {
            return false;
        } else {
            selectComplication(complicationId);
        }

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Complication click(): " + complicationAdapter.getSelectedComplicationId());
        }
        // Verifies the watch face supports the complication location, then launches the helper
        // class, so user can choose their complication data provider.
        Activity currentActivity = (Activity) view.getContext();
        currentActivity.startActivityForResult(
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        currentActivity,
                        complicationAdapter.getComponentName(),
                        complicationAdapter.getSelectedComplicationId().ordinal(),
                        watchfaceConfig.getComplicationConfig(complicationId).getSupportedTypes()),
                COMPLICATION_CONFIG_REQUEST_CODE);
        return true;
    }

    public boolean updateComplicationView(Context context, ComplicationProviderInfo complicationProviderInfo, ComplicationId complicationId) {
        Log.e(LOG_TAG, "updateComplicationView: " + complicationId + ", " + complicationProviderInfo);
        ImageButton complication = getComplicationViews(complicationId).complication;
        if (complication == null) {
            return false;
        }

        complication.setImageIcon(null);
        if (complicationProviderInfo != null) {
            try {
                context.getPackageManager().getApplicationInfo(complicationProviderInfo.providerIcon.getResPackage(), PackageManager.GET_SHARED_LIBRARY_FILES);
                complication.setImageIcon(complicationProviderInfo.providerIcon);
            } catch (Exception e) {
                Log.e(LOG_TAG, "updateComplicationView: unable to retrieve icon from provider info");
            }
            complication.setContentDescription(context.getString(R.string.edit_complication));
        } else {
            complication.setContentDescription(context.getString(R.string.add_complication));
        }
        if (complication.getDrawable() == null) {
            complication.setImageDrawable(defaultComplicationDrawable);
        }
        return true;
    }

    public void setBackground(ComplicationId complicationId, Drawable drawable) {
        ImageView background = getComplicationViews(complicationId).background;
        if (background != null) {
            background.setBackground(drawable);
        }
    }

    public void setColorFilter(ComplicationId complicationId, ColorFilter colorFilter) {
        ImageButton complication = getComplicationViews(complicationId).complication;
        if (complication != null) {
            complication.setColorFilter(colorFilter);
        }
    }

    public void setBorder(ComplicationId complicationId, Drawable borderDrawable) {
        ImageView border = getComplicationViews(complicationId).border;
        if (border != null) {
            border.setImageDrawable(borderDrawable);
        }
    }

    public void setIcon(ComplicationId complicationId, Icon icon) {
        ImageButton complication = getComplicationViews(complicationId).complication;
        if (complication != null) {
            complication.setImageIcon(icon);
        }
    }

    public void setDrawable(ComplicationId complicationId, Drawable drawable) {
        ImageButton complication = getComplicationViews(complicationId).complication;
        if (complication != null) {
            complication.setImageDrawable(drawable);
        }
    }

    @Override
    public void onBackgroundChanged() {
        bkgViewGroup.removeAllViews();
        Context context = bkgViewGroup.getContext();
        ConfigItemData itemData = watchfaceConfig.getSelectedItem(context, ConfigPageData.ConfigType.BACKGROUND);
        if (itemData != null && itemData.getResourceId() != 0) {
            ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.layout_config_item_page, bkgViewGroup, false);
            ImageView bkgView = view.findViewById(R.id.image);
            bkgView.setImageDrawable(ContextCompat.getDrawable(context, itemData.getResourceId()));
            // add more items indicator
            ImageView more = view.findViewById(R.id.label_image);
            more.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.config_more_items));
            bkgViewGroup.addView(view);
        }
    }

    static class ComplicationViews {
        final ImageButton complication;
        final ImageView background;
        final ImageView border;
        final ImageView selector;

        ComplicationViews(ViewGroup complicationViewGroup, View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener) {
            complication = complicationViewGroup.findViewById(R.id.complication);
            background = complicationViewGroup.findViewById(R.id.background);
            border = complicationViewGroup.findViewById(R.id.border);
            selector = complicationViewGroup.findViewById(R.id.selector);
            complication.setOnClickListener(onClickListener);
            complication.setOnLongClickListener(onLongClickListener);
        }
    }
}
