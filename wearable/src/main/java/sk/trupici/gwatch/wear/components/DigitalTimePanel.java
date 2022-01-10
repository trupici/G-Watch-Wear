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

import android.annotation.SuppressLint;
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
import android.graphics.PointF;
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
import java.util.TimeZone;

import sk.trupici.gwatch.wear.BuildConfig;
import sk.trupici.gwatch.wear.R;
import sk.trupici.gwatch.wear.config.BorderType;
import sk.trupici.gwatch.wear.config.WatchfaceConfig;
import sk.trupici.gwatch.wear.util.BorderUtils;

import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_DASH_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_DOT_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_GAP_LEN;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_RING_RADIUS;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_ROUND_RECT_RADIUS;
import static sk.trupici.gwatch.wear.util.BorderUtils.BORDER_WIDTH;

public class DigitalTimePanel implements ComponentPanel {
    public static final String LOG_TAG = DigitalTimePanel.class.getSimpleName();

    public static final int CONFIG_ID = 16;

    public static final String PREF_IS_24_HR_TIME = "time_is_24_hr";
    public static final String PREF_SHOW_SECS = "time_show_seconds";

    public static final String PREF_BKG_COLOR = "time_background_color";
    public static final String PREF_TEXT_COLOR = "time_text_color";

    public static final String PREF_BORDER_COLOR = "time_border_color";
    public static final String PREF_BORDER_TYPE = "time_border_type";

    final private int refScreenWidth;
    final private int refScreenHeight;

    final private WatchfaceConfig watchfaceConfig;

    private int backgroundColor;
    private int textColor;
    private int ambientTextColor;
    private boolean is24hrTime;
    private boolean showSeconds;

    private int borderColor;
    private BorderType borderType;

    private Calendar calendar;
    private DateFormat timeFormat;
    private DateFormat ambientTimeFormat;

    private RectF sizeFactors;
    private RectF bounds;
    private PointF position;

    private Bitmap bkgBitmap;

    private TextPaint paint;

    private boolean timeZoneRegistered = false;
    private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            calendar.setTimeZone(TimeZone.getDefault());
        }
    };

    public static boolean getIs24HourFormatDefaultValue(Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    public DigitalTimePanel(int screenWidth, int screenHeight, WatchfaceConfig watchfaceConfig) {
        this.refScreenWidth = screenWidth;
        this.refScreenHeight = screenHeight;
        this.watchfaceConfig = watchfaceConfig;
    }

    @Override
    public void onCreate(Context context, SharedPreferences sharedPrefs) {

        calendar = Calendar.getInstance();

        paint = new TextPaint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);

        sizeFactors = new RectF(
                context.getResources().getDimension(R.dimen.digital_layout_time_panel_left) / refScreenWidth,
                context.getResources().getDimension(R.dimen.digital_layout_time_panel_top) / refScreenHeight,
                context.getResources().getDimension(R.dimen.digital_layout_time_panel_right) / refScreenWidth,
                context.getResources().getDimension(R.dimen.digital_layout_time_panel_bottom) / refScreenHeight
        );

        ambientTextColor = Color.LTGRAY;
    }

    @Override
    public void onSizeChanged(Context context, int width, int height) {
        bounds = new RectF(
                width * sizeFactors.left,
                height * sizeFactors.top,
                width * sizeFactors.right,
                height * sizeFactors.bottom);

        bkgBitmap = Bitmap.createBitmap((int) bounds.width(), (int) bounds.height(), Bitmap.Config.ARGB_8888);

        paint.setTextSize(bounds.height() * 0.8f);

        Rect textBounds = new Rect();
        paint.getTextBounds("12:00", 0, 5, textBounds);

        position = new PointF(
                bounds.width() / 2, // center
                (bounds.height() - textBounds.height()) / 2
        );

        drawBackgroundAndBorder();
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onConfigChanged(Context context, SharedPreferences sharedPrefs) {
        /* Update time zone in case it changed while we weren"t visible. */
        calendar.setTimeZone(TimeZone.getDefault());

        is24hrTime = sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_IS_24_HR_TIME, getIs24HourFormatDefaultValue(context));
        showSeconds = sharedPrefs.getBoolean(watchfaceConfig.getPrefsPrefix() + PREF_SHOW_SECS, context.getResources().getBoolean(R.bool.def_time_show_seconds));

        if (is24hrTime) {
            if (showSeconds) {
                timeFormat = new SimpleDateFormat("HH:mm:ss");
                ambientTimeFormat = new SimpleDateFormat("HH:mm");
            } else {
                timeFormat = new SimpleDateFormat("HH:mm");
                ambientTimeFormat = timeFormat;
            }
        } else {
            if (showSeconds) {
                timeFormat = new SimpleDateFormat("h:mm:ss");
                ambientTimeFormat = new SimpleDateFormat("h:mm");
            } else {
                timeFormat = new SimpleDateFormat("h:mm");
                ambientTimeFormat = timeFormat;
            }
        }

        // colors
        backgroundColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BKG_COLOR, context.getColor(R.color.def_time_background_color));
        textColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_TEXT_COLOR, context.getColor(R.color.def_time_text_color));

        // border
        borderColor = sharedPrefs.getInt(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_COLOR, context.getColor(R.color.def_time_border_color));
        borderType = BorderType.getByNameOrDefault(sharedPrefs.getString(watchfaceConfig.getPrefsPrefix() + PREF_BORDER_TYPE, context.getString(R.string.def_time_border_type)));
    }

    @Override
    public void onPropertiesChanged(Context context, Bundle properties) {
        calendar.setTimeZone(TimeZone.getDefault());
    }

    @Override
    public void onDraw(Canvas canvas, boolean isAmbientMode) {

        if (timeFormat == null) {
            return; // not ready yet
        }

        calendar.setTimeInMillis(System.currentTimeMillis());
        final Date date = calendar.getTime();

        String text;
        if (isAmbientMode) {
            text = ambientTimeFormat.format(date);
            paint.setColor(ambientTextColor);
        } else {
            canvas.drawBitmap(bkgBitmap, bounds.left, bounds.top, paint);
            text = timeFormat.format(date);
            paint.setColor(textColor);
        }

        canvas.drawText(text, bounds.left + position.x, bounds.bottom - position.y, paint);
    }

    private void drawBackgroundAndBorder() {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "drawBackgroundAndBorder: ");
        }
        if (bkgBitmap == null || timeFormat == null) {
            return; // not ready yet
        }
        bkgBitmap.eraseColor(Color.TRANSPARENT);

        Canvas canvas = new Canvas(bkgBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // draw background
        if (backgroundColor != Color.TRANSPARENT) {
            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            if (BorderUtils.isBorderRounded(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
            } else if (BorderUtils.isBorderRing(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_RING_RADIUS, BORDER_RING_RADIUS, paint);
            } else {
                canvas.drawRect(0, 0, bounds.width() - 1, bounds.height() - 1, paint);
            }
        }

        // draw border
        if (borderType != BorderType.NONE) {
            paint.setColor(borderColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(BORDER_WIDTH);
            if (BorderUtils.getBorderDrawableStyle(borderType) == ComplicationDrawable.BORDER_STYLE_DASHED) {
                if (BorderUtils.isBorderDotted(borderType)) {
                    paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DOT_LEN, BORDER_GAP_LEN}, 0f));
                } else {
                    paint.setPathEffect(new DashPathEffect(new float[]{BORDER_DASH_LEN, BORDER_GAP_LEN}, 0f));
                }
            }
            if (BorderUtils.isBorderRounded(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_ROUND_RECT_RADIUS, BORDER_ROUND_RECT_RADIUS, paint);
            } else if (BorderUtils.isBorderRing(borderType)) {
                canvas.drawRoundRect(0, 0, bounds.width() - 1, bounds.height() - 1,
                        BORDER_RING_RADIUS, BORDER_RING_RADIUS, paint);
            } else {
                canvas.drawRect(0, 0, bounds.width() - 1, bounds.height() - 1, paint);
            }
        }
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
