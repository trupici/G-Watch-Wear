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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

/**
 * Simple 2-state switch config item view holder
 */
public class SwitchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    final private Switch button;
// 2    final private TextView label;
    private String prefName;
// 1   private int offIconId;
// 1   private int onIconId;
// 1,2   private boolean isOn;

    public SwitchViewHolder(View view) {
        super(view);
        button = view.findViewById(R.id.switch_button);
// 2        label = view.findViewById(R.id.switch_label);
        view.setOnClickListener(this);
    }

    public void init(int labelId, int offIconId, int onIconId, String prefName, int defaultValueId) {
// 1       this.offIconId = offIconId;
// 1       this.onIconId = onIconId;
        this.prefName = prefName;

        Context context = button.getContext();
// 2       label.setText(context.getString(labelId));
        button.setText(context.getString(labelId));

        boolean defaultValue = defaultValueId != -1 && context.getResources().getBoolean(defaultValueId);
// 1,2       isOn = PreferenceUtils.isConfigured(button.getContext(), prefName, defaultValue);
        boolean isOn = PreferenceUtils.isConfigured(button.getContext(), prefName, defaultValue);
        button.setChecked(isOn);

// 1       int iconId = isOn ? onIconId : offIconId;
// 1       button.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(iconId), null, null, null);
    }

    @Override
    public void onClick(View view) {
        PreferenceUtils.setIsConfigured(view.getContext(), prefName, button.isChecked());
// 1,2       isOn = !isOn;
// 1,2       PreferenceUtils.setIsConfigured(view.getContext(), prefName, isOn);
// 1,2       button.setChecked(isOn);

// 1       int iconId = isOn ? onIconId : offIconId;
// 1       button.setCompoundDrawablesWithIntrinsicBounds(view.getContext().getDrawable(iconId), null, null, null);
    }
}
