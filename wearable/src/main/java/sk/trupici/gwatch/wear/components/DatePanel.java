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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.WatchFaceService;
import android.text.TextPaint;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BorderType;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.util.BorderUtils;
import sk.trupici.gwatch.wear.util.UiUtils;

import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_DASH_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_DOT_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_GAP_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_RING_RADIUS;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_ROUND_RECT_RADIUS;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_WIDTH;

public class DatePanel implements ComponentPanel {
    public static final String LOG_TAG = DatePanel.class.getSimpleName();

    public static final int CONFIG_ID = 14;

    public static final String PREF_SHOW_MONTH = "date_show_month";

    public static final String PREF_BKG_COLOR = "date_background_color";
    public static final String PREF_MONTH_COLOR = "date_month_color";
    public static final String PREF_DAY_OF_MONTH_COLOR = "date_day_of_month_color";
    public static final String PREF_DAY_OF_WEEK_COLOR = "date_day_of_week_color";

    public static final String PREF_BORDER_COLOR = "date_border_color";
    public static final String PREF_BORDER_TYPE = "date_border_type";

    private static final int PADDING = 2;
    private static final int TOP_OFFSET = 2;

    final private int refScreenWidth;
    final private int refScreenHeight;

    final private WatchfaceConfig watchfaceConfig;

    private boolean showMonth;

    private int backgroundColor;
    private int monthColor;
    private int dayOfMonthColor;
    private int dayOfWeekColor;

    private int borderColor;
    private BorderType borderType;

    private Calendar calendar;
    private DateFormat dayOfWeekFormat;
    private DateFormat monthFormat;
    private DateFormat dayOfMonthFormat;
    private DateFormat dayOfWeekFullNameFormat;
    private DateFormat monthFullNameFormat;

    private RectF sizeFactors;
    private RectF bounds;

    private Bitmap bitmap;
    private Bitmap bkgBitmap;

    private int lastDayOfYear;

    private TextPaint paint;
    private TextPaint ambientPaint;
    private Paint erasePaint;
    private Paint bkgPaint;

    private boolean timeZoneRegistered = false;
    private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            calendar.setTimeZone(TimeZone.getDefault());
            drawDate();
        }
    };

    public DatePanel(int screenWidth, int screenHeight, WatchfaceConfig watchfaceConfig) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
        this.watchfaceConfig = watchfaceConfig;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {
        lastDayOfYear = -1; // force redraw

        calendar = Calendar.getInstance();

        paint = UiUtils.createTextPaint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextScaleX(0.9f);

        ambientPaint = UiUtils.createAmbientTextPaint();
        ambientPaint.setTextAlign(paint.getTextAlign());
        ambientPaint.setTextScaleX(paint.getTextScaleX());

        erasePaint = UiUtils.createErasePaint();
        bkgPaint = UiUtils.createPaint();

        sizeFactors = new RectF(
                context.getResources().getDimension(R.dimen.analog_layout_date_panel_left) / refScreenWidth,
                context.getResources().getDimension(R.dimen.analog_layout_date_panel_top) / refScreenHeight,
                context.getResources().getDimension(R.dimen.analog_layout_date_panel_right) / refScreenWidth,
                context.getResources().getDimension(R.dimen.analog_layout_date_panel_bottom) / refScreenHeight
        );
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        bounds = new RectF(
                width * sizeFactors.left + PADDING,
                height * sizeFactors.top + PADDING + TOP_OFFSET,
                width * sizeFactors.right - PADDING,
                height * sizeFactors.bottom - PADDING);

        bitmap = Bitmap.createBitmap((int) bounds.width(), (int) bounds.height(), Bitmap.Config.ARGB_8888);
        bkgBitmap = Bitmap.createBitmap((int) bounds.width(), (int) bounds.height(), Bitmap.Config.ARGB_8888);

        paint.setTextSize(bounds.height() / 2.3f);
        ambientPaint.setTextSize(paint.getTextSize());

        drawDate();
        drawBackgroundAndBorder();
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {
        calendar.setTimeInMillis(System.currentTimeMillis());

        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        if (dayOfYear != lastDayOfYear) {
            lastDayOfYear = dayOfYear;
            drawDate();
        }

        if (isAmbientMode) {
            canvas.drawBitmap(bitmap, bounds.left, bounds.top, ambientPaint);
        } else {
            canvas.drawBitmap(bkgBitmap, bounds.left, bounds.top, paint);
            canvas.drawBitmap(bitmap, bounds.left, bounds.top, paint);
        }
    }

    private void drawDate() {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "drawDate: ");
        }
        if (bitmap == null || dayOfMonthFormat == null) {
            return; // not ready yet
        }

        calendar.setTimeInMillis(System.currentTimeMillis());
        final Date date = calendar.getTime();

        Canvas canvas = new Canvas(bitmap);
        // erase bitmap
        canvas.drawRect(new Rect(0, 0, bitmap.getWidth(), bitmap.getWidth()), erasePaint);

        String line1;
        if (showMonth) { // Month
            paint.setColor(monthColor);
            line1 = formatDayMonth(monthFormat, monthFullNameFormat, date);
        } else { // Day of week
            paint.setColor(dayOfWeekColor);
            line1 = formatDayMonth(dayOfWeekFormat, dayOfWeekFullNameFormat, date);
        }

        final float centerX = bounds.width() / 2f;

        // upper text
        canvas.drawText(line1, centerX, bounds.height() / 2f - 2, paint);

        // Day of Month
        paint.setColor(dayOfMonthColor);
        canvas.drawText(dayOfMonthFormat.format(date), centerX, bounds.height() - 7, paint);
    }

    private String formatDayMonth(DateFormat format, DateFormat fullNameFormat, Date date) {
        String formatted = format.format(date).toUpperCase();
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length()-1);
        }
        if (formatted.length() == 3) {
            return formatted;
        } else if (formatted.length() > 3) {
            return formatted.substring(0, 3);
        } else { // < 3
            formatted = fullNameFormat.format(date).toUpperCase();
            return formatted.length() <= 3 ? formatted : formatted.substring(0, 3);
        }
    }

    private void drawBackgroundAndBorder() {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "drawBackgroundAndBorder: ");
        }
        if (bkgBitmap == null || dayOfMonthFormat == null) {
            return; // not ready yet
        }
        bkgBitmap.eraseColor(Color.TRANSPARENT);

        Canvas canvas = new Canvas(bkgBitmap);
        // erase bitmap
        canvas.drawRect(new Rect(0, 0, bkgBitmap.getWidth(), bkgBitmap.getWidth()), erasePaint);

        // draw background
        if (backgroundColor != Color.TRANSPARENT) {
            bkgPaint.setColor(backgroundColor);
            bkgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            if (BorderUtils.isBorderRounded(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, bkgPaint);
            } else if (BorderUtils.isBorderRing(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_RING_RADIUS, BORDER_RING_RADIUS, bkgPaint);
            } else {
                canvas.drawRect(0, 0, bounds.width() - 1, bounds.height() - 1, bkgPaint);
            }
        }

        // draw border
        if (borderType != BorderType.NONE && borderType != null) {
            bkgPaint.setColor(borderColor);
            bkgPaint.setStyle(Paint.Style.STROKE);
            bkgPaint.setStrokeWidth(BORDER_WIDTH);
            if (BorderUtils.getBorderDrawableStyle(borderType) == ComplicationDrawable.BORDER_STYLE_DASHED) {
                if (BorderUtils.isBorderDotted(borderType)) {
                    bkgPaint.setPathEffect(new DashPathEffect(new float[]{BORDER_DOT_LEN, BORDER_GAP_LEN}, 0f));
                } else {
                    bkgPaint.setPathEffect(new DashPathEffect(new float[]{BORDER_DASH_LEN, BORDER_GAP_LEN}, 0f));
                }
            }
            if (BorderUtils.isBorderRounded(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, bkgPaint);
            } else if (BorderUtils.isBorderRing(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_RING_RADIUS, BORDER_RING_RADIUS, bkgPaint);
            } else {
                canvas.drawRect(0, 0, bounds.width() - 1, bounds.height() - 1, bkgPaint);
            }
        }
    }

    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {
        /* Update time zone in case it changed while we weren"t visible. */

        configureFormats();

        showMonth = sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_SHOW_MONTH, context.getResources().getBoolean(R.bool.def_date_show_month));

        // colors
        backgroundColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BKG_COLOR, context.getColor(R.color.def_date_background_color));
        monthColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_MONTH_COLOR, context.getColor(R.color.def_date_month_color));
        dayOfMonthColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_DAY_OF_MONTH_COLOR, context.getColor(R.color.def_date_day_of_month_color));
        dayOfWeekColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_DAY_OF_WEEK_COLOR, context.getColor(R.color.def_date_day_of_week_color));

        // border
        borderColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_COLOR, context.getColor(R.color.def_date_border_color));
        borderType = BorderType.getByNameOrDefault(sharedPrefs.getString(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_TYPE, context.getString(R.string.def_date_border_type)));

        drawDate();
        drawBackgroundAndBorder();
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {
        configureFormats();

        drawDate();
        drawBackgroundAndBorder();
    }

    private void configureFormats() {
        Locale locale = Locale.getDefault(); // user language and country properties shows incorrect values

        calendar.setTimeZone(TimeZone.getDefault());
        dayOfWeekFormat = new SimpleDateFormat("EEE", locale);
        monthFormat = new SimpleDateFormat("MMM", locale);
        dayOfMonthFormat = new SimpleDateFormat("d", locale);

        dayOfWeekFullNameFormat = new SimpleDateFormat("EEEE", locale);
        monthFullNameFormat = new SimpleDateFormat("MMM", locale);
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
