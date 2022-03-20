/*
 * Copyright (C) 2022 Juraj Antal
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
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import sk.trupici.gwatch.wear.components.BgGraph;

public class BgGraphView extends View {

    private BgGraph bgGraph;
    private final RectF bounds;

    public BgGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.bounds = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bgGraph != null) {
            bgGraph.draw(canvas, false);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (right > 0 && bottom > 0) {
            bounds.left = 0;
            bounds.top = 0;
            bounds.right = getWidth();
            bounds.bottom = getHeight();
            bgGraph.resize(bounds);
        }
    }

    public void setBgGraph(BgGraph bgGraph) {
        this.bgGraph = bgGraph;
    }
}
