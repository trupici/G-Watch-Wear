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
import android.widget.Switch;

import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.PreferenceUtils;

/**
 * Simple 2-state switch config item view holder
 */
public class SwitchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    final private Switch button;
    private String prefName;

    public SwitchViewHolder(View view) {
        super(view);
        button = view.findViewById(R.id.switch_button);
        view.setOnClickListener(this);
    }

    public void init(int labelId, String prefName, int defaultValueId) {
        this.prefName = prefName;

        Context context = button.getContext();
        button.setText(context.getString(labelId));

        boolean defaultValue = defaultValueId != -1 && context.getResources().getBoolean(defaultValueId);
        boolean isOn = PreferenceUtils.isConfigured(button.getContext(), prefName, defaultValue);
        button.setChecked(isOn);
    }

    @Override
    public void onClick(View view) {
        PreferenceUtils.setIsConfigured(view.getContext(), prefName, button.isChecked());
    }
}
