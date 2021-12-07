/*
 * Copyright (C) 2019 Juraj Antal
 *
 * Originally created in G-Watch App
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

package sk.trupici.gwatch.wear.view;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class HorizontalSwipeDetector extends GestureDetector.SimpleOnGestureListener {
    private static final int SWIPE_MIN_DISTANCE = 50;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    final private SwipeListener listener;

    public HorizontalSwipeDetector(SwipeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            float diffY = e1.getY() - e2.getY();
            float diffX = e1.getX() - e2.getX();

            if (Math.abs(diffY) > SWIPE_MAX_OFF_PATH || Math.abs(velocityX) < SWIPE_THRESHOLD_VELOCITY) {
                return false;
            }

            if (diffX > SWIPE_MIN_DISTANCE) { // Left swipe
                return listener.onLeftSwipe();
            } else if (-diffX > SWIPE_MIN_DISTANCE) { // Right swipe
                return listener.onRightSwipe();
            }
        } catch (Exception e) {
            Log.e("HorizontalSwipeDetector", "Error on gestures");
        }
        return false;
    }


    /**
     * Interface with horizontal swipe listener callbacks.
     *
     * Methods should return true if the event is consumed, otherwise false
     */
    public interface SwipeListener {
        boolean onLeftSwipe();
        boolean onRightSwipe();
    }
}
