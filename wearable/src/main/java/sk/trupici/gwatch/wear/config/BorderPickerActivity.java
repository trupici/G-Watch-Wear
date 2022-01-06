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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.item.BorderConfigItem;
import sk.trupici.gwatch.wear.config.menu.BorderTypeMenuItems;

public class BorderPickerActivity extends Activity {

    public static final String EXTRA_ITEM_ID = "itemId";
    public static final String EXTRA_ITEM_TYPE = "itemType";
    public static final String EXTRA_BORDER_TYPE = "borderType";

    final private static BorderConfigItem[] items = BorderTypeMenuItems.items;

    private BorderType borderType;
    private Integer itemId;
    private Integer itemType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent data = getIntent();
        if (data != null) {
            itemId = data.getExtras().getInt(EXTRA_ITEM_ID);
            itemType = data.getExtras().getInt(EXTRA_ITEM_TYPE);
            borderType = (BorderType) data.getExtras().get(EXTRA_BORDER_TYPE);
        }

        if (borderType == null) {
            borderType = BorderType.NONE;
        }

        setContentView(R.layout.activity_border_selection_config);

        WearableRecyclerView recyclerView = findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        recyclerView.setEdgeItemsCenteringEnabled(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Improves performance because we know changes in content do not change the layout size of the RecyclerView.
        recyclerView.setHasFixedSize(true);

        recyclerView.setAdapter(new BorderSelectionAdapter());

        recyclerView.scrollToPosition(getItemPosition(borderType));
    }

    private int getItemPosition(BorderType type) {
        for (int i=0; i < items.length; i++) {
            if (items[i].getId() == type) {
                return i;
            }
        }
        return 0;
    }

    void closeAndReturnResult(BorderType borderType) {
        Intent data = new Intent();
        data.putExtra(EXTRA_ITEM_ID, itemId);
        data.putExtra(EXTRA_ITEM_TYPE, itemType);
        data.putExtra(EXTRA_BORDER_TYPE, borderType);
        setResult(RESULT_OK, data);
        finish();
    }

    class BorderSelectionAdapter extends RecyclerView.Adapter<BorderItemViewHolder> {

        @NonNull
        @Override
        public BorderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_border_item, parent, false);
            return new BorderItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BorderItemViewHolder holder, int position) {
            BorderConfigItem item = items[position];

            Context context = holder.itemView.getContext();

            holder.setId(item.getId());
            holder.getButton().setText(context.getString(item.getLabelResourceId()));
            holder.getButton().setCompoundDrawablesWithIntrinsicBounds(null, null, context.getDrawable(item.getIconResourceId()), null);
        }

        @Override
        public int getItemCount() {
            return items.length;
        }
    }

    class BorderItemViewHolder extends WearableRecyclerView.ViewHolder implements View.OnClickListener {
        BorderType id;
        Button button;

        public BorderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.border);
            button.setOnClickListener(this);
        }

        public BorderType getId() {
            return id;
        }

        public void setId(BorderType id) {
            this.id = id;
        }

        public Button getButton() {
            return button;
        }

        @Override
        public void onClick(View v) {
            closeAndReturnResult(id);
        }
    }
}
