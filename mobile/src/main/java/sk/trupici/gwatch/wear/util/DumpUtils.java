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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class DumpUtils {

    final private static String LOG_TAG = DumpUtils.class.getSimpleName();

    public static void dumpView(View view, String indent) {
        Log.d(LOG_TAG, "dumpView: " + indent + view);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int j = 0; j < viewGroup.getChildCount(); j++) {
                dumpView(viewGroup.getChildAt(j), indent+"  ");
            }
        }
    }

    public static void dumpIntent(Intent intent) {
        String indent = "   ";
        Log.i(LOG_TAG, "\n");
        Log.i(LOG_TAG, indent + "Intent: " + intent.getAction());
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            dumpBundle(bundle, indent);
        } else {
            Log.i(LOG_TAG, indent + "No extras data");
        }
        Log.i(LOG_TAG, "\n");
    }

    public static void dumpBundle(Bundle bundle, String indent) {
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.i(LOG_TAG, indent + key + ": " + value);
            }
        }
    }

    public static String dumpData(byte buffer[], int len) {

        if (len == 0) {
            return StringUtils.EMPTY_STRING;
        }

        StringBuffer str1 = new StringBuffer(64);
        StringBuffer str2 = new StringBuffer(24);
        StringBuffer str = new StringBuffer().append(" \n");

        // shrink size if it is too big
        int maxlen = Math.min(len, 10240);

        for (int i = 0; i < maxlen; i++) {
            if (i > 0 && (i & 7) == 0) {
                if ((i & 15) == 0) {
                    str.append("  ").append(str1.toString()).append(":   ").append(str2).append("\n");
                    str1.setLength(0);
                    str2.setLength(0);
                } else {
                    str1.append(" ");
                }
            }

            byte b = buffer[i];
            str1.append(String.format("%02x ", b));
            char c = (char) b;
            str2.append(isPrintableChar(c) ? ""+c : ".");
        }

        str.append("  ").append(String.format("%-49s", str1.toString())).append(":   ").append(str2).append("\n");

        if (maxlen < len) {
            str.append("\n  ...");
        }

        return str.toString();
    }

    public static boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) && block != null && block != Character.UnicodeBlock.SPECIALS;
    }
}
