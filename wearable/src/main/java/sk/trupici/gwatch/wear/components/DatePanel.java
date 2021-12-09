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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.wearable.watchface.WatchFaceService;
import android.text.TextPaint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.util.CommonConstants;

public class DatePanel implements ComponentPanel {
    public static final String LOG_TAG = CommonConstants.LOG_TAG;

    private Calendar calendar;
    private DateFormat dayOfWeekFormat;
    private DateFormat monthFormat;
    private DateFormat dateOfMonthFormat;

    private RectF sizeFactors;
    private Rect bounds;
    private TextPaint paint;

    final private int refScreenWidth;
    final private int refScreenHeight;

    private boolean timeZoneRegistered = false;
    private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            calendar.setTimeZone(TimeZone.getDefault());
            initDateFormats();
        }
    };

    public DatePanel(int screenWidth, int screenHeight) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {

        calendar = Calendar.getInstance();
        paint = new TextPaint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextScaleX(0.9f);
        initDateFormats();

        sizeFactors = new RectF(
                context.getResources().getDimension(R.dimen.layout_center_compl_left) / refScreenWidth,
                context.getResources().getDimension(R.dimen.layout_center_compl_top) / refScreenHeight,
                context.getResources().getDimension(R.dimen.layout_center_compl_right) / refScreenWidth,
                context.getResources().getDimension(R.dimen.layout_center_compl_bottom) / refScreenHeight
        );

    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {

        // date component
        int left = (int) (sizeFactors.left * width);
        int top = (int) (sizeFactors.top * height);
        int right = (int) (sizeFactors.right * width);
        int bottom = (int) (sizeFactors.bottom * height);
        bounds = new Rect(left, top, right, bottom);
        paint.setTextSize(bounds.height() / 2.2f);
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        Date date = calendar.getTime();

        // FIXME - configurable colors

//            // draw background if needed
//            datePaint.setColor(Color.GREEN);
//            datePaint.setStyle(Paint.Style.FILL);
//            canvas.drawRect(datePanelBounds, datePaint);

        int color = isAmbientMode ? Color.LTGRAY : Color.WHITE;
        paint.setColor(color);

        int centerX = bounds.left + bounds.width() / 2;
        if (false) {
            // Day of week
            canvas.drawText(dayOfWeekFormat.format(date),
                    centerX, bounds.top + bounds.height() / 2f - 5,
                    paint);
            // Day of Month
            canvas.drawText(dateOfMonthFormat.format(date),
                    centerX, bounds.bottom - 5,
                    paint);
        } else {
            // Day of Month
            canvas.drawText(dateOfMonthFormat.format(date),
                    centerX, bounds.top + bounds.height()/2f - 5,
                    paint);
            // Month
            canvas.drawText(monthFormat.format(date),
                    centerX, bounds.bottom - 5,
                    paint);
        }
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {
        /* Update time zone in case it changed while we weren"t visible. */
        calendar.setTimeZone(TimeZone.getDefault());
        initDateFormats();
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {

    }

    private void initDateFormats() {
        dayOfWeekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        dayOfWeekFormat.setCalendar(calendar);
        monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        monthFormat.setCalendar(calendar);
        dateOfMonthFormat = new SimpleDateFormat("d", Locale.getDefault());
        dateOfMonthFormat.setCalendar(calendar);
    }

    public void registerReceiver(WatchFaceService watchFaceService) {
        if (timeZoneRegistered) {
            return;
        }
        timeZoneRegistered = true;
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
        watchFaceService.registerReceiver(timeZoneReceiver, filter);
    }

    public void unregisterReceiver(WatchFaceService watchFaceService) {
        if (!timeZoneRegistered) {
            return;
        }
        timeZoneRegistered = false;
        watchFaceService.unregisterReceiver(timeZoneReceiver);
    }

}
