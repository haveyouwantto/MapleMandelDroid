package hywt.maplemandel.droid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import hywt.maplemandel.core.DrawCall;
import hywt.maplemandel.core.Mandelbrot;
import hywt.maplemandel.core.numtype.DeepComplex;

class MandelbrotView extends View {

    private Paint paint = new Paint();
    private float scaleFactor = 1.0f;
    private float translateX = 0f;
    private float translateY = 0f;

    private ScaleGestureDetector scaleDetector;
    private Bitmap bitmap;
    private Bitmap oldBitmap;
    private GestureDetector panDetector;

    private int width;
    private int height;
    private double baseStep;

    private Handler handler = new Handler(Looper.getMainLooper());
//    private ExecutorService executorService = Executors.newSingleThreadExecutor();
//    private Future<?> drawer;

    private Mandelbrot mandelbrot;
    private DrawCall drawCall;

    private int touchAmount;

    public MandelbrotView(Context context) {
        super(context);
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
        oldBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        mandelbrot = new Mandelbrot(width, height);
        mandelbrot.setMaxIter(1024);
        Canvas canvas = new Canvas(bitmap);
        drawCall = new DrawCall(width, height) {
            private Paint paint = new Paint();

            @Override
            public synchronized void draw(int x, int y, int w, int h, hywt.maplemandel.core.Color color) {
                paint.setARGB(255, color.r, color.g, color.b);
                canvas.drawRect(x, y, x + w, y + h, paint);
                invalidate(x, y, x + w, y + h);
            }

            @Override
            public synchronized void draw(int x, int y, hywt.maplemandel.core.Color color) {
                bitmap.setPixel(x, y, Color.rgb(color.r, color.g, color.b));
                invalidate(x, y, x + 1, y + 1);
            }
        };

        // Run Mandelbrot calculation on a background thread
        updateMandelbrotAsync();
    }

    public void updateMandelbrotAsync() {
        mandelbrot.startDraw(drawCall, null);
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
            if (translateX == 0f && translateY == 0f && scaleFactor == 1f) return true;
            // Calculate the center based on the current scale and translation
            float screenCenterX = width / 2f;  // Tap position X
            float screenCenterY = height / 2f;  // Tap position Y

            DeepComplex center = mandelbrot.getCenter();
            center = center.add(mandelbrot.getDeepDelta(
                    (int) ((screenCenterX - translateX) / scaleFactor),
                    (int) ((screenCenterY - translateY) / scaleFactor)
            ).toDeepComplex());

            mandelbrot.gotoLocation(center, mandelbrot.getScale().div(scaleFactor));

//            // Transform screen coordinates to the original unscaled/untranslated coordinate system
//            centerX = getCx((screenCenterX - translateX) / scaleFactor);
//            centerY = getCy((screenCenterY - translateY) / scaleFactor);
//
//            // Apply magnification
//            magnification *= scaleFactor;

            synchronized (this) {
                Canvas canvas = new Canvas(oldBitmap);
                draw(canvas);
                Canvas canvas1 = new Canvas(bitmap);
                canvas1.drawBitmap(oldBitmap, 0, 0, paint);

                translateX = 0;
                translateY = 0;
                scaleFactor = 1;
            }

            updateMandelbrotAsync();
//            invalidate();

//            Log.e("center", "centerX: " + centerX + ", centerY: " + centerY);
        } else {
            mandelbrot.cancel();
//            if (drawer != null) drawer.cancel(true);
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

            if (!(scaleFactor < 1 && mandelbrot.getScale().div(scaleFactor).doubleValue() > 8))
                scaleFactor *= scaleFactorChange;


            // Translate the center point based on the scale factor change
            translateX = focusX - (focusX - translateX) * scaleFactorChange;
            translateY = focusY - (focusY - translateY) * scaleFactorChange;

            invalidate();
            return true;
        }
    }

    public Mandelbrot getMandelbrot() {
        return mandelbrot;
    }
}
