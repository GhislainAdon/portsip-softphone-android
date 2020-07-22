package com.portgo.database;

import android.database.Cursor;

public class CursorHepperWrapper extends android.database.CursorWrapper{
    public CursorHepperWrapper(Cursor cursor){
        super(cursor);
    }
    @Override
    protected void finalize(){
        if(getWrappedCursor()==null){
            return;
        }
        if(!isClosed()){
            close();
        }
    }

    @Override
    public boolean isClosed() {
        if(getWrappedCursor()==null){
            return true;
        }
        return super.isClosed();
    }
}
