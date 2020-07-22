package com.portgo.manager;

import android.content.ContentValues;
import android.provider.ContactsContract;
import android.text.TextUtils;

import static com.portgo.manager.ContactDataAdapter.DATA_ACTION.ACTION_NONE;

public abstract class ContactDataAdapter {
    protected int mId =-1;//INVALIDE_ID
    protected String mData1="";
    protected int mData2=0;
    protected String mData3;
    protected DATA_ACTION mAction = ACTION_NONE;

    public enum DATA_ACTION{
        ACTION_NONE,
        ACTION_ADD,
        ACTION_DEL,
    };
    ContactDataAdapter(int id, String data1, int data2, String data3){
        this.mId=id;
        this.mData1=data1;
        this.mData2=data2;
        this.mData3 =data3;

    }

    public int getId(){
        return mId;
    }
    public void setAction(DATA_ACTION mAction) {
        this.mAction = mAction;
    }

    public DATA_ACTION getAction() {
        return mAction;
    }

    public void setType(int type) {
        this.mData2 = type;
    }

    public void setLable(String lable) {
        this.mData3 = lable;
    }

    public void setNumber(String number) {
        this.mData1 = number;
    }

    ContentValues getContentValue(){
        ContentValues values = null;
        if(dataAvailable()){
            values = new ContentValues();
            values.put(ContactsContract.Data.DATA1,mData1);
            values.put(ContactsContract.Data.DATA2,mData2);
            if(mData2==0) {//type custom
                values.put(ContactsContract.Data.DATA3, mData3);
            }
        }
        return values;
    }

    /**
     *
     * @return true available false unavailable
     */
    boolean dataAvailable(){
        return !(TextUtils.isEmpty(mData1)||(mData2 == 0&&TextUtils.isEmpty(mData3)));
    }
}