package com.portgo.database;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

public class CursorJoinerLoader extends AsyncTaskLoader<CursorJoiner4Loader> {
    final ForceLoadContentObserver mObserver;
    PortCursorJoiner.JoinerCompare joinerCompare;
    Uri mUriLeft, mUriRight;
    String[] mProjectionLeft,mProjectionRight;
    String mSelectionLeft,mSelectionRight;
    String[] mSelectionArgsLeft,mSelectionArgsRight;
    String mSortOrderLeft,mSortOrderRight;
    String[] mJoinLeft,mJoinRight;

    CancellationSignal mCancellationSignal;
    CursorJoiner4Loader mJoiner4Loader;

    /* Runs on a worker thread */
    @Override
    public CursorJoiner4Loader loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
        try {
            PortCursorJoiner joiner = null;
            Cursor cursorLeft = getContext().getContentResolver().query(mUriLeft, mProjectionLeft, mSelectionLeft,
                    mSelectionArgsLeft, mSortOrderLeft, mCancellationSignal);
            Cursor cursorRight = getContext().getContentResolver().query(mUriRight, mProjectionRight, mSelectionRight,
                    mSelectionArgsRight, mSortOrderRight, mCancellationSignal);
            if (cursorLeft != null&&cursorRight!=null) {
                try {
                    // Ensure the cursor window is filled.
                    cursorLeft.registerContentObserver(mObserver);
                    cursorRight.registerContentObserver(mObserver);
                    joiner = new PortCursorJoiner(cursorLeft,mJoinLeft,
                            cursorRight,mJoinRight,joinerCompare);

                } catch (RuntimeException ex) {
                    cursorLeft.close();
                    cursorRight.close();
                    throw ex;
                }
            }
            return new CursorJoiner4Loader(joiner,cursorLeft,cursorRight);
        }
        finally{
            synchronized (this) {
                mCancellationSignal = null;
            }
        }
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(CursorJoiner4Loader joiner) {
        if (isReset()) {//
            // An async query came in while the loader is stopped
            Cursor cursorLeft = joiner.getCursorLeft();
            Cursor cursorRight = joiner.getCursorRight();
            if (cursorLeft != null) {
                cursorLeft.close();
            }

            if (cursorRight != null) {
                cursorRight.close();
            }
            return;
        }
        CursorJoiner4Loader oldjoiner = mJoiner4Loader;
        mJoiner4Loader = joiner;

        if (isStarted()) {
            super.deliverResult(joiner);
        }

        if (oldjoiner != null ) {
            Cursor left = oldjoiner.getCursorLeft();
            Cursor right = oldjoiner.getCursorRight();
            if(left != joiner.getCursorLeft() && !left.isClosed()){
                left.close();
            }

            if(right != joiner.getCursorRight() && !right.isClosed()){
                right.close();
            }
        }
    }

    /**
     * Creates a fully-specified CursorJoinerLoader.  See
     */
    public CursorJoinerLoader(Context context, Uri uriLeft, String[] projectionLeft, String selectionLeft,
                        String[] selectionArgsLeft, String sortOrderLeft,
                        Uri uriRight, String[] projectionRight, String selectionRight,
                        String[] selectionArgsRight, String sortOrderRight ,String[] joinLeft,String[] joinRight,PortCursorJoiner.JoinerCompare compare) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        mUriLeft = uriLeft;
        mProjectionLeft = projectionLeft;
        mSelectionLeft = selectionLeft;
        mSelectionArgsLeft = selectionArgsLeft;
        mSortOrderLeft = sortOrderLeft;

        mUriRight = uriRight;
        mProjectionRight = projectionRight;
        mSelectionRight = selectionRight;
        mSelectionArgsRight = selectionArgsRight;
        mSortOrderRight = sortOrderRight;
        mJoinLeft =joinLeft ;
        mJoinRight =joinRight;
        joinerCompare = compare;
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mJoiner4Loader!=null) {
            deliverResult(mJoiner4Loader);
        }
        if (takeContentChanged() || mJoiner4Loader==null||mJoiner4Loader.getCursorLeft() == null||mJoiner4Loader.getCursorRight() == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(CursorJoiner4Loader joiner4Loader) {
        if(joiner4Loader!=null) {
            Cursor cursorLeft = joiner4Loader.getCursorLeft();
            Cursor cursorRight = joiner4Loader.getCursorLeft();
            joiner4Loader.getCursorRight();
            if (cursorLeft != null && !cursorLeft.isClosed()) {
                cursorLeft.close();
            }

            if (cursorRight != null && !cursorRight.isClosed()) {
                cursorRight.close();
            }
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
        if(mJoiner4Loader!=null) {
            Cursor cursorLeft = mJoiner4Loader.getCursorLeft();
            Cursor cursorRight = mJoiner4Loader.getCursorLeft();
            if (cursorLeft != null && !cursorLeft.isClosed()) {
                cursorLeft.close();
            }

            if (cursorRight != null && !cursorRight.isClosed()) {
                cursorRight.close();
            }
        }
        mJoiner4Loader =null;
    }

}