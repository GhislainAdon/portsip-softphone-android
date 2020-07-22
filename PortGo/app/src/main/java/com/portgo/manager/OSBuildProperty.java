package com.portgo.manager;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * Created by huacai on 2017/8/20.
 */

public class OSBuildProperty {
    private final Properties mProperties;
    static  OSBuildProperty mInstance;
    private OSBuildProperty() throws IOException {
        mProperties = new Properties();
        mProperties.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
    }

    public boolean containsKey(final Object key) {
        return mProperties.containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return mProperties.containsValue(value);
    }

    public Set<Entry<Object, Object>> entrySet() {
        return mProperties.entrySet();
    }

    public String getProperty(final String name) {
        return mProperties.getProperty(name);
    }

    public String getProperty(final String name, final String defaultValue) {
        return mProperties.getProperty(name, defaultValue);
    }

    public boolean isEmpty() {
        return mProperties.isEmpty();
    }

    public Enumeration<Object> keys() {
        return mProperties.keys();
    }

    public Set<Object> keySet() {
        return mProperties.keySet();
    }

    public int size() {
        return mProperties.size();
    }

    public Collection<Object> values() {
        return mProperties.values();
    }

    public static OSBuildProperty getInstance() throws IOException {
        if(mInstance==null){
            synchronized (OSBuildProperty.class){
                mInstance = new OSBuildProperty();
            }
        }
        return mInstance;
    }
}
