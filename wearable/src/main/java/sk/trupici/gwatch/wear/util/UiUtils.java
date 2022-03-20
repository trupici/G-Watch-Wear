package sk.trupici.gwatch.wear.util;

import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.text.TextPaint;
import android.view.View;

public class UiUtils {

    public static Paint createPaint() {
        return setPaint(new Paint());
    }

    public static TextPaint createTextPaint() {
        return setPaint(new TextPaint());
    }

    private static <T extends Paint> T setPaint(T paint) {
        paint.setAntiAlias(true);
        return paint;
    }

    public static Paint createAmbientPaint() {
        return setAmbientPaint(new Paint());
    }

    public static TextPaint createAmbientTextPaint() {
        return setAmbientPaint(new TextPaint());
    }

    private static <T extends Paint> T setAmbientPaint(T paint) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        return paint;
    }

    public static Paint createErasePaint() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setColor(Color.TRANSPARENT);
        return paint;
    }

    public static Rect getViewBounds(View view) {
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], view.getWidth(), view.getHeight());
    }
}
