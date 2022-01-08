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
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;

import static sk.trupici.gwatch.wear.util.CommonConstants.COMPLICATION_CONFIG_REQUEST_CODE;

/**
 * Displays watch face preview along with complication locations. Allows user to tap on the
 * complication they want to change and preview updates dynamically.
 */
public class ComplicationsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    private final static String LOG_TAG = ComplicationsViewHolder.class.getSimpleName();

    private final ComplicationAdapter complicationAdapter;

    final private ViewGroup bkgViewGroup;
    final private ComplicationViews left;
    final private ComplicationViews right;

    final private Drawable defaultComplicationDrawable;


    public ComplicationsViewHolder(Context context, ComplicationAdapter complicationAdapter, final View view) {
        super(view);

        this.complicationAdapter = complicationAdapter;
        defaultComplicationDrawable = context.getDrawable(R.drawable.config_add_complication); // FIXME change drawable

        bkgViewGroup = view.findViewById(R.id.backgrounds);

        left = new ComplicationViews(view.findViewById(R.id.left_complication));
        right = new ComplicationViews(view.findViewById(R.id.right_complication));

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
                        Config.getComplicationConfig(complicationId).getSupportedTypes()),
                COMPLICATION_CONFIG_REQUEST_CODE);
        return true;
    }

    public boolean updateComplicationView(Context context, ComplicationProviderInfo complicationProviderInfo, Integer id) {
        if (id == null) {
            return false;
        }
        ComplicationId complicationId = ComplicationId.valueOf(id);
        ImageButton complication = getComplicationViews(complicationId).complication;
        if (complication == null) {
            return false;
        }

        complication.setImageIcon(null);
        if (complicationProviderInfo != null) {
            try {
                complication.setImageIcon(complicationProviderInfo.providerIcon);
            } catch (Exception e) {
                Log.e(LOG_TAG, "updateComplicationView: " + e.getLocalizedMessage());
            }
            complication.setContentDescription(context.getString(R.string.edit_complication));
        } else {
            complication.setContentDescription(context.getString(R.string.add_complication));
        }
        if (complication.getDrawable() == null) {
            complication.setImageDrawable(defaultComplicationDrawable != null ? defaultComplicationDrawable : new ColorDrawable(Color.TRANSPARENT));
        }
        return true;
    }

    public ComplicationViews getComplicationViews(ComplicationId complicationId) {
        switch (complicationId) {
            case LEFT_COMPLICATION_ID:
                return left;
            case RIGHT_COMPLICATION_ID:
                return right;
            default:
                return null;
        }
    }

    public ComplicationId getComplicationId(View button) {
        if (button.equals(left.complication)) {
            return ComplicationId.LEFT_COMPLICATION_ID;
        } else if (button.equals(right.complication)) {
            return ComplicationId.RIGHT_COMPLICATION_ID;
        } else {
            return null;
        }
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

    public ViewGroup getBackgroundViewGroup() {
        return bkgViewGroup;
    }

    class ComplicationViews {
        final ImageButton complication;
        final ImageView background;
        final ImageView border;
        final ImageView selector;

        ComplicationViews(ViewGroup complicationViewGroup) {
            complication = complicationViewGroup.findViewById(R.id.complication);
            background = complicationViewGroup.findViewById(R.id.background);
            border = complicationViewGroup.findViewById(R.id.border);
            selector = complicationViewGroup.findViewById(R.id.selector);
            complication.setOnClickListener(ComplicationsViewHolder.this);
            complication.setOnLongClickListener(ComplicationsViewHolder.this);
        }
    }
}
