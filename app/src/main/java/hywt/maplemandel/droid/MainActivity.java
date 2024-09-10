package hywt.maplemandel.droid;


import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import hywt.maplemandel.core.Mandelbrot;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Mandelbrot mandelbrot = mandelbrotView.getMandelbrot();
        int id = item.getItemId();
        if (id == R.id.increase_iter) {
            mandelbrot.cancel();
            mandelbrot.setMaxIter(mandelbrot.getMaxIter() * 4);
            mandelbrotView.updateMandelbrotAsync();
        }
        return true;
    }
}