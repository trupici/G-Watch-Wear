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
import sk.trupici.gwatch.wear.R;

import static sk.trupici.gwatch.wear.config.StandardAnalogWatchFaceConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE;

/**
 * Displays watch face preview along with complication locations. Allows user to tap on the
 * complication they want to change and preview updates dynamically.
 */
public class ComplicationsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    private final static String LOG_TAG = ComplicationsViewHolder.class.getSimpleName();

    private final ComplicationAdapter complicationAdapter;

    final private ViewGroup bkgViewGroup;

    final private ImageButton leftComplication;
    final private ImageView leftBackground;
    final private ImageView leftComplicationBorder;

    final private ImageButton rightComplication;
    final private ImageView rightBackground;
    final private ImageView rightComplicationBorder;

    final private ImageButton centerComplication;
    final private ImageView centerBackground;
    final private ImageView centerComplicationBorder;

    final private ImageButton bottomComplication;
    final private ImageView bottomBackground;
    final private ImageView bottomComplicationBorder;

    final private Drawable defaultComplicationDrawable;


    public ComplicationsViewHolder(Context context, ComplicationAdapter complicationAdapter, final View view) {
        super(view);

        this.complicationAdapter = complicationAdapter;
        defaultComplicationDrawable = context.getDrawable(R.drawable.config_add_complication);

        bkgViewGroup = view.findViewById(R.id.backgrounds);

        ViewGroup complLayout = view.findViewById(R.id.left_complication);
        leftComplication = complLayout.findViewById(R.id.complication);
        leftBackground = complLayout.findViewById(R.id.background);
        leftComplicationBorder = complLayout.findViewById(R.id.border);
        leftComplication.setOnClickListener(this);
        leftComplication.setOnLongClickListener(this);

        complLayout = view.findViewById(R.id.right_complication);
        rightComplication = complLayout.findViewById(R.id.complication);
        rightBackground = complLayout.findViewById(R.id.background);
        rightComplicationBorder = complLayout.findViewById(R.id.border);
        rightComplication.setOnClickListener(this);
        rightComplication.setOnLongClickListener(this);

        complLayout = view.findViewById(R.id.center_complication);
        centerComplication = complLayout.findViewById(R.id.complication);
        centerBackground = complLayout.findViewById(R.id.background);
        centerComplicationBorder = complLayout.findViewById(R.id.border);
        centerComplication.setOnClickListener(this);
        centerComplication.setOnLongClickListener(this);

        complLayout = view.findViewById(R.id.bottom_complication);
        bottomComplication = complLayout.findViewById(R.id.complication);
        bottomBackground = complLayout.findViewById(R.id.background);
        bottomComplicationBorder = complLayout.findViewById(R.id.border);
        bottomComplication.setOnClickListener(this);
        bottomComplication.setOnLongClickListener(this);

    }

    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "onClick: " + view);
        ComplicationId complicationId = getComplicationId(view);
        if (complicationId != null) {
            selectComplication(complicationId);
        }
    }

    public void selectComplication(ComplicationId complicationId) {
        Log.d(LOG_TAG, "selectComplication: " + complicationId.name());
        ComplicationId prevComplicationId = complicationAdapter.getSelectedComplicationId();
        if (prevComplicationId == complicationId) {
            return;
        } else if (prevComplicationId != null) {
            unselectComplication(prevComplicationId);
        }

        // TODO

        complicationAdapter.setSelectedComplicationId(complicationId);
    }

    public void unselectComplication(ComplicationId complicationId) {
        Log.d(LOG_TAG, "unselectComplication: " + complicationId.name());
        // TODO
    }

    @Override
    public boolean onLongClick(View view) {
        ComplicationId complicationId = getComplicationId(view);
        if (complicationId == null) {
            return false;
        } else {
            selectComplication(complicationId);
        }

        Log.d(LOG_TAG, "Complication click(): " + complicationAdapter.getSelectedComplicationId());

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
        ImageButton complication = getComplicationButton(complicationId);
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

    public ImageButton getComplicationButton(ComplicationId complicationId) {
        switch (complicationId) {
            case LEFT_COMPLICATION_ID:
                return leftComplication;
            case RIGHT_COMPLICATION_ID:
                return rightComplication;
            case CENTER_COMPLICATION_ID:
                return centerComplication;
            case BOTTOM_COMPLICATION_ID:
                return bottomComplication;
            default:
                return null;
        }
    }

    private ImageView getComplicationBackground(ComplicationId complicationId) {
        switch (complicationId) {
            case LEFT_COMPLICATION_ID:
                return leftBackground;
            case RIGHT_COMPLICATION_ID:
                return rightBackground;
            case CENTER_COMPLICATION_ID:
                return centerBackground;
            case BOTTOM_COMPLICATION_ID:
                return bottomBackground;
            default:
                return null;
        }
    }

    private ImageView getComplicationBorder(ComplicationId complicationId) {
        switch (complicationId) {
            case LEFT_COMPLICATION_ID:
                return leftComplicationBorder;
            case RIGHT_COMPLICATION_ID:
                return rightComplicationBorder;
            case CENTER_COMPLICATION_ID:
                return centerComplicationBorder;
            case BOTTOM_COMPLICATION_ID:
                return bottomComplicationBorder;
            default:
                return null;
        }
    }

    public ComplicationId getComplicationId(View button) {
        if (button.equals(leftComplication)) {
            return ComplicationId.LEFT_COMPLICATION_ID;
        } else if (button.equals(rightComplication)) {
            return ComplicationId.RIGHT_COMPLICATION_ID;
        } else if (button.equals(centerComplication)) {
            return ComplicationId.CENTER_COMPLICATION_ID;
        } else if (button.equals(bottomComplication)) {
            return ComplicationId.BOTTOM_COMPLICATION_ID;
        } else {
            return null;
        }
    }

    public void setBackground(ComplicationId complicationId, Drawable drawable) {
        ImageView background = getComplicationBackground(complicationId);
        if (background != null) {
            background.setBackground(drawable);
        }
    }

    public void setColorFilter(ComplicationId complicationId, ColorFilter colorFilter) {
        ImageButton complication = getComplicationButton(complicationId);
        if (complication != null) {
            complication.setColorFilter(colorFilter);
        }
    }

    public void setBorder(ComplicationId complicationId, Drawable borderDrawable) {
        ImageView border = getComplicationBorder(complicationId);
        if (border != null) {
            border.setImageDrawable(borderDrawable);
        }
    }

    public void setIcon(ComplicationId complicationId, Icon icon) {
        ImageButton complication = getComplicationButton(complicationId);
        if (complication != null) {
            complication.setImageIcon(icon);
        }
    }

    public void setDrawable(ComplicationId complicationId, Drawable drawable) {
        ImageButton complication = getComplicationButton(complicationId);
        if (complication != null) {
            complication.setImageDrawable(drawable);
        }
    }

    public ViewGroup getBackgroundViewGroup() {
        return bkgViewGroup;
    }
}
