package com.pavlov.receiptcalculator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.pavlov.receiptcalculator.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private TesseractOCR mTessOCR;
    private Context context;
    protected String mCurrentPhotoPath;
    private Uri photoURI1;
    private Uri oldPhotoURI;
    private Map<Rect, String> rect;
    private Canvas c;
    private Bitmap imgBitmap;
    private Paint paint;

    private static final String errorFileCreate = "Error file create!";
    private static final String errorConvert = "Error convert!";
    private static final int REQUEST_IMAGE_CAPTURE_1 = 1;

    private final int PERMISSION_ALL = 1;
    private boolean flagPermissions = false;

    private String[] PERMISSIONS = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        if (Build.VERSION.SDK_INT <= 18) {
            PERMISSIONS = Arrays.copyOf(PERMISSIONS, 3);
            PERMISSIONS[2] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }

        // check permissions
        if (!checkPermissions()) {
            return;
        }

        String language = "eng";
        mTessOCR = new TesseractOCR(this, language);

        paint = new Paint();
        paint.setColor(Color.parseColor("#CD5C5C"));
        paint.setAlpha(128);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE_1) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream is = context.getContentResolver().openInputStream(photoURI1);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    imgBitmap = BitmapFactory.decodeStream(is, null, options);

                } catch (Exception ex) {
                    Log.i(getClass().getSimpleName(), ex.getMessage());
                    Toast.makeText(context, errorConvert, Toast.LENGTH_SHORT).show();
                }

                doOCR(imgBitmap);

                // TODO why is this needed???
//                OutputStream os;
//                try {
//                    os = new FileOutputStream(photoURI1.getPath());
//                    if (bmp != null) {
//                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
//                    }
//                    os.flush();
//                    os.close();
//                } catch (Exception ex) {
//                    Log.e(getClass().getSimpleName(), ex.getMessage());
//                    Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
//                }

            } else {
                {
                    photoURI1 = oldPhotoURI;
                    binding.ocrImage.setImageURI(photoURI1);
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (rect == null) {
            return super.dispatchTouchEvent(event);
        }

        int touchX = (int)event.getX();
        int touchY = (int)event.getY();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                System.out.println("Touching down!");
                for(Map.Entry<Rect, String> rect : rect.entrySet()){
                    if(rect.getKey().contains(touchX,touchY)){
                        System.out.println("Touched Rectangle, start activity.");
                        // TODO open modal to sum number
//                        Intent i = new Intent(<your activity info>);
//                        startActivity(i);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                System.out.println("Touching up!");
                break;
            case MotionEvent.ACTION_MOVE:
                System.out.println("Sliding your finger around on the screen.");
                break;
        }

        return super.dispatchTouchEvent(event);
    }

    public void onClickScanButton(View view) {
        // check permissions
        if (!checkPermissions()) {
            return;
        }

        //prepare intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
                Log.i("File error", ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                oldPhotoURI = photoURI1;
                photoURI1 = FileProvider.getUriForFile(this,
                        "com.pavlov.receiptcalculator.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI1);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_1);
            }
        }
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public boolean checkPermissions() {
        if (flagPermissions) {
            return true;
        }

        if (!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            flagPermissions = false;
            return false;
        }

        flagPermissions = true;
        return true;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void doOCR(final Bitmap bitmap) {
        Bitmap cBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        binding.ocrImage.setImageBitmap(cBitmap);
        binding.ocrImage.postInvalidate();

        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing",
                    "Doing OCR...", true);
        } else {
            mProgressDialog.show();
        }

        new Thread(() -> {
            List<Pair<String, int[]>> srcText = mTessOCR.getOCRResult(bitmap);
            runOnUiThread(() -> {
                rect = new HashMap<>();
                c = new Canvas(cBitmap);

                // image and screen are not the same size
                // touch events have the exact screen coords
                // also, the image view's top left coords are not 0,0 as it is positioned lower
                // on the screen (below the scan button)
                // find the scale coefficient
                // factor in image view start position

                Rect imgCoords = new Rect();
                binding.ocrImage.getGlobalVisibleRect(imgCoords);

                int imgXOffset = imgCoords.left;
                int imgYOffset = imgCoords.top;
                double widthCoef = Math.max(imgCoords.right - imgCoords.left, cBitmap.getWidth())
                        / (double)Math.min(imgCoords.right - imgCoords.left, cBitmap.getWidth());
                double heightCoef = Math.max(imgCoords.bottom - imgCoords.top, cBitmap.getHeight())
                        / (double)Math.min(imgCoords.bottom - imgCoords.top, cBitmap.getHeight());

                boolean screenBigger = imgCoords.bottom > cBitmap.getHeight();

                for (Pair<String, int[]> p : srcText) {

                    int x = p.second[0];
                    int y = p.second[1];
                    int w = p.second[2];
                    int h = p.second[3];

                    Rect r = new Rect(x, y, w, h);
                    c.drawRect(r, paint);

                    x = rectangleCoordsTranform(p.second[0], widthCoef, screenBigger, imgXOffset);
                    y = rectangleCoordsTranform(p.second[1], heightCoef, screenBigger, imgYOffset);
                    w = rectangleCoordsTranform(p.second[2], widthCoef, screenBigger, imgXOffset);
                    h = rectangleCoordsTranform(p.second[3], heightCoef, screenBigger, imgYOffset);
                    rect.put(new Rect(x, y, w, h), p.first);
                }

                mProgressDialog.dismiss();
            });
        }).start();
    }

    private int rectangleCoordsTranform(int coord, double coef, boolean screenBigger, int offset) {
        return (int)(screenBigger ? coord * coef : coord / coef) + offset;
    }
}