package com.portgo.util;

import android.text.InputFilter;
import android.text.Spanned;

import java.io.File;

public  class MIMEType{
    public static final String  MIMETYPE_custom_file = "file/";
    public static final String  MIMETYPE_image = "image/";
    public static final String  MIMETYPE_video ="video/";
    public static final String  MIMETYPE_audio ="audio/";
    public static final String  MIMETYPE_text ="text/";
    public static final String  MIMETYPE_textplain ="text/plain";
    public static final String  MIMETYPE_appJson ="application/json";
    public static final String  MIMETYPE_audiompeg ="audio/mpeg";
    public static final String  MIMETYPE_audioamr ="audio/amr";
    public static final String  MIMETYPE_audiowav ="audio/wav";
    public static final String  MIMETYPE_videompeg ="video/mpeg";
    public static final String  MIMETYPE_videomp4 ="video/mp4";

    public static final String  MIMETYPE_imagejpeg = "image/jpeg";
    public static final String  MIMETYPE_imagejpg = "image/jpg";
    public static final String  MIMETYPE_imagepng = "image/png";

    public static final String  MIMETYPE_videomp4_EXT =".mp4";
    public static final String  MIMETYPE_imagejpg_EXT =".jpg";

    static public  String getExtensionNameByMIME(String mimeType){
        String extName = "";
        switch (mimeType){
            case MIMETYPE_audiompeg:
                extName = ".jpg";
                break;
            case MIMETYPE_audioamr:
                extName = ".amr";
                break;
            case MIMETYPE_videomp4:
                extName = ".mp4";
                break;
            case MIMETYPE_videompeg:
                extName = ".mp4";
                break;
            case MIMETYPE_audiowav:
                extName = ".wav";
                break;
                default:
                    break;
//            case MIMETYPE_audiowav:
//                extName = ".wav";
//                break;
        }

        return extName;

    }

    public static String getMIMEType(String fName) {
        String type="*/*";
        int dotIndex = fName.lastIndexOf(".");
        if(dotIndex < 0)
            return type;
        String fileType = fName.substring(dotIndex,fName.length()).toLowerCase();
        if(fileType == null || "".equals(fileType))
            return type;

        for(int i=0;i<MIME_MapTable.length;i++){
            if(fileType.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }
    public static String getMIMEType(File file) {
        String fName = file.getName();
        return getMIMEType(fName);
    }


    private static final String[][] MIME_MapTable={

            {".3gp",    "video/3gpp"},
            {".apk",    "application/vnd.android.package-archive"},
            {".asf",    "video/x-ms-asf"},
            {".avi",    "video/x-msvideo"},
            {".bin",    "application/octet-stream"},
            {".bmp",      "image/bmp"},
            {".c",        "text/plain"},
            {".class",    "application/octet-stream"},
            {".conf",    "text/plain"},
            {".cpp",    "text/plain"},
            {".doc",    "application/msword"},
            {".exe",    "application/octet-stream"},
            {".gif",    "image/gif"},
            {".gtar",    "application/x-gtar"},
            {".gz",        "application/x-gzip"},
            {".h",        "text/plain"},
            {".htm",    "text/html"},
            {".html",    "text/html"},
            {".jar",    "application/java-archive"},
            {".java",    "text/plain"},
            {".jpeg",    "image/jpeg"},
            {".jpg",    "image/jpeg"},
            {".js",        "application/x-javascript"},
            {".log",    "text/plain"},
            {".m3u",    "audio/x-mpegurl"},
            {".m4a",    "audio/mp4a-latm"},
            {".m4b",    "audio/mp4a-latm"},
            {".m4p",    "audio/mp4a-latm"},
            {".m4u",    "video/vnd.mpegurl"},
            {".m4v",    "video/x-m4v"},
            {".mov",    "video/quicktime"},
            {".mp2",    "audio/x-mpeg"},
            {".mp3",    "audio/x-mpeg"},
            {".mp4",    "video/mp4"},
            {".mpc",    "application/vnd.mpohun.certificate"},
            {".mpe",    "video/mpeg"},
            {".mpeg",    "video/mpeg"},
            {".mpg",    "video/mpeg"},
            {".mpg4",    "video/mp4"},
            {".mpga",    "audio/mpeg"},
            {".msg",    "application/vnd.ms-outlook"},
            {".ogg",    "audio/ogg"},
            {".pdf",    "application/pdf"},
            {".png",    "image/png"},
            {".pps",    "application/vnd.ms-powerpoint"},
            {".ppt",    "application/vnd.ms-powerpoint"},
            {".prop",    "text/plain"},
            {".rar",    "application/x-rar-compressed"},
            {".rc",        "text/plain"},
            {".rmvb",    "audio/x-pn-realaudio"},
            {".rtf",    "application/rtf"},
            {".sh",        "text/plain"},
            {".tar",    "application/x-tar"},
            {".tgz",    "application/x-compressed"},
            {".txt",    "text/plain"},
            {".wav",    "audio/x-wav"},
            {".wma",    "audio/x-ms-wma"},
            {".wmv",    "audio/x-ms-wmv"},
            {".wps",    "application/vnd.ms-works"},
            //{".xml",    "text/xml"},
            {".xml",    "text/plain"},
            {".z",        "application/x-compress"},
            {".zip",    "application/zip"},
            {"",        "*/*"}
    };

}