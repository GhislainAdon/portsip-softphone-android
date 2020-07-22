package com.portgo.database;

import android.database.Cursor;
//import android.database.CursorJoiner;


/**
 * Contact class defining an entity from the native address book or XCAP server.
 */

public class CursorJoiner4Loader {
    private PortCursorJoiner joiner;
    private Cursor cursorLeft;
    private Cursor cursorRight;

    public CursorJoiner4Loader(PortCursorJoiner cursorJoiner,Cursor cursorLeft,Cursor cursorRight)
    {
        this.joiner=cursorJoiner;this.cursorLeft=cursorLeft;this.cursorRight=cursorRight;
    }
    public void setCursorLeft(Cursor cursorLeft) {
        this.cursorLeft = cursorLeft;
    }

    public Cursor getCursorLeft() {
        return cursorLeft;
    }

    public void setCursorRight(Cursor cursorRight) {
        this.cursorRight = cursorRight;
    }

    public Cursor getCursorRight() {
        return cursorRight;
    }

    public PortCursorJoiner getJoiner() {
        return joiner;
    }

    public void setJoiner(PortCursorJoiner joiner) {
        this.joiner = joiner;
    }
}
