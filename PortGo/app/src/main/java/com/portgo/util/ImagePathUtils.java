package com.portgo.util;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.portgo.BuildConfig;
import com.portgo.R;
import com.portgo.manager.CursorHelper;
import com.portgo.manager.OSVersion;

import java.io.File;

/**
 * Created by huacai on 2017/8/22.
 */

public class ImagePathUtils {
    public static Uri getProviderUri(final Context context, final Uri uri) {
        String path = getPath(context, uri);
        Uri encodeUri = null;
        if(path!=null) {
            File imgUri = new File(path);
            encodeUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, imgUri);
        }
        return encodeUri;
    }
    public static boolean hasSdcard() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else
            return false;
    }

    public static Uri getFileProviderUri(Context context,File file){
        Uri  resutl =null;
        if(hasSdcard()) {
            if (Build.VERSION.SDK_INT >= 24) {
                resutl = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file);//通过FileProvider创建一个content类型的Uri
            } else {
                resutl = Uri.fromFile(file);
            }
        }
        return  resutl;
    }

    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) { // Return the remote address
            if (isGooglePhotosUri(uri)) return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    //Android 4.4以下版本自动使用该方法
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = CursorHelper.resolverQuery(context.getContentResolver(),uri, projection, selection, selectionArgs, null);
            if (CursorHelper.moveCursorToFirst(cursor)) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (IllegalArgumentException e) {
            e.toString();
        }finally {
            CursorHelper.closeCursor(cursor);
        }
        return null;
    }

    /**
     * @param uri The Uri to check. * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check. * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check. * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check. * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getAvatarFileDir(Context context){
        File avatarFile,oldAvatarFile;
        String oldPath,newPath;
        avatarFile = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        newPath = avatarFile.getPath()+File.separator+"img"+File.separator;
        oldAvatarFile = Environment.getExternalStorageDirectory();
        oldPath = oldAvatarFile.getPath()+File.separator+context.getResources().getString(R.string.app_name) +File.separator+"img"+File.separator;
        File file =new File(oldPath);
        File newFile = new File(newPath);
        if(file.exists()){
            file.renameTo(newFile);
        }else{
            if(!newFile.exists()){
                newFile.mkdirs();
            }
        }

        return newFile.getPath();
    }
}
