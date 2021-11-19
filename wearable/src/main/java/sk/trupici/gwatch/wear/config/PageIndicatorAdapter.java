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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;

import androidx.viewpager2.widget.ViewPager2;
import sk.trupici.gwatch.wear.R;

public class PageIndicatorAdapter extends ViewPager2.OnPageChangeCallback {

    final private ViewGroup indicatorView;
    final private ViewPager2 pager;
    final private int numElements;

    public PageIndicatorAdapter(@NotNull ViewGroup indicatorView, int numElements, ViewPager2 pager) {
        this.indicatorView = indicatorView;
        this.numElements = numElements;
        this.pager = pager;

        onCreate();
    }

    private void onCreate() {
        createIndicatorElements();

        pager.registerOnPageChangeCallback(this);
    }

    public void destroy() {
        pager.unregisterOnPageChangeCallback(this);
    }

    @Override
    public void onPageSelected(int position) {
        updateIndicator(position);
    }

    private void createIndicatorElements() {
        for (int i=0; i < numElements; i++) {
            createIndicatorElement(i);
        }
    }

    private void createIndicatorElement(int position) {
        final Context context = indicatorView.getContext();
        final ImageView childView = new ImageView(context);
        childView.setId(position);
        childView.setImageDrawable(context.getDrawable(R.drawable.circle_8));
        childView.setColorFilter(context.getColor(R.color.page_indicator_inactive));
        childView.setPadding(2, 2, 2, 2);
        childView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        childView.setForegroundGravity(Gravity.BOTTOM);

        indicatorView.addView(childView);
    }

    private void updateIndicator(int selectedPosition) {
        final Context context = indicatorView.getContext();

        final Drawable defDrawable = context.getDrawable(R.drawable.circle_8);
        final int defColor = context.getColor(R.color.page_indicator_inactive);

        for (int i=0; i < indicatorView.getChildCount(); i++) {
            ImageView childView = (ImageView) indicatorView.getChildAt(i);
            if (i == selectedPosition) {
                childView.setImageDrawable(context.getDrawable(R.drawable.circle_10));
                childView.setColorFilter(context.getColor(R.color.page_indicator_active));
            } else {
                childView.setImageDrawable(defDrawable);
                childView.setColorFilter(defColor);
            }
        }
    }

}