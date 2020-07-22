package com.portgo.ui;
//

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
//import com.github.chrisbanes.photoview.PhotoView;

import com.portgo.R;
import com.portgo.util.MIMEType;

import photoview.OnScaleChangedListener;
import photoview.PhotoView;

public class PortActivityImageView extends Activity implements View.OnClickListener, OnScaleChangedListener {
    PhotoView photoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_image_view);

        Intent intent = getIntent();
        String type = intent.getType();
        Uri data = intent.getData();
        if(data ==null&&type==null||!type.startsWith(MIMEType.MIMETYPE_image)){
            this.finish();
        }

        photoView = findViewById(R.id.image_photo_view);
        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoView.setOnScaleChangeListener(this);
        photoView.setImageURI(getIntent().getData());


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        photoView = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }


    @Override
    public void onScaleChange(float scaleFactor, float focusX, float focusY) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        photoView.setLayoutParams(params);
    }
}