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

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BackgroundChangeAware;

public class ComplicationsConfigViewHolder extends WearableRecyclerView.ViewHolder implements BackgroundChangeAware {
    final private TextView title;
    final private WearableRecyclerView recyclerView;

    public ComplicationsConfigViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.page_title);
        recyclerView = itemView.findViewById(R.id.wearable_recycler_view);
    }

    @Override
    public void onBackgroundChanged() {
        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            View view = recyclerView.getChildAt(i);
            if (view != null) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
                if (holder instanceof BackgroundChangeAware) {
                    ((BackgroundChangeAware)holder).onBackgroundChanged();
                }
            }
        }
    }

    public TextView getTitle() {
        return title;
    }

    public WearableRecyclerView getRecyclerView() {
        return recyclerView;
    }
}
