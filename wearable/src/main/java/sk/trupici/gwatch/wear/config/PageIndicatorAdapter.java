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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import sk.trupici.gwatch.wear.R;

/**
 * Adapter class for active page index indicator
 */
public class PageIndicatorAdapter extends ViewPager2.OnPageChangeCallback {

    private final static int INDICATOR_DELAY_MS = 800;
    private final static int INDICATOR_DURATION_MS = 500;

    final private ViewGroup indicatorView;
    final private ViewPager2 pager;
    final private int numElements;

    public PageIndicatorAdapter(@NonNull ViewGroup indicatorView, int numElements, ViewPager2 pager) {
        this.indicatorView = indicatorView;
        this.numElements = numElements;
        this.pager = pager;

        onCreate();
    }

    public void showToFade() {
        if (pager.getOrientation() != ViewPager2.ORIENTATION_VERTICAL) {
            return;
        }

        if (indicatorView.getVisibility() != View.VISIBLE) {
            indicatorView.setAlpha(1f);
            indicatorView.setVisibility(View.VISIBLE);
        }

        Animation anim = indicatorView.getAnimation();
        if (anim == null) {
            anim = new AlphaAnimation(1, 0);
            anim.setDuration(INDICATOR_DURATION_MS);
            anim.setStartOffset(INDICATOR_DELAY_MS);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    indicatorView.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            indicatorView.startAnimation(anim);
        } else {
            anim.reset();
            anim.start();
        }
    }

    private void onCreate() {
        createIndicatorElements();

        pager.registerOnPageChangeCallback(this);

        showToFade();
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
                if (pager.getOrientation() == ViewPager2.ORIENTATION_HORIZONTAL) {
                    childView.setColorFilter(context.getColor(R.color.page_indicator_active));
                } else {
                    childView.setColorFilter(context.getColor(R.color.page_vert_indicator_active));
                }
            } else {
                childView.setImageDrawable(defDrawable);
                childView.setColorFilter(defColor);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        super.onPageScrollStateChanged(state);
        if (state == ViewPager2.SCROLL_STATE_IDLE) {
            showToFade();
        } else if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
            if (indicatorView.getVisibility() != View.VISIBLE) {
                indicatorView.setAlpha(1f);
                indicatorView.setVisibility(View.VISIBLE);
            }
        }
    }

}