package com.cameratest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class BaseProfilePicActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 11;
    private static final int REQUEST_IMAGE_GALLERY_PIC = 12;
    public static final String KEY_BITMAP = "KEY_BITMAP";
    private static final int REQUEST_IMAGE_CROP = 13;
    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_MULTIPLE_PERMISSION = 123;

    private BottomSheetDialog mBottomSheetDialog;
    private String mCurrentPhotoPath;
    private AlbumStorageDirFactory mAlbumStorageDirFactory;
    private static final String TAG = "";


    public abstract void updateImageView(Bitmap bitmap);


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
    }


    public void showBottomSheetDialog(boolean isAddRemove) {
        mBottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.sheet, null);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        recyclerView.setAdapter(new ItemAdapter(createItems(isAddRemove)));
        mBottomSheetDialog.setContentView(view);
        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBottomSheetDialog = null;
            }
        });
    }


    public class Item {

        private int mDrawableRes;

        private String mTitle;

        public Item(int drawable, String title) {
            mDrawableRes = drawable;
            mTitle = title;

        }

        public int getDrawableResource() {
            return mDrawableRes;
        }

        public String getTitle() {
            return mTitle;
        }

    }


    class ItemAdapter extends RecyclerView.Adapter<ViewHolder> implements OnItemClickListener {

        private List<Item> mItems;

        public ItemAdapter(List<Item> items) {
            mItems = items;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.picker_item, parent, false);
            return new ViewHolder(inflate, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(mItems.get(position).getTitle());
            holder.imageView.setImageResource(mItems.get(position).getDrawableResource());
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }


        @Override
        public void onItemClick(int position) {
            BaseProfilePicActivity.this.onItemClick(position);
        }
    }

    private Intent dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f;
        try {
            f = mAlbumStorageDirFactory.setUpPhotoFile();
            mCurrentPhotoPath = f.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
            return takePictureIntent;
        } catch (IOException e) {
            e.printStackTrace();
            mCurrentPhotoPath = null;
        }

        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentPhotoPath != null) {
            outState.putString("cameraImageUri", mCurrentPhotoPath);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("cameraImageUri")) {
            mCurrentPhotoPath = savedInstanceState.getString("cameraImageUri");
        }
    }

    private final String[] PERMISSION_STORAGE =
            new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
                    , Manifest.permission.CAMERA
                    , Manifest.permission.WRITE_EXTERNAL_STORAGE

            };
    List<String> permissionsNeeded = new ArrayList<>();
    boolean isFirstTime = true;

    private void onItemClick(int position) {
        mBottomSheetDialog.dismiss();
        Intent pictureIntent = null;
        int resultCode = 0;
        switch (position) {
            case 0:
                if (Build.VERSION.SDK_INT >= 23) {
                    if (verifyPermission()) return;
                }
                pictureIntent = dispatchTakePictureIntent();
                resultCode = REQUEST_IMAGE_CAPTURE;
                break;
            case 1:
                if (Build.VERSION.SDK_INT >= 23) {
                    if (verifyGalleryPermission()) return;
                }
                pictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                resultCode = REQUEST_IMAGE_GALLERY_PIC;
                break;
            case 2:
                removeProfilePick();
                break;
        }

        if (pictureIntent != null && pictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(pictureIntent, resultCode);
        }

    }

    private void removeProfilePick() {

    }


    @TargetApi(23)
    private boolean verifyGalleryPermission() {
        if (checkSelfPermission(PERMISSION_STORAGE[0]) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(PERMISSION_STORAGE[0])) {
                displayPermissionDialog("Would like to grant access to CameraTest to read your gallery", new String[]{PERMISSION_STORAGE[0]}, REQUEST_PERMISSION);
            } else {
                requestPermissions(new String[]{PERMISSION_STORAGE[0]}, REQUEST_PERMISSION);
            }
            return true;
        }
        return false;
    }

    @TargetApi(23)
    private boolean verifyPermission() {
        if (checkAllPermission()) {
            if (permissionsNeeded.size() > 0 && !isFirstTime) {
                for (String permission : permissionsNeeded) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        displayPermissionDialog("Would like to grant access to CameraTest to take picture from camera", PERMISSION_STORAGE, REQUEST_MULTIPLE_PERMISSION);
                        break;
                    }
                }
            } else {
                isFirstTime = false;
                requestPermissions(PERMISSION_STORAGE, REQUEST_MULTIPLE_PERMISSION);
            }
            return true;
        }
        return false;
    }

    @TargetApi(23)
    private void displayPermissionDialog(String msg, final String[] permission, final int resultCode) {
        AlertDialog alertDialog = new AlertDialog
                .Builder(this)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        requestPermissions(permission, resultCode);
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create();
        alertDialog.show();

    }

    @TargetApi(23)
    private boolean checkAllPermission() {
        boolean isPermissionRequired = false;
        for (String permission : PERMISSION_STORAGE) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
                isPermissionRequired = true;
            }
        }
        return isPermissionRequired;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent pictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pictureIntent, REQUEST_IMAGE_GALLERY_PIC);
                } else {
                    String msg = String.format(Locale.ENGLISH, "%s permission is missing ", "Read");
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_MULTIPLE_PERMISSION:
                boolean isStartActivity = true;
                int position = 0;
                for (int permission : grantResults) {
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        //DO NOTHING
                        Log.i(TAG, "Permission is granted");
                    } else {
                        isStartActivity = false;
                        position = permission;
                    }
                }
                if (isStartActivity) {
                    Intent pictureIntent = dispatchTakePictureIntent();
                    startActivityForResult(pictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    String msg = String.format(Locale.ENGLISH, "%s permission is missing ", position == 1 ? "Camera" : "Read and write");
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                break;

        }

    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    if (mCurrentPhotoPath != null) {
                        galleryAddPic();
                        crop(mCurrentPhotoPath);
                        //  mCurrentPhotoPath = null;
                    }
                    break;
                case REQUEST_IMAGE_GALLERY_PIC:
                    String selectedImagePath = getString(data);
                    if (selectedImagePath != null) {
                        crop(selectedImagePath);
                    } else {
                        Toast.makeText(this, "Unable to select picture please retry again", Toast.LENGTH_SHORT).show();
                    }
                    break;

                /*case REQUEST_IMAGE_CROP:
                    try {
                        Bitmap bitmap = data.getExtras().getParcelable("data");
                        updateImageView(bitmap);
                    } catch (Exception ex) {
                        try {
                            // File file = new File(data.getData().getPath());
                            getBitmap(data.getStringExtra(CropImage.IMAGE_PATH));
                        } catch (Exception ignored) {
                            Toast.makeText(this, "Unable to select picture please retry again", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;*/
            }

        }

    }

    private String getString(Intent data) {
        Uri selectedImageUri = data.getData();

        return getRealFilePath(selectedImageUri);
    }

    public String getRealFilePath(final Uri uri) {

        if (null == uri) return null;

        final String scheme = uri.getScheme();
        String data = null;

        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;


    }

    private void getBitmap(final String photoPath) {
        updateUi(true);
        AsyncTaskCompat.executeParallel(new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                return BitmapUtils.decodeSampledBitmapFromFile(photoPath, getResources().getDimensionPixelOffset(R.dimen.image_width),
                        getResources().getDimensionPixelOffset(R.dimen.image_height));
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                updateUi(false);
                if (bitmap != null) {
                    updateImageView(bitmap);
                }
            }
        });
    }

    public abstract void updateUi(boolean updateUi);

    private void crop(final String photoPath) {
        //  updateUi(true);
        //cropImage(photoPath);
        getBitmap(photoPath);
    }

    /*private void cropImage(String photoPath) {
        Intent intent = new Intent(this, CropImage.class);
        intent.putExtra(CropImage.IMAGE_PATH, photoPath);
        // allow CropImage activity to rescale image
        intent.putExtra(CropImage.SCALE, true);
        // if the aspect ratio is fixed to ratio 3/2
        intent.putExtra(CropImage.ASPECT_X, 1);
        intent.putExtra(CropImage.ASPECT_Y, 1);
        intent.putExtra(CropImage.OUTPUT_X, 256);
        intent.putExtra(CropImage.OUTPUT_Y, 256);
        startActivityForResult(intent, REQUEST_IMAGE_CROP);
    }*/


    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.imageView)
        ImageView imageView;
        @BindView(R.id.textView)
        TextView textView;
        OnItemClickListener listener;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
            this.listener = onItemClickListener;

        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(getAdapterPosition());
        }

    }

    private interface OnItemClickListener {

        void onItemClick(int adapterPosition);
    }

    public List<Item> createItems(boolean isAddRemove) {
        ArrayList<Item> items = new ArrayList<>();
        items.add(new Item(android.R.drawable.ic_menu_camera, "Camera"));
        items.add(new Item(android.R.drawable.ic_menu_gallery, "Gallery"));
        return items;
    }


}