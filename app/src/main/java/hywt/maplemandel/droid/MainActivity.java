package hywt.maplemandel.droid;


import android.os.Bundle;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MandelbrotView mandelbrotView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mandelbrotView = new MandelbrotView(this);
        setContentView(mandelbrotView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mandelbrotView.onTouchEvent(event) || super.onTouchEvent(event);
    }

}