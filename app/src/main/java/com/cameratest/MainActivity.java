package com.cameratest;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends BaseProfilePicActivity {

    @BindView(R.id.openBottomSheetBtn)
    Button openBottomSheetBtn;
    @BindView(R.id.profilePicImgView)
    CircleImageView profilePicImgView;

    @Override
    public void updateImageView(Bitmap bitmap) {
        profilePicImgView.setImageBitmap(bitmap);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    public void updateUi(boolean updateUi) {

    }

    @OnClick(R.id.openBottomSheetBtn)
    public void ButtonClick() {
        showBottomSheetDialog(true);
    }
}
