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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import androidx.recyclerview.widget.RecyclerView;
import sk.trupici.gwatch.wear.R;

/**
 * Displays options for the item on the watch face. These could include border, color, etc.
 */
public class PickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final static String LOG_TAG = PickerViewHolder.class.getSimpleName();

    private Button button;

    private final Consumer<View> onClickCallback;

    public PickerViewHolder(View view, Consumer<View> onClickCallback) {
        super(view);

        this.onClickCallback = onClickCallback;

        button = view.findViewById(R.id.button);
        view.setOnClickListener(this);
    }

    public void setName(int resourceId) {
        Context context = button.getContext();
        button.setText(context.getString(resourceId));
    }

    public void setIcon(int resourceId) {
        Context context = button.getContext();
        button.setCompoundDrawablesWithIntrinsicBounds(null, null, context.getDrawable(resourceId), null);
    }

    @Override
    public void onClick(View view) {
        if (onClickCallback != null) {
            onClickCallback.accept(view);
        }
    }
}
