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

package sk.trupici.gwatch.wear.config.menu;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.item.BorderConfigItem;
import sk.trupici.gwatch.wear.config.BorderType;

public class BorderTypeMenuItems {
    final public static BorderConfigItem[] items = {
            new BorderConfigItem(BorderType.NONE,
                R.string.border_type_none,
                R.drawable.config_no_border_24),
            new BorderConfigItem(BorderType.ROUNDED_RECT,
                    R.string.border_type_round_rect,
                    R.drawable.config_border_round_rect_24),
            new BorderConfigItem(BorderType.RECT,
                    R.string.border_type_rect,
                    R.drawable.config_border_rect_24),
            new BorderConfigItem(BorderType.RING,
                    R.string.border_type_ring,
                    R.drawable.config_border_circle_24),
            new BorderConfigItem(BorderType.DASHED_ROUNDED_RECT,
                    R.string.border_type_dashed_round_rect,
                    R.drawable.config_border_round_rect_24),
            new BorderConfigItem(BorderType.DASHED_RECT,
                    R.string.border_type_dashed_rect,
                    R.drawable.config_border_rect_24),
            new BorderConfigItem(BorderType.DASHED_RING,
                    R.string.border_type_dashed_ring,
                    R.drawable.config_border_circle_dash_24),
            new BorderConfigItem(BorderType.DOTTED_ROUNDED_RECT,
                    R.string.border_type_dotted_round_rect,
                    R.drawable.config_border_round_rect_24),
            new BorderConfigItem(BorderType.DOTTED_RECT,
                    R.string.border_type_dotted_rect,
                    R.drawable.config_border_rect_24),
            new BorderConfigItem(BorderType.DOTTED_RING,
                    R.string.border_type_dotted_ring,
                    R.drawable.config_border_circle_24)
    };
}
