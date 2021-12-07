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

import android.support.wearable.complications.ComplicationData;

public class Config {

    private static ComplicationConfig config[] = {
            new ComplicationConfig(
                    ComplicationId.LEFT_COMPLICATION_ID,
                    ComplicationLocation.LEFT,
                    new int[] {
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_LONG_TEXT,
                            ComplicationData.TYPE_SMALL_IMAGE,
                            ComplicationData.TYPE_ICON
                    }),
            new ComplicationConfig(ComplicationId.RIGHT_COMPLICATION_ID,
                    ComplicationLocation.RIGHT,
                    new int[] {
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_LONG_TEXT,
                            ComplicationData.TYPE_SMALL_IMAGE,
                            ComplicationData.TYPE_ICON
                    })
    };

    final static int[] idsArray;

    static {
        idsArray = new int[config.length];
        for (int i=0; i<idsArray.length; i++) {
            idsArray[i] = config[i].getId();
        }
    }

    public static int getComplicationCount() {
        return config.length;
    }

    public static ComplicationConfig[] getConfig() {
        return config;
    }

    public static int[] getComplicationIds() {
        return idsArray;
    }

    public static ComplicationConfig getComplicationConfig(ComplicationId id) {
        for (ComplicationConfig cc : config) {
            if (cc.getComplicationId() == id) {
                return cc;
            }
        }
        return null;
    }

    public static ComplicationConfig getComplicationConfig(int id) {
        for (ComplicationConfig cc : config) {
            if (cc.getComplicationId().ordinal() == id) {
                return cc;
            }
        }
        return null;
    }

    public static ComplicationConfig getComplicationConfig(ComplicationLocation location) {
        for (ComplicationConfig config : config) {
            if (config.getLocation() == location) {
                return config;
            }
        }
        return null;
    }

    public static int getComplicationId(ComplicationLocation location) {
        ComplicationConfig config = getComplicationConfig(location);
        return config != null ? config.getId() : -1;
    }
}
