package com.portgo.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.portgo.PortApplication;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by huacai on 2017/8/22.
 */

public class VideoThumbnailUtils {

    static public final int width=240,height =240;

    public static Bitmap getVideoThumbnail(String videoPath,String thumbnailPath,String fileName, int width, int height, int kind) {
        Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath+File.separator+fileName);
        if(bitmap==null||bitmap.getWidth()!=width||bitmap.getHeight()!=height) {
            bitmap = getVideoThumbnail(videoPath+fileName, kind);
            if(bitmap!= null){
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                bitmap2File(thumbnailPath,bitmap,50,fileName);
            }
        }

        return bitmap;
    }

    public static long getAVDuration(File file,long defaultDuration){
        long duration = defaultDuration;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(file.getAbsolutePath());
            String strDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.valueOf(strDuration);

            long fileSize = file.length();
            long bitRate = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            duration = (fileSize*8) /(bitRate);
        }catch (Exception e){
        }finally {
            mmr.release();
        }

        return duration;
    }
    public static Bitmap getVideoThumbnail(@NonNull String videoPath,String thumbnailPath, int maxWidth, int maxHeight) {
        maxWidth = maxWidth<10?10:maxWidth;
        maxHeight = maxHeight<10?10:maxHeight;
        Bitmap bitmap = null;
        String fileName;
        File file = new File(videoPath);

        if(!file.exists()||TextUtils.isEmpty(videoPath))
            return null;
        String thumbnailJpgName = null;
        if(!TextUtils.isEmpty(thumbnailPath)) {
            fileName = file.getName();
            int index = fileName.lastIndexOf('.');
            if (index > 0) {
                thumbnailJpgName = fileName.substring(0, index) + ".jpg";
            } else {
                thumbnailJpgName += fileName + ".jpg";
            }
            bitmap = BitmapFactory.decodeFile(thumbnailPath + File.separator + thumbnailJpgName);
        }


        if(bitmap==null) {
            bitmap = getVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
        }

        if(bitmap!=null&&(bitmap.getWidth()>maxWidth||bitmap.getHeight()>maxHeight)){
            bitmap = getBitmapThumbnail(bitmap,thumbnailPath,thumbnailJpgName,maxWidth,maxHeight);
        }
        return bitmap;
    }


    public static Bitmap getBitmapThumbnail(@NonNull Bitmap bitmap, String thumbnailPath,@NonNull String fileName, int maxWidth, int maxHeight){
        maxWidth = maxWidth<10?10:maxWidth;
        maxHeight = maxHeight<10?10:maxHeight;
        if(TextUtils.isEmpty(fileName))
            return null;
        Bitmap result=null;
        if(bitmap!= null){
            float heightRate = bitmap.getHeight()/(float)maxHeight;
            float widthRate = bitmap.getWidth()/(float)maxWidth;
            float rate = heightRate>widthRate?heightRate:widthRate;
            if(rate>1) {
                result = ThumbnailUtils.extractThumbnail(bitmap, (int)(bitmap.getWidth()/rate), (int)(bitmap.getHeight()/rate),
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                if(!TextUtils.isEmpty(thumbnailPath)) {
                    bitmap2File(thumbnailPath, result, 50, fileName);
                }
            }else {
                result =bitmap;
            }
        }
        return result;
    }

    public static Bitmap getBitmapThumbnail(String filePath,String thumbnailPath,String fileName, int maxWidth, int maxHeight){
        if(TextUtils.isEmpty(filePath)||TextUtils.isEmpty(thumbnailPath)||TextUtils.isEmpty(fileName))
            return null;
        Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath+File.separator+fileName);
        if(bitmap==null||bitmap.getWidth()>maxWidth||bitmap.getHeight()>maxHeight) {
            bitmap =BitmapFactory.decodeFile(filePath+File.separator+fileName);
            bitmap = getBitmapThumbnail(bitmap,thumbnailPath,fileName,maxWidth,maxHeight);
        }
        return bitmap;
    }

    private static Bitmap getVideoThumbnail(String videoPath, int kind) {
        Bitmap bitmap = null;
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        return bitmap;
    }

    public  static String bitmap2File(String path,Bitmap bitmap, int qulity,String name) {

        File f = new File(path + File.separator+name);
        FileOutputStream fOut = null;
        try  {
            if  (f.exists()) {
                f.delete();
            }else{
                File fileParent = f.getParentFile();
                if(fileParent!=null&&!fileParent.exists()) {
                    f.mkdirs();
                }
            }
            fOut = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, qulity, fOut);
            fOut.flush();
            fOut.close();

        } catch (IOException e) {
            return  null;
        }
        return  f.getAbsolutePath();
    }


    public static void appendVideo(Context context, String saveVideoPath, List<String> videos)throws IOException{

        if(videos==null)
            return ;

        Movie[] inMovies =new Movie[videos.size()];
        int index =0;

        for(String video:videos)
        {
            inMovies[index] = MovieCreator.build(video);
            index++;
        }

        List<Track> videoTracks =new LinkedList();
        List<Track> audioTracks =new LinkedList();

        for(Movie m : inMovies) {
            for(Track t : m.getTracks()) {
                if(t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                if(t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }
            }
        }
        Movie result =new Movie();

        if(audioTracks.size() >0) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }

        if(videoTracks.size() >0) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }

        Container out =new DefaultMp4Builder().build(result);
        FileChannel fc =new RandomAccessFile(String.format(saveVideoPath),"rw").getChannel();
        out.writeContainer(fc);
        fc.close();

    }

    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public static Bitmap rotaingImageView(int angle,Bitmap bitmap) {
        Bitmap returnBm = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        try {
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }

    public static Bitmap rotaingImageView(int angle,Bitmap bitmap,boolean salex,boolean saley) {
        Bitmap returnBm = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        if(salex){
            matrix.postScale(-1, 1);
        }
        if(saley){
            matrix.postScale(1, -1);
        }
        try {
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }

}

