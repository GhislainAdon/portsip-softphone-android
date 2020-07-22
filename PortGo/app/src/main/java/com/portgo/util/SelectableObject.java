package com.portgo.util;

import android.widget.Checkable;

/**
 * Created by huacai on 2017/11/21.
 */

public class SelectableObject<T> implements Checkable{
    boolean check = false;
    final T object;

    public SelectableObject(T object) {
        this.object = object;
        this.check  = false;
    }

    public T getObject() {
        return object;
    }

    public SelectableObject(T object, boolean checked) {
        this.object = object;
        this.check  = checked;
    }


    @Override
    public void setChecked(boolean b) {
        check = b;
    }

    @Override
    public boolean isChecked() {
        return check;
    }

    @Override
    public void toggle() {
        check = !check;
    }
}
