package com.portgo.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.UriUtil;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.portgo.R;


import java.util.Hashtable;

public class ActivityQRScanner extends AppCompatActivity implements
        DecoratedBarcodeView.TorchListener {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private ImageView switchFlashlightButton;
    private ProgressDialog mProgress;
    private String photo_path;
    private Bitmap scanBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_scanner);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        if(toolbar!=null) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.portgo_color_toobar_gray));
            toolbar.setTitle(R.string.scanner_tilte);
            toolbar.setTitleTextAppearance(this,R.style.ToolBarTextAppearance);
            toolbar.setNavigationIcon(R.drawable.nav_back_ico);
            toolbar.setTitleMarginStart(0);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        barcodeScannerView = (DecoratedBarcodeView)findViewById(R.id.zxing_barcode_scanner);
        barcodeScannerView.setTorchListener(this);

        switchFlashlightButton = findViewById(R.id.zxing_switch_flashlight);
        switchFlashlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchFlashlight(view);
            }
        });
        if (!hasFlash()) {
            switchFlashlightButton.setVisibility(View.GONE);
        }

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scanner,menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
    boolean isFlashOn = false;
    public void switchFlashlight(View view) {
        if (!isFlashOn) {
            barcodeScannerView.setTorchOn();
        } else {
            barcodeScannerView.setTorchOff();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                setResult(Activity.RESULT_CANCELED);
                this.finish();
                break;
            case R.id.menu_piture_scanner:
                Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT); //"android.intent.action.GET_CONTENT"
                innerIntent.setType("image/*");
                startActivityForResult(innerIntent, REQUEST_CODE_SCAN_GALLERY);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    final int REQUEST_CODE_SCAN_GALLERY = 32141;
    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SCAN_GALLERY:
                    handleAlbumPic(data);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleAlbumPic(Intent data) {
        photo_path = UriUtil.getRealPathFromUri(ActivityQRScanner.this, data.getData());

        mProgress = new ProgressDialog(ActivityQRScanner.this);
        mProgress.setMessage("Scan...");
        mProgress.setCancelable(false);
        mProgress.show();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgress.dismiss();
                Result result = scanningImage(photo_path);
                if (result != null) {
                    BarcodeResult barcodeResult = new BarcodeResult(result, null);
                    Intent intent = CaptureManager.resultIntent(barcodeResult,photo_path);
                    ActivityQRScanner.this.setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(ActivityQRScanner.this, "failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public Result scanningImage(String path) {
        if(TextUtils.isEmpty(path)){
            return null;
        }
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); //

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //
        scanBitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false; //
        int sampleSize = (int) (options.outHeight / (float) 200);
        if (sampleSize <= 0)
            sampleSize = 1;
        options.inSampleSize = sampleSize;
        scanBitmap = BitmapFactory.decodeFile(path, options);
        RGBLuminanceSource source = RGBLuminanceSourcefromBitmap(scanBitmap);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();
        try {
            return reader.decode(bitmap1, hints);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public RGBLuminanceSource RGBLuminanceSourcefromBitmap(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        return  new RGBLuminanceSource(width,height,pixels);
    }

    @Override
    public void onTorchOn() {
        switchFlashlightButton.setImageResource(R.drawable.flash_on);
        isFlashOn =true;
    }

    @Override
    public void onTorchOff() {
        isFlashOn =false;
        switchFlashlightButton.setImageResource(R.drawable.flash_off);
    }

}
