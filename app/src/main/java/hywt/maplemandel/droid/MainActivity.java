package hywt.maplemandel.droid;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import hywt.maplemandel.core.Mandelbrot;
import hywt.maplemandel.core.Parameter;
import hywt.maplemandel.core.numtype.DeepComplex;
import hywt.maplemandel.core.numtype.FloatExp;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_MPR = 54;
    private MandelbrotView mandelbrotView;
    private Mandelbrot mandelbrot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mandelbrotView = new MandelbrotView(this);
        mandelbrot = mandelbrotView.getMandelbrot();
        System.out.println(mandelbrot);
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
        int id = item.getItemId();
        if (mandelbrot == null) {
            mandelbrot = mandelbrotView.getMandelbrot();
        }
        if (id == R.id.action_increase_iter) {
            mandelbrot.cancel();
            mandelbrot.setMaxIter(mandelbrot.getMaxIter() * 4);
            mandelbrotView.updateMandelbrotAsync();
        } else if (id == R.id.action_load_param) {
            openFileSelector();
        } else if (id == R.id.action_reset) {
            mandelbrot.cancel();
            mandelbrot.gotoLocation(new DeepComplex(0,0), new FloatExp(4));
            mandelbrot.setMaxIter(256);
            mandelbrotView.updateMandelbrotAsync();
        }
        return true;
    }


    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        // Only allow the selection of .mpr files
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(Intent.createChooser(intent, "Select MPR File"), REQUEST_CODE_SELECT_MPR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_MPR && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri selectedFile = data.getData();
                if (selectedFile != null) {
                    Toast.makeText(this, "Selected file: " + selectedFile, Toast.LENGTH_LONG).show();
                    try {
                        InputStream is = new GZIPInputStream(getContentResolver().openInputStream(selectedFile));
                        if (mandelbrot == null) {
                            mandelbrot = mandelbrotView.getMandelbrot();
                        }
                        mandelbrot.cancel();
                        mandelbrot.loadParameter(Parameter.load(is));
                        mandelbrotView.updateMandelbrotAsync();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}