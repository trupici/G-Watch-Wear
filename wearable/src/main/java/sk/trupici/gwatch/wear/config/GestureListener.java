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

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {
    private final int Y_BUFFER = 10;

    @Override
    public boolean onDown(MotionEvent e) {
        // Prevent ViewPager from intercepting touch events as soon as a DOWN is detected.
        // If we don't do this the next MOVE event may trigger the ViewPager to switch
        // tabs before this view can intercept the event.
        Log.d("vp", "true1");
//        recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
        return super.onDown(e);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (Math.abs(distanceX) > Math.abs(distanceY)) {
            Log.d("vp2", "true");
            // Detected a horizontal scroll, allow the viewpager from switching tabs
//            recyclerView.getParent().requestDisallowInterceptTouchEvent(false);
        } else if (Math.abs(distanceY) > Y_BUFFER) {
            // Detected a vertical scroll prevent the viewpager from switching tabs
            Log.d("vp3", "false");
//            recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return super.onScroll(e1, e2, distanceX, distanceY);
    }
}