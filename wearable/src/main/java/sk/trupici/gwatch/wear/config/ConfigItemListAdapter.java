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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.item.BasicConfigItem;
import sk.trupici.gwatch.wear.config.item.BoolConfigItem;
import sk.trupici.gwatch.wear.config.item.ConfigItem;

import static android.app.Activity.RESULT_OK;
import static sk.trupici.gwatch.wear.util.CommonConstants.BORDER_TYPE_CONFIG_REQUEST_CODE;
import static sk.trupici.gwatch.wear.util.CommonConstants.UPDATE_COLORS_CONFIG_REQUEST_CODE;

/**
 * {@code Adapter} for {@code ConfigItem} list
 */
public class ConfigItemListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ActivityResultAware {

    final private static String LOG_TAG = ConfigItemListAdapter.class.getSimpleName();

    final private Context context;
    final private ConfigItem[] items;

    final private SharedPreferences prefs;

//    final private RecyclerView recyclerView;

    final private int configId;

    final private WatchfaceConfig watchfaceConfig;

    public ConfigItemListAdapter(Context context, RecyclerView parent, int configId,
                                 ConfigItem[] items, SharedPreferences prefs, WatchfaceConfig watchfaceConfig) {

        this.configId = configId;

        this.context = context;
        this.items = items;

        this.prefs = prefs;
//        this.recyclerView = parent;

        this.watchfaceConfig = watchfaceConfig;
    }

    @NonNull
    @Override
    // viewType holds item position here, see getViewType
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onCreateViewHolder(): " + position);
        }
        RecyclerView.ViewHolder viewHolder;
        ConfigItem configItem = items[position];
        ConfigItem.Type type = configItem.getConfigType(); //ConfigItem.Type.valueOf(viewType);
        switch (type) {
            case TYPE_COLOR:
                viewHolder = new PickerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_button_item, parent, false),
                        view -> {
//                            int position = recyclerView.getChildLayoutPosition(view);
                            if (BuildConfig.DEBUG) {
                                Log.i(LOG_TAG, "onClick: " + position);
                            }
                            Intent launchIntent = new Intent(context, ColorPickerActivity.class);
                            launchIntent.putExtra(ColorPickerActivity.EXTRA_ITEM_ID, position);
                            launchIntent.putExtra(ColorPickerActivity.EXTRA_ITEM_TYPE, type.ordinal());
                            launchIntent.putExtra(ColorPickerActivity.EXTRA_COLOR, getItemColor(position));
                            ((Activity)context).startActivityForResult(launchIntent, UPDATE_COLORS_CONFIG_REQUEST_CODE);
                        }
                );
                break;
            case TYPE_BORDER_TYPE:
                viewHolder = new PickerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_button_item, parent, false),
                        view -> {
//                            int position = recyclerView.getChildLayoutPosition(view);
                            if (BuildConfig.DEBUG) {
                                Log.i(LOG_TAG, "onClick: " + position);
                            }
                            Intent launchIntent = new Intent(context, BorderPickerActivity.class);
                            launchIntent.putExtra(BorderPickerActivity.EXTRA_ITEM_ID, position);
                            launchIntent.putExtra(BorderPickerActivity.EXTRA_ITEM_TYPE, type.ordinal());
                            launchIntent.putExtra(BorderPickerActivity.EXTRA_BORDER_TYPE, getBorderType(position));
                            ((Activity)context).startActivityForResult(launchIntent, BORDER_TYPE_CONFIG_REQUEST_CODE);
                        });
                break;
            case TYPE_SWITCH:
                viewHolder = new SwitchViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_switch_item, parent, false));
                break;
            case TYPE_PADDING:
                viewHolder = new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_empty_item, parent, false)) {
                };
                break;
            default:
                throw new IllegalArgumentException(""+type);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "onBindViewHolder " + position);
        }
        // Pulls all data required for creating the UX for the specific setting option.
        ConfigItem configItem = items[position];
        ConfigItem.Type type = configItem.getConfigType();

        switch (type) {
            case TYPE_COLOR:
            case TYPE_BORDER_TYPE:
                PickerViewHolder pickerViewHolder = (PickerViewHolder) holder;
                BasicConfigItem basicConfigItem = (BasicConfigItem) configItem;
                pickerViewHolder.setIcon(basicConfigItem.getIconResourceId());
                pickerViewHolder.setName(basicConfigItem.getLabelResourceId());
                break;
            case TYPE_SWITCH:
                SwitchViewHolder switchViewHolder = (SwitchViewHolder) holder;
                BoolConfigItem boolConfigItem = (BoolConfigItem) configItem;
                boolean defaultValue = boolConfigItem.getDefaultValueResourceId() == -1
                        ? watchfaceConfig.getBoolPrefDefaultValue(context, boolConfigItem.getPreferenceName())
                        : context.getResources().getBoolean(boolConfigItem.getDefaultValueResourceId());
                switchViewHolder.init(
                        boolConfigItem.getLabelResourceId(),
                        (boolConfigItem.isGlobal() ? "" : watchfaceConfig.getPrefsPrefix()) + boolConfigItem.getPreferenceName(),
                        defaultValue);
                break;
            case TYPE_PADDING:
                break;
            default:
                throw new IllegalArgumentException(""+type);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position; // return direct item position
//        ConfigItem configItem = items.get(position);
//        return configItem.getConfigType().ordinal();
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    private int getItemColor(int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "getItemColor: " + position);
        }
        BasicConfigItem colorTypeItem = (BasicConfigItem) items[position];
        String prefName = (colorTypeItem.isGlobal() ? "" : watchfaceConfig.getPrefsPrefix()) + colorTypeItem.getPreferenceName();
        int defaultValue = colorTypeItem.getDefaultValueResourceId() == -1 ? Color.TRANSPARENT
                : context.getResources().getColor(colorTypeItem.getDefaultValueResourceId(), null);
        int color = prefs.getInt(prefName, defaultValue);
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "getItemColor: '" + prefName + "' " + color);
        }
        return color;
    }

    private BorderType getBorderType(int position) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "getBorderType: " + position);
        }

        BasicConfigItem borderTypeItem = (BasicConfigItem) items[position];
        String prefName = (borderTypeItem.isGlobal() ? "" : watchfaceConfig.getPrefsPrefix()) + borderTypeItem.getPreferenceName();
        String borderTypeName = prefs.getString(prefName, null);
        if (borderTypeName == null && borderTypeItem.getDefaultValueResourceId() != -1) {
            borderTypeName = context.getResources().getString(borderTypeItem.getDefaultValueResourceId());
        }
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "getBorderType: '" + prefName + "' " + borderTypeName);
        }
        return BorderType.getByNameOrDefault(borderTypeName);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case UPDATE_COLORS_CONFIG_REQUEST_CODE:
                    int color = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR, Color.TRANSPARENT);
                    int id = (Integer) data.getExtras().get(ColorPickerActivity.EXTRA_ITEM_ID);
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "onActivityResult: itemId=" + id);
                    }
                    BasicConfigItem colorConfigItem = (BasicConfigItem) items[id];
                    String prefName = (colorConfigItem.isGlobal() ? "" : watchfaceConfig.getPrefsPrefix()) + colorConfigItem.getPreferenceName();
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "update color: " + prefName + " -> " + color);
                    }
                    prefs.edit().putInt(prefName, color).commit();
                    break;
                case BORDER_TYPE_CONFIG_REQUEST_CODE:
                    BorderType borderType = (BorderType) data.getExtras().get(BorderPickerActivity.EXTRA_BORDER_TYPE);
                    id = (Integer) data.getExtras().get(BorderPickerActivity.EXTRA_ITEM_ID);
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "onActivityResult: item Id=" + id);
                    }
                    BasicConfigItem shapeConfigItem = (BasicConfigItem) items[id];
                    prefName = (shapeConfigItem.isGlobal() ? "" : watchfaceConfig.getPrefsPrefix()) + shapeConfigItem.getPreferenceName();
                    if (BuildConfig.DEBUG) {
                        Log.d(LOG_TAG, "update border: " + prefName + " -> " + borderType);
                    }
                    prefs.edit().putString(prefName, borderType.name()).commit();
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    public int getConfigId() {
        return configId;
    }
}
