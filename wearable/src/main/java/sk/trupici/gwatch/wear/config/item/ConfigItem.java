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

package sk.trupici.gwatch.wear.config.item;

import java.util.Arrays;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Interface all ConfigItems must implement so the {@link RecyclerView}'s Adapter associated
 * with the configuration activity knows what type of ViewHolder to inflate.
 */
public interface ConfigItem {

    enum Type {
        TYPE_COMPLICATION,
        TYPE_COLOR,
        TYPE_SWITCH,
        TYPE_PADDING,
        TYPE_BORDER_TYPE,
        // complications specific
        TYPE_BORDER_COLOR,
        TYPE_DATA_COLOR,
        TYPE_BKG_COLOR,
        ;

        public static Type valueOf(int ordinal) {
            return Arrays.stream(values()).filter(x -> x.ordinal() == ordinal).findFirst().orElseThrow(IllegalArgumentException::new);
        }
    }

    Type getConfigType();
}
