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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.pavlov.receiptcalculator.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private TesseractOCR mTessOCR;
    private Context context;
    protected String mCurrentPhotoPath;
    private Uri photoURI1;
    private Uri oldPhotoURI;

    private static final String errorFileCreate = "Error file create!";
    private static final String errorConvert = "Error convert!";
    private static final int REQUEST_IMAGE_CAPTURE_1 = 1;

    int PERMISSION_ALL = 1;
    boolean flagPermissions = false;

    String[] PERMISSIONS = {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE_1) {
            if (resultCode == RESULT_OK) {
                Bitmap bmp = null;
                try {
                    InputStream is = context.getContentResolver().openInputStream(photoURI1);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    bmp = BitmapFactory.decodeStream(is, null, options);

                } catch (Exception ex) {
                    Log.i(getClass().getSimpleName(), ex.getMessage());
                    Toast.makeText(context, errorConvert, Toast.LENGTH_SHORT).show();
                }

                binding.ocrImage.setImageBitmap(bmp);
                doOCR(bmp);

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


//    @RequiresApi(api = Build.VERSION_CODES.M)
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
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing",
                    "Doing OCR...", true);
        } else {
            mProgressDialog.show();
        }

        new Thread(() -> {
            final String srcText = mTessOCR.getOCRResult(bitmap);
            runOnUiThread(() -> {

                if (srcText != null && !srcText.equals("")) {
                    binding.ocrText.setText(srcText.replaceAll("[^0-9.,]+", " "));
                }

                mProgressDialog.dismiss();
            });
        }).start();
    }
}