package hywt.maplemandel.droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class MandelbrotView extends View {

    private Paint paint = new Paint();
    private float scaleFactor = 1.0f;
    private float translateX = 0f;
    private float translateY = 0f;

    private double centerX = 0f;  // New centerX variable
    private double centerY = 0f;  // New centerY variable
    private double magnification = 1f;

    private ScaleGestureDetector scaleDetector;
    private Bitmap bitmap;
    private int[] colors;
    private int maxIterations;
    private GestureDetector panDetector;

    private int width;
    private int height;
    private double baseStep;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> drawer;

    private int touchAmount;

    public MandelbrotView(Context context) {
        super(context);
        setDrawingCacheEnabled(true);
        setBackgroundColor(Color.BLACK);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        panDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                translateX -= distanceX;
                translateY -= distanceY;
                invalidate();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
        generateMandelbrot();
    }

    private void generateMandelbrot() {
        width = getWidth();
        height = getHeight();
        int min = Math.min(width, height);
        baseStep = 4d / min;
        Log.i("d", String.valueOf(baseStep));

        if (width == 0 || height == 0) return;

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        maxIterations = 256;
        colors = new int[maxIterations + 1];

        double frequency = 0.2;
        for (int i = 0; i < maxIterations; i++) {
            double red = Math.sin(frequency * i) * 127 + 128;
            double green = Math.sin(frequency * 1.1 * i) * 127 + 128;
            double blue = Math.sin(frequency * 1.2 * i) * 127 + 128;
            colors[i] = Color.rgb((int) red, (int) green, (int) blue);
        }
        colors[maxIterations] = Color.BLACK;

        // Run Mandelbrot calculation on a background thread
        updateMandelbrotAsync();
    }

    private void updateMandelbrotAsync() {
        Canvas canvas = new Canvas(bitmap);
        buildDrawingCache();
        canvas.drawBitmap(getDrawingCache(), 0, 0, paint);

        translateX = 0;
        translateY = 0;
        scaleFactor = 1;
        if (drawer != null) drawer.cancel(true);
        drawer = executorService.submit(() -> {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    double cx = getCx(x);
                    double cy = getCy(y);
                    double zx = cx;
                    double zy = cy;
                    int iteration = 0;

                    while (zx * zx + zy * zy < 4.0 && iteration < maxIterations) {
                        double temp = zx * zx - zy * zy + cx;
                        zy = 2.0f * zx * zy + cy;
                        zx = temp;
                        iteration++;
                    }

                    int color = colors[iteration];
                    bitmap.setPixel(x, y, color);
                }
                invalidate();
            }

        });
    }

    private double getCy(double y) {
        return (y - height / 2) * baseStep / magnification + centerY;
    }

    private double getCx(double x) {
        return (x - width / 2) * baseStep / magnification + centerX;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) generateMandelbrot();
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) touchAmount++;
        else if (event.getAction() == MotionEvent.ACTION_UP) touchAmount--;
        if (touchAmount == 0) {
            // Calculate the center based on the current scale and translation
            float screenCenterX = width / 2f;  // Tap position X
            float screenCenterY = height / 2f;  // Tap position Y

            // Transform screen coordinates to the original unscaled/untranslated coordinate system
            centerX = getCx((screenCenterX - translateX) / scaleFactor);
            centerY = getCy((screenCenterY - translateY) / scaleFactor);

            // Apply magnification
            magnification *= scaleFactor;

            updateMandelbrotAsync();
//            invalidate();

            Log.e("center", "centerX: " + centerX + ", centerY: " + centerY);
        }
        panDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            float scaleFactorChange = detector.getScaleFactor();
            scaleFactor *= scaleFactorChange;


            // Translate the center point based on the scale factor change
            translateX = focusX - (focusX - translateX) * scaleFactorChange;
            translateY = focusY - (focusY - translateY) * scaleFactorChange;

            invalidate();
            return true;
        }
    }
}
