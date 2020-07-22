package com.portgo.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;


import com.portgo.PortApplication;

import java.io.InputStream;

/**
 * Created by qqche_000 on 2018/6/3.
 * 用于OpenGl的工具类
 */

public class OpenGlUtils {
    //通过资源路径加载shader脚本文件
    public static String uRes(Context context, String path) {
        Resources resources = context.getResources();
        StringBuilder result = new StringBuilder();
        try {
            InputStream is = resources.getAssets().open(path);
            int ch;
            byte[] buffer = new byte[1024];
            while (-1 != (ch = is.read(buffer))) {
                result.append(new String(buffer, 0, ch));
            }
        } catch (Exception e) {
        }
        return result.toString().replaceAll("\\r\\n", "\n");
    }
}
