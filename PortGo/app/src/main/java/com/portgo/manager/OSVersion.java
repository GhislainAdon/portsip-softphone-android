package com.portgo.manager;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by huacai on 2017/8/20.
 */

public class OSVersion {
    public enum ROM_TYPE {
        MIUI_ROM,
        FLYME_ROM,
        EMUI_ROM,
        OTHER_ROM
    }


//MIUI
    private static final String KEY_MIUI_VERSION_CODE="ro.miui.ui.version.code";
    public static final String KEY_MIUI_VERSION_NAME="ro.miui.ui.version.name";
//EMUI
    private static final String KEY_EMUI_VERSION_CODE="ro.build.version.emui";
//Flyme
    private static final String KEY_FLYME_ID_FALG_KEY="ro.build.display.id";
    private static final String KEY_FLYME_ID_FALG_VALUE_KEYWORD="Flyme";
    private static final String KEY_FLYME_ICON_FALG="persist.sys.use.flyme.icon";
    private static final String KEY_FLYME_SETUP_FALG="ro.meizu.setupwizard.flyme";

    private static final String KEY_FLYME_PUBLISH_FALG="ro.flyme.published";

    public static String getRomName(String versionKey){
        try {
            OSBuildProperty buildProperties = OSBuildProperty.getInstance();
            return buildProperties.getProperty(versionKey);
        }catch (IOException e){
            e.printStackTrace();
        }
        return  null;
    }
    public static ROM_TYPE getRomType() {

        ROM_TYPE rom_type = ROM_TYPE.OTHER_ROM;

        try{

            OSBuildProperty buildProperties = OSBuildProperty.getInstance();

            if(buildProperties.containsKey(KEY_EMUI_VERSION_CODE)) {

                return ROM_TYPE.EMUI_ROM;

            }

            if(buildProperties.containsKey(KEY_MIUI_VERSION_CODE) || buildProperties.containsKey(KEY_MIUI_VERSION_NAME)) {

                return ROM_TYPE.MIUI_ROM;

            }

            if(buildProperties.containsKey(KEY_FLYME_ICON_FALG) || buildProperties.containsKey(KEY_FLYME_SETUP_FALG) || buildProperties.containsKey(KEY_FLYME_PUBLISH_FALG)) {

                return ROM_TYPE.FLYME_ROM;

            }

            if(buildProperties.containsKey(KEY_FLYME_ID_FALG_KEY)) {

                String romName = buildProperties.getProperty(KEY_FLYME_ID_FALG_KEY);

                if(!TextUtils.isEmpty(romName) && romName.contains(KEY_FLYME_ID_FALG_VALUE_KEYWORD)) {

                    return ROM_TYPE.FLYME_ROM;

                }

            }

        }catch(IOException e) {

            e.printStackTrace();

        }
        return rom_type;
    }

}
