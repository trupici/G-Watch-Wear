package sk.trupici.gwatch.wear.util;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.text.TextPaint;

public class UiUtils {

    public static Paint createPaint() {
        Paint paint = new Paint();
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
}
