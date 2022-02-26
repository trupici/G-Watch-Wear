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

package sk.trupici.gwatch.wear.util;

import android.content.Context;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Date;

public class DumpUtils {

    final private static String LOG_TAG = DumpUtils.class.getSimpleName();

    public static void dumpComplicationData(Context context, ComplicationData complicationData) {
        String indent = "   ";
        Log.i(LOG_TAG, "\n");
        Log.i(LOG_TAG, indent + "Complication data:" + complicationData.toString());
        Log.i(LOG_TAG, indent + "describe contents: " + complicationData.describeContents());
        Log.i(LOG_TAG, indent + "Type             : " + getComplicationDataType(complicationData.getType()));

        Bundle fields = (Bundle)getPrivateFieldValue(complicationData, "mFields");

        if (fields != null) {
            for (String key : fields.keySet()) {
                Object value = fields.get(key);
                if (value instanceof ComplicationText) {
                    ComplicationText complicationText = (ComplicationText) value;
                    Log.d(LOG_TAG, indent + key + ": " + complicationText.getText(context, new Date().getTime()));
                } else if (value instanceof Number) {
                    Log.i(LOG_TAG, indent + key + ": " + value + " [" + value.getClass().getSimpleName() +"]");
                } else {
                    Log.i(LOG_TAG, indent + key + ": " + value);
                }
            }
        }
    }

    public static String getComplicationDataType(Integer type) {
        if (type == null) {
            return "null";
        }
        switch (type) {
            case ComplicationData.TYPE_NOT_CONFIGURED:
                return "TYPE_NOT_CONFIGURED";
            case ComplicationData.TYPE_EMPTY:
                return "TYPE_EMPTY";
            case ComplicationData.TYPE_NO_DATA:
                return "TYPE_NO_DATA";
            case ComplicationData.TYPE_SHORT_TEXT:
                return "TYPE_SHORT_TEXT";
            case ComplicationData.TYPE_LONG_TEXT:
                return "TYPE_LONG_TEXT";
            case ComplicationData.TYPE_RANGED_VALUE:
                return "TYPE_RANGED_VALUE";
            case ComplicationData.TYPE_ICON:
                return "TYPE_ICON";
            case ComplicationData.TYPE_SMALL_IMAGE:
                return "TYPE_SMALL_IMAGE";
            case ComplicationData.TYPE_LARGE_IMAGE:
                return "TYPE_LARGE_IMAGE";
            case ComplicationData.TYPE_NO_PERMISSION:
                return "TYPE_NO_PERMISSION";
            default:
                return "UNKNOWN";
        }
    }

    private static Object getPrivateFieldValue(Object obj, String fieldName) {
        try {
            Field field = ComplicationData.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
            return null;
        }
    }
}
