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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.AnalogWatchfaceConfig;
import sk.trupici.gwatch.wear.config.BackgroundChangeAware;
import sk.trupici.gwatch.wear.config.ColorPickerActivity;
import sk.trupici.gwatch.wear.config.ConfigPageData;
import sk.trupici.gwatch.wear.util.StringUtils;

import static android.app.Activity.RESULT_OK;
import static sk.trupici.gwatch.wear.config.StandardAnalogWatchFaceConfigActivity.BORDER_TYPE_CONFIG_REQUEST_CODE;
import static sk.trupici.gwatch.wear.config.StandardAnalogWatchFaceConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE;
import static sk.trupici.gwatch.wear.config.StandardAnalogWatchFaceConfigActivity.UPDATE_COLORS_CONFIG_REQUEST_CODE;

public class ComplicationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements BackgroundChangeAware {

    final public static int BORDER_WIDTH = 2;
    final public static float BORDER_DASH_LEN = 6f;
    final public static float BORDER_GAP_LEN = 2f;
    final public static float BORDER_DOT_LEN = BORDER_WIDTH;
    final public static float BORDER_ROUND_RECT_RADIUS = 15f;
    final public static float BORDER_RING_RADIUS = 100f;

    final private static String LOG_TAG = ComplicationAdapter.class.getSimpleName();
    final private Context context;
    final private ConfigPageData pageData;
    final private ComponentName componentName;
    final private ComplicationId complicationId;
    final private List<ConfigItem> items;
    final private AnalogWatchfaceConfig config;

    final private SharedPreferences prefs;

    // Required to retrieve complication data from watch face for preview.
    private final ProviderInfoRetriever providerInfoRetriever;

    // Maintains reference view holder to dynamically update watch face preview. Used instead of
    // notifyItemChanged(int position) to avoid flicker and re-inflating the view.
    private ComplicationViewHolder complicationViewHolder;

    public ComponentName getComponentName() {
        return componentName;
    }

    public ComplicationAdapter(Context context, ConfigPageData pageData, ComponentName componentName,
                               ComplicationId complicationId, List<ConfigItem> items, AnalogWatchfaceConfig config,
                               SharedPreferences prefs) {
        this.context = context;
        this.pageData = pageData;
        this.componentName = componentName;
        this.complicationId = complicationId;
        this.items = items;

        this.prefs = prefs;
        this.config = config;

        // Initialization of code to retrieve active complication data for the watch face.
        this.providerInfoRetriever = new ProviderInfoRetriever(context, Executors.newCachedThreadPool());
        providerInfoRetriever.init();
    }


    public void destroy() {
        providerInfoRetriever.release();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(LOG_TAG, "onCreateViewHolder(): viewType: " + viewType);

        RecyclerView.ViewHolder viewHolder;

        ConfigItem.Type type = ConfigItem.Type.valueOf(viewType);
        switch (type) {
            case TYPE_COMPLICATION:
                ComplicationConfigItem complItem = (ComplicationConfigItem) getConfigItemByType(type);
                View layout = LayoutInflater.from(parent.getContext()).inflate(complItem.getLayoutId(), parent, false);
                viewHolder = new ComplicationViewHolder(layout);
                complicationViewHolder = (ComplicationViewHolder) viewHolder;
                break;
            case TYPE_BORDER_COLOR:
            case TYPE_DATA_COLOR:
            case TYPE_BKG_COLOR:
                viewHolder = new ColorPickerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_button_item, parent, false));
                ColorConfigItem colorItem = (ColorConfigItem) getConfigItemByType(type);
                int color = prefs.getInt(colorItem.getSharedPrefString(), type == ConfigItem.Type.TYPE_DATA_COLOR ? Color.WHITE : Color.TRANSPARENT);
                Log.d(LOG_TAG, "onCreateViewHolder: '" + colorItem.getSharedPrefString() + "' "+ type.name() + "=" + StringUtils.formatColorStr(color));
                ((ColorPickerViewHolder) viewHolder).setColor(color);
                break;
            case TYPE_PADDING:
                viewHolder = new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_empty_item, parent, false)) {
                };
                break;
            case TYPE_BORDER_TYPE:
                viewHolder = new BorderTypePickerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_button_item, parent, false));
                BorderType borderType = getBorderType();
                ((BorderTypePickerViewHolder) viewHolder).setBorderType(borderType);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return viewHolder;
    }

    @Override
    public void onBackgroundChanged() {
        if (complicationViewHolder != null) {
            complicationViewHolder.onBackgroundChanged();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Log.d(LOG_TAG, "Element " + position + " set.");

        // Pulls all data required for creating the UX for the specific setting option.
        ConfigItem configItem = items.get(position);

        switch (ConfigItem.Type.valueOf(holder.getItemViewType())) {
            case TYPE_COMPLICATION:
                ComplicationViewHolder complicationViewHolder = (ComplicationViewHolder) holder;
                ComplicationConfigItem complicationConfigItem = (ComplicationConfigItem) configItem;
                complicationViewHolder.onBackgroundChanged();
                complicationViewHolder.setDefaultComplicationDrawable(complicationConfigItem.getDefaultComplicationResourceId());
                complicationViewHolder.initializeComplications();
                break;
            case TYPE_BORDER_COLOR:
            case TYPE_DATA_COLOR:
            case TYPE_BKG_COLOR:
                PickerViewHolder pickerViewHolder = (PickerViewHolder) holder;
                ColorConfigItem colorConfigItem = (ColorConfigItem) configItem;
                pickerViewHolder.setIcon(colorConfigItem.getIconResourceId());
                pickerViewHolder.setName(colorConfigItem.getLabel());
                pickerViewHolder.setType(configItem.getConfigType());
                pickerViewHolder.setPickerActivity(colorConfigItem.getActivityToChoosePreference());
                pickerViewHolder.setActivityCode(UPDATE_COLORS_CONFIG_REQUEST_CODE);
                break;
            case TYPE_PADDING:
                break;
            case TYPE_BORDER_TYPE:
                PickerViewHolder borderShapeViewHolder = (PickerViewHolder) holder;
                ShapeConfigItem borderConfigItem = (ShapeConfigItem) configItem;
                borderShapeViewHolder.setIcon(borderConfigItem.getIconResourceId());
                borderShapeViewHolder.setName(borderConfigItem.getLabel());
                borderShapeViewHolder.setType(borderConfigItem.getConfigType());
                borderShapeViewHolder.setPickerActivity(borderConfigItem.getActivityToChoosePreference());
                borderShapeViewHolder.setActivityCode(BORDER_TYPE_CONFIG_REQUEST_CODE);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getItemViewType(int position) {
        ConfigItem configItem = items.get(position);
        return configItem.getConfigType().ordinal();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Displays watch face preview along with complication locations. Allows user to tap on the
     * complication they want to change and preview updates dynamically.
     *
     * TODO to be renamed (multi-use)
     */
    public class ComplicationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, BackgroundChangeAware {
        final private ImageButton complication;
        final private ImageView background;
        final private ImageView border;
        private Drawable defaultDrawable;

        ViewGroup bkgViewGroup;

        public ComplicationViewHolder(final View view) {
            super(view);

            // Sets up complication preview.
            bkgViewGroup = view.findViewById(R.id.backgrounds);
            complication = view.findViewById(R.id.complication);
            background = view.findViewById(R.id.background);
            border = view.findViewById(R.id.border);

            complication.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.equals(complication)) {
                Log.d(LOG_TAG, "Left Complication click()");

                // Verifies the watch face supports the complication location, then launches the helper
                // class, so user can choose their complication data provider.
                Activity currentActivity = (Activity) view.getContext();
                currentActivity.startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                currentActivity,
                                componentName,
                                complicationId.ordinal(),
                                Config.getComplicationConfig(complicationId).getSupportedTypes()),
                        COMPLICATION_CONFIG_REQUEST_CODE);

            }
        }

        public void setDefaultComplicationDrawable(int resourceId) {
            defaultDrawable = context.getDrawable(resourceId);
        }

        private void updateComplicationView(ComplicationProviderInfo complicationProviderInfo) {
            if (complicationProviderInfo != null) {
                try {
                    complication.setImageIcon(complicationProviderInfo.providerIcon);
                } catch(Exception e) {
                }
                complication.setContentDescription(context.getString(R.string.edit_complication));
            } else {
                complication.setContentDescription(context.getString(R.string.add_complication));
            }
            if (complication.getDrawable() == null) {
                complication.setImageDrawable(defaultDrawable != null ? defaultDrawable : new ColorDrawable(Color.TRANSPARENT));
            }
        }

        public void initializeComplications() {
            final int[] complicationIds = Config.getComplicationIds();

            providerInfoRetriever.retrieveProviderInfo(
                    new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                        @Override
                        public void onProviderInfoReceived(int watchFaceComplicationId, @Nullable ComplicationProviderInfo complicationProviderInfo) {
                            Log.d(LOG_TAG, "onProviderInfoReceived: " + complicationProviderInfo);

                            updateComplicationView(complicationProviderInfo);
                        }
                    },
                    componentName,
                    complicationIds);

            BorderType borderType = getBorderType();

            // draw background
            ColorConfigItem colorItem = (ColorConfigItem) getConfigItemByType(ConfigItem.Type.TYPE_BKG_COLOR);
            int color = prefs.getInt(colorItem.getSharedPrefString(), Color.TRANSPARENT);
            Drawable drawable = createBorderDrawable(borderType, color, false);
            if (borderType == BorderType.NONE) {
                ((GradientDrawable)drawable).setColor(color);
            }
            complicationViewHolder.background.setBackground(drawable);

            // set data color
            colorItem = (ColorConfigItem) getConfigItemByType(ConfigItem.Type.TYPE_DATA_COLOR);
            color = prefs.getInt(colorItem.getSharedPrefString(), Color.TRANSPARENT);
            PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            complicationViewHolder.complication.setColorFilter(colorFilter);

            // draw border
            if (borderType != BorderType.NONE) {
                // draw border
                colorItem = (ColorConfigItem) getConfigItemByType(ConfigItem.Type.TYPE_BORDER_COLOR);
                color = prefs.getInt(colorItem.getSharedPrefString(), Color.TRANSPARENT);
                drawable = createBorderDrawable(borderType, color, true);
                complicationViewHolder.border.setImageDrawable(drawable);
            }
        }

        @Override
        public void onBackgroundChanged() {
            if (bkgViewGroup != null) {
                bkgViewGroup.removeAllViews();
                addBackgroundView(bkgViewGroup,
                        config.getConfigItemData(ConfigPageData.ConfigType.BACKGROUND,
                            prefs.getInt(AnalogWatchfaceConfig.PREF_BACKGROUND_IDX, AnalogWatchfaceConfig.DEF_BACKGROUND_IDX)
                        ).getResourceId());
                addBackgroundView(bkgViewGroup,
                        config.getConfigItemData(ConfigPageData.ConfigType.HANDS,
                            prefs.getInt(AnalogWatchfaceConfig.PREF_HANDS_SET_IDX, AnalogWatchfaceConfig.DEF_HANDS_SET_IDX)
                        ).getResourceId());
            }
        }

        void addBackgroundView(ViewGroup bkgViewGroup, int resourceId) {
            if (resourceId == 0) {
                return;
            }

            ViewGroup view = (ViewGroup) LayoutInflater.from(bkgViewGroup.getContext()).inflate(R.layout.layout_config_item_page, bkgViewGroup, false);
            Log.d(LOG_TAG, "bounds: " + view.getClipBounds());
            ImageView bkgView = view.findViewById(R.id.image);
            bkgView.setImageDrawable(bkgViewGroup.getContext().getDrawable(resourceId));
            ImageView more = view.findViewById(R.id.label_image);
            more.setImageDrawable(bkgViewGroup.getContext().getDrawable(R.drawable.config_more_items));
            bkgViewGroup.addView(view);
//            bkgViewGroup.invalidate();

        }
    }

    /**
     * Displays color options for the an item on the watch face. These could include border color,
     * background color, etc.
     */
    public static abstract class PickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Button button;
        ConfigItem.Type type;
        Class<? extends Activity> pickerActivity;
        int activityCode;

        protected abstract void addExtraData(Intent intent, ConfigItem.Type type);

        public PickerViewHolder(View view) {
            super(view);

            button = view.findViewById(R.id.button);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            button.setText(name);
        }

        public void setIcon(int resourceId) {
            Context context = button.getContext();
            button.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(resourceId), null, null, null);
        }

        public void setPickerActivity(Class<? extends Activity> activity) {
            pickerActivity = activity;
        }

        public void setType(ConfigItem.Type type) {
            this.type = type;
        }

        public void setActivityCode(int activityCode) {
            this.activityCode = activityCode;
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Log.d(LOG_TAG, "Complication onClick() position: " + position);

            if (pickerActivity != null) {
                Intent launchIntent = new Intent(view.getContext(), pickerActivity);
                addExtraData(launchIntent, type);

                Activity activity = (Activity) view.getContext();
                activity.startActivityForResult(launchIntent, activityCode);
            }
        }
    }

    public static class ColorPickerViewHolder extends PickerViewHolder {

        private int color;

        public ColorPickerViewHolder(View view) {
            super(view);
        }

        @Override
        protected void addExtraData(Intent intent, ConfigItem.Type type) {
            intent.putExtra(ColorPickerActivity.EXTRA_ITEM_ID, type.ordinal());
            intent.putExtra(ColorPickerActivity.EXTRA_COLOR, color);
        }

        public void setColor(int color) {
            this.color = color;
        }
    }

    public static class BorderTypePickerViewHolder extends PickerViewHolder {

        private BorderType borderType;

        public BorderTypePickerViewHolder(View view) {
            super(view);
        }

        @Override
        protected void addExtraData(Intent intent, ConfigItem.Type type) {
            intent.putExtra(BorderPickerActivity.EXTRA_ITEM_ID, type.ordinal());
            intent.putExtra(BorderPickerActivity.EXTRA_BORDER_TYPE, borderType);
        }

        public void setBorderType(BorderType borderType) {
            this.borderType = borderType;
        }
    }

    /**
     * @return true if the activity result was processed successfully and the view should be scrolled to the very first position
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case COMPLICATION_CONFIG_REQUEST_CODE:
                    // Retrieves information for selected Complication provider.
                    ComplicationProviderInfo complicationProviderInfo = data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
                    Log.d(LOG_TAG, "Provider: " + complicationProviderInfo);

                    // Updates preview with new complication information for selected complication id.
                    // Note: complication id is saved and tracked in the adapter class.
                    complicationViewHolder.updateComplicationView(complicationProviderInfo);
                    break;
                case UPDATE_COLORS_CONFIG_REQUEST_CODE:
                    int color = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR, Color.TRANSPARENT);
                    ConfigItem.Type type = ConfigItem.Type.valueOf(data.getExtras().getInt(ColorPickerActivity.EXTRA_ITEM_ID));
                    return updatePreviewColors(color, type);
                case BORDER_TYPE_CONFIG_REQUEST_CODE:
                    type = ConfigItem.Type.valueOf(data.getExtras().getInt(BorderPickerActivity.EXTRA_ITEM_ID));
                    BorderType borderType = (BorderType) data.getExtras().get(BorderPickerActivity.EXTRA_BORDER_TYPE);
                    return updateBorderType(type, borderType);
                default:
                    break;
            }
        }
        return false;
    }


    @Override
    public void onDetachedFromRecyclerView(@NotNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Required to release retriever for active complication data on detach.
        providerInfoRetriever.release();
    }


    private boolean updatePreviewColors(int color, ConfigItem.Type type) {
        Log.d(LOG_TAG, "updatePreviewColors: " + type + ", " + color);
        if (type == null) {
            Log.e(LOG_TAG, "updatePreviewColors: no item type specified");
            return false;
        }

        Drawable drawable;
        switch (type) {
            case TYPE_BORDER_COLOR:
                drawable = createBorderDrawable(getBorderType(), color, true);
                complicationViewHolder.border.setImageDrawable(drawable);
                break;
            case TYPE_DATA_COLOR:
                PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                complicationViewHolder.complication.setColorFilter(colorFilter);
                break;
            case TYPE_BKG_COLOR:
                BorderType borderType = getBorderType();
                drawable = createBorderDrawable(borderType, color, false);
                if (borderType == BorderType.NONE) {
                    ((GradientDrawable)drawable).setColor(color);
                }
                complicationViewHolder.background.setBackground(drawable);
                break;
            default:
                Log.e(LOG_TAG, "updatePreviewColors: " + "unsupported component type: " + type.name());
                return false;
        }
        ColorConfigItem item = (ColorConfigItem) getConfigItemByType(type);
        prefs.edit().putInt(item.getSharedPrefString(), color).commit();
        return true;
    }

    private BorderType getBorderType() {
        Log.d(LOG_TAG, "getBorderType: ");

        ShapeConfigItem borderTypeItem = (ShapeConfigItem) getConfigItemByType(ConfigItem.Type.TYPE_BORDER_TYPE);
        String prefName = borderTypeItem.getSharedPrefString();
        String borderTypeName = prefs.getString(prefName, null);
        Log.d(LOG_TAG, "getBorderType: '" + prefName + "' "+ borderTypeName);
        return BorderType.getByNameOrDefault(borderTypeName);
    }

    private Drawable createBorderDrawable(BorderType borderType, int color, boolean setStroke) {
        Log.d(LOG_TAG, "getBorderDrawable: " + borderType + ", " + color);

        if (borderType == null) {
            borderType = BorderType.NONE;
        }

        GradientDrawable drawable = new GradientDrawable();
        switch (borderType) {
            case RECT:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(0);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, 0);
                break;
            case ROUNDED_RECT:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(BORDER_ROUND_RECT_RADIUS);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, 0);
                break;
            case RING:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(BORDER_RING_RADIUS);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, 0);
                break;
            case DASHED_RECT:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(0);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, BORDER_DASH_LEN);
                break;
            case DASHED_ROUNDED_RECT:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(BORDER_ROUND_RECT_RADIUS);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, BORDER_DASH_LEN);
                break;
            case DASHED_RING:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(BORDER_RING_RADIUS);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, BORDER_DASH_LEN);
                break;
            case DOTTED_RECT:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(0);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, BORDER_DOT_LEN);
                break;
            case DOTTED_ROUNDED_RECT:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(BORDER_ROUND_RECT_RADIUS);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, BORDER_DOT_LEN);
                break;
            case DOTTED_RING:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(BORDER_RING_RADIUS);
                drawableSetBorderStrokeOrColor(drawable, color, setStroke, BORDER_DOT_LEN);
                break;
            default:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(0);
                drawable.setColor(Color.TRANSPARENT);
                break;
        }
        return drawable;
    }

    private void drawableSetBorderStrokeOrColor(GradientDrawable drawable, int color, boolean setStroke, float dashWidth) {
        if (setStroke) {
            drawable.setStroke(BORDER_WIDTH, color, dashWidth, BORDER_GAP_LEN);
        } else {
            drawable.setColor(color);
        }
    }


    private boolean updateBorderType(ConfigItem.Type type, BorderType borderType) {
        Log.d(LOG_TAG, "updateBorderType: " + borderType);
        if (borderType == null) {
            Log.e(LOG_TAG, "updateBorderType: no border type specified");
            return false;
        }

        // use cache ???

        ShapeConfigItem item = (ShapeConfigItem) getConfigItemByType(ConfigItem.Type.TYPE_BORDER_TYPE);
        prefs.edit().putString(item.getSharedPrefString(), borderType.name()).commit();

        // draw border
        ColorConfigItem colorItem = (ColorConfigItem) getConfigItemByType(ConfigItem.Type.TYPE_BORDER_COLOR);
        int color = prefs.getInt(colorItem.getSharedPrefString(), Color.TRANSPARENT);
        Drawable drawable = createBorderDrawable(borderType, color, true);
        complicationViewHolder.border.setImageDrawable(drawable);

        // draw background
        colorItem = (ColorConfigItem) getConfigItemByType(ConfigItem.Type.TYPE_BKG_COLOR);
        color = prefs.getInt(colorItem.getSharedPrefString(), Color.TRANSPARENT);
        drawable = createBorderDrawable(borderType, color, false);
        if (borderType == BorderType.NONE) {
            ((GradientDrawable)drawable).setColor(color);
        }
        complicationViewHolder.background.setBackground(drawable);

        return true;
    }

    private ConfigItem getConfigItemByType(ConfigItem.Type type) {
        for (ConfigItem item : items) {
            if (item.getConfigType() == type) {
                return item;
            }
        }
        return null;
    }
}
