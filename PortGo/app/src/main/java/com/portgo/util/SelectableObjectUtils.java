package com.portgo.util;

import androidx.annotation.NonNull;
import android.widget.Checkable;

import java.util.ArrayList;

/**
 * Created by huacai on 2017/11/21.
 */

public class SelectableObjectUtils<T> {
    public static <T> void add(@NonNull  ArrayList<SelectableObject<T>> objects,T obj){
        objects.add(getSelectableObject(obj,false));
    }

    public static <T> void remove(ArrayList<SelectableObject<T>> objects,T obj){
        for(SelectableObject<T> selectableObject:objects) {
            if(selectableObject.getObject()==obj) {
                objects.remove(selectableObject);
                break;
            }
        }
    }

    public static <T>  ArrayList<SelectableObject<T>> getSelectAbleObjetList(ArrayList<T> list){
        ArrayList<SelectableObject<T>> arrayList = new ArrayList<>();
        for(T ob:list){
            arrayList.add(getSelectableObject(ob,false));
        }
        return arrayList;
    }

    public static <T> void  setListSelectStatus(@NonNull ArrayList<SelectableObject<T>> selectableObjects, boolean checked) {

        for(SelectableObject<T> ob:selectableObjects){
            ob.setChecked(checked);
        }

    }

    public static <T> boolean isAllObjectChecked(@NonNull ArrayList<SelectableObject<T>> selectableObjects) {
        for(SelectableObject<T> ob:selectableObjects){
            if(!ob.isChecked()){
                return false;
            }
        }
        return true;
    }

    public static <T> SelectableObject getSelectableObject(T object,boolean checked){
        return new SelectableObject<T>(object,checked);
    }
}
