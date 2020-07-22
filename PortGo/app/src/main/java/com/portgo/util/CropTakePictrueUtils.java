package com.portgo.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.portgo.BuildConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.portgo.util.ImagePathUtils.getFileProviderUri;
import static com.portgo.util.ImagePathUtils.hasSdcard;

/**
 * Created by huacai on 2017/8/22.
 */

public class CropTakePictrueUtils {
    static public final int CROP_PHOTO = 0x116;
    static public final int TAKE_PHOTO = CROP_PHOTO+1;
    static public final int CHOOSE_PHOTO = CROP_PHOTO+2;
    static public final int CHOOSE_PHOTO_KITKAT = CROP_PHOTO+3;
    static String IMAGE_SUBPATH =  "avatars/";
    static String IMAGE_FILE_NAME = "avatar1.jpg" ;
    static String CROP_IMAGE_FILE_NAME = "avatar2.jpg" ;
    static String IMAGE_GALLERY_NAME = "avatar3.jpg" ;

    int PHOTO_WIDTH=400,PHOTO_HEIGTH =400;


    File mCameraFile,mCropFile,mGalleryFile;
    public CropTakePictrueUtils(Activity activity){
        String path = activity.getExternalFilesDir(null).getAbsolutePath()+IMAGE_SUBPATH;
        File pathfile = new File(path);
        if(!pathfile.exists()){
            pathfile.mkdirs();
        }
        mCameraFile = new File(path, IMAGE_FILE_NAME);
        mCropFile = new File(path, CROP_IMAGE_FILE_NAME);
        mGalleryFile = new File(path, IMAGE_GALLERY_NAME);
    }

    public Uri getCamareUri(Context context){
        return getFileProviderUri(context, mCameraFile);
    }


    public String getCropFilePath(){
        return mCropFile.getAbsolutePath();
    }

    public String getCameraFilePath(){
        return mCameraFile.getAbsolutePath();
    }
    /**
     * 裁剪图片方法实现 * * @param inputUri
     */
    public void startPhotoZoom(Activity activity, Uri inputUri,int width,int height) {
        if (inputUri == null) {
            return;
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
//        Uri outPutUri =getCropUri(activity);
        Uri outPutUri = Uri.fromFile(mCropFile);
        //sdk>=24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setDataAndType(inputUri, "image/*");
            intent.putExtra("noFaceDetection", false);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        } else {
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                String url = ImagePathUtils.getPath(activity, inputUri);
                intent.setDataAndType(Uri.fromFile(new File(url)), "image/*");
            } else {
                intent.setDataAndType(inputUri, "image/*");
            }

        }
        //
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
        intent.putExtra("crop", "true"); // aspectX aspectY
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1); // outputX outputY
        intent.putExtra("outputX", width);
        intent.putExtra("outputY", height);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        activity.startActivityForResult(intent,CROP_PHOTO );
    }

    public void seletPicture(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//
            Uri uriForFile = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID, mGalleryFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivityForResult(intent, CHOOSE_PHOTO_KITKAT);
        } else {
            activity.startActivityForResult(intent, CHOOSE_PHOTO);
        }
    }

    public void seletPicture(Fragment fragment,Context context) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//
            Uri uriForFile = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, mGalleryFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fragment.startActivityForResult(intent, CHOOSE_PHOTO_KITKAT);
        } else {
            fragment.startActivityForResult(intent, CHOOSE_PHOTO);
        }
    }

    /**
     */
    public void startPhotoZoom(Activity activity, Uri inputUri) {
        startPhotoZoom(activity, inputUri,PHOTO_WIDTH,PHOTO_HEIGTH);
    }



    public void capturPicture(Activity activity) {
        Intent intentFromCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (hasSdcard()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//7
                Uri uriForFile = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID, mCameraFile);
                intentFromCapture.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
                intentFromCapture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intentFromCapture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } else {
                intentFromCapture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCameraFile));
            }
        }
        activity.startActivityForResult(intentFromCapture, TAKE_PHOTO);
    }

    /**
     * @param uri：
     * @return Bitmap；
     */
    public static Bitmap decodeUriAsBitmap(ContentResolver resolver, Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(resolver.openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    public Bitmap getCropedBitmap(Context context){
        return decodeUriAsBitmap(context.getContentResolver(),Uri.fromFile(mCropFile));
    }


    public Uri saveBitmapforCrop(Context context,Bitmap bmp){
        return saveBitmap(context,bmp,mCameraFile.getAbsolutePath());
    }

    public Uri saveBitmap(Context context,Bitmap bmp,String name){
        File imageFile = new File(name);
        FileOutputStream out = null;
        //
        try {
            if (imageFile.exists()) {
                imageFile.delete();
            }
            imageFile.createNewFile();
            if(bmp!=null){
                out = new FileOutputStream(imageFile);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                return getFileProviderUri(context,imageFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
