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

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.R;

public class ListConfigViewHolder extends WearableRecyclerView.ViewHolder {
    final private TextView title;
    final private WearableRecyclerView recyclerView;
    final private int viewId;

    public ListConfigViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.page_title);
        recyclerView = itemView.findViewById(R.id.wearable_recycler_view);
        viewId = itemView.getId();
    }

    public TextView getTitle() {
        return title;
    }

    public WearableRecyclerView getRecyclerView() {
        return recyclerView;
    }

    public int getViewId() {
        return viewId;
    }
}
