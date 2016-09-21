package com.cameratest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    public static Bitmap decodeSampledBitmapFromFile(String filePath, int width, int height) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BufferedInputStream stream = null;
        if (filePath.contains("http://") || filePath.contains("https://")) {
            OkHttpClient build = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(filePath).build();
            try {
                Response response = build.newCall(request).execute();
                InputStream is = response.body().byteStream();
                stream = new BufferedInputStream(is);
                BitmapFactory.decodeStream(stream, null, options);
                stream.reset();
            } catch (IOException e) {
                e.printStackTrace();
                BitmapFactory.decodeFile(filePath, options);
            }
        } else {
            BitmapFactory.decodeFile(filePath, options);
        }


        // Calculate inSampleSize

        options.inSampleSize = calculateInSampleSize(options, width, height);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        if (stream != null) {
            return BitmapFactory.decodeStream(stream, null, options);
        }
        try {
            return rotateImageIfRequired(BitmapFactory.decodeFile(filePath, options), filePath);
        } catch (IOException e) {
            return BitmapFactory.decodeFile(filePath, options);
        }


    }


    private static Bitmap rotateImageIfRequired(Bitmap img, String filePath) throws IOException {

        ExifInterface ei = new ExifInterface(filePath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int newWidth, int newHeight) {


        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        int inSampleSize = 1;
        if (height > newHeight || width > newWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > newHeight && (halfWidth / inSampleSize) > newWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
