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

package sk.trupici.gwatch.wear.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;

public interface ComponentPanel {

    void onCreate(Context context, SharedPreferences sharedPrefs);

    void onSizeChanged(Context context, int width, int height);

    void onConfigChanged(Context context, SharedPreferences sharedPrefs);

    void onPropertiesChanged(Context context, Bundle properties);

    void onDraw(Canvas canvas, boolean isAmbientMode);
}
