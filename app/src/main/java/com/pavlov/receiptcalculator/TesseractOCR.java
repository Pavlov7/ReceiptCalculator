package com.pavlov.receiptcalculator;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

public class TesseractOCR {

    private final TessBaseAPI mTess;

    public TesseractOCR(Context context, String language) {
        mTess = new TessBaseAPI();
        boolean fileExistFlag = false;

        AssetManager assetManager = context.getAssets();

        String dstPathDir = "/tesseract/tessdata/";

        String srcFile = "eng.traineddata";
        InputStream inFile = null;

        dstPathDir = context.getFilesDir() + dstPathDir;
        String dstInitPathDir = context.getFilesDir() + "/tesseract";
        String dstPathFile = dstPathDir + srcFile;
        FileOutputStream outFile = null;


        try {
            inFile = assetManager.open(srcFile);

            File f = new File(dstPathDir);

            if (!f.exists()) {
                if (!f.mkdirs()) {
                    Toast.makeText(context, srcFile + " not found.", Toast.LENGTH_SHORT).show();
                }
                outFile = new FileOutputStream(new File(dstPathFile));
            } else {
                fileExistFlag = true;
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());

        } finally {

            if (fileExistFlag) {
                try {
                    if (inFile != null) inFile.close();
                    init(dstInitPathDir, language);
                    return;

                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }

            if (inFile != null && outFile != null) {
                try {
                    //copy file
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inFile.read(buf)) != -1) {
                        outFile.write(buf, 0, len);
                    }
                    inFile.close();
                    outFile.close();
                    init(dstInitPathDir, language);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            } else {
                Toast.makeText(context, srcFile + " can't be read.", Toast.LENGTH_SHORT).show();
            }


        }
    }

    private void init(String dstInitPathDir, String language) {
        mTess.init(dstInitPathDir, language);
//        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
//        mTess.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!?@#$%&*()<>_-+=/:;'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
//        mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, ".,0123456789");
//        mTess.setVariable("classify_bln_numeric_mode", "1");
    }

    public String getOCRResult(Bitmap bitmap) {
        mTess.setImage(bitmap);
        return mTess.getUTF8Text();
    }


    public void onDestroy() {
        if (mTess != null) mTess.end();
    }
}
