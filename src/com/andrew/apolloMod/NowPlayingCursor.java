
package com.andrew.apolloMod;

import java.util.Arrays;

import android.content.Context;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;

import com.andrew.apolloMod.IApolloService;
import com.andrew.apolloMod.helpers.utils.MusicUtils;

public class NowPlayingCursor extends AbstractCursor {

    public NowPlayingCursor(IApolloService service, String[] projection, Context c) {
        mProjection = projection;
        mService = service;
        makeNowPlayingCursor();
        context = c;
    }

    private void makeNowPlayingCursor() {
        mCurrentPlaylistCursor = null;
        try {
            mNowPlaying = mService.getQueue();
        } catch (RemoteException ex) {
            mNowPlaying = new long[0];
        }
        mSize = mNowPlaying.length;
        if (mSize == 0) {
            return;
        }

        StringBuilder where = new StringBuilder();
        where.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < mSize; i++) {
            where.append(mNowPlaying[i]);
            if (i < mSize - 1) {
                where.append(",");
            }
        }
        where.append(")");

        mCurrentPlaylistCursor = MusicUtils.query(context, Audio.Media.EXTERNAL_CONTENT_URI,
                mProjection, where.toString(), null, BaseColumns._ID);

        if (mCurrentPlaylistCursor == null) {
            mSize = 0;
            return;
        }

        int size = mCurrentPlaylistCursor.getCount();
        mCursorIdxs = new long[size];
        mCurrentPlaylistCursor.moveToFirst();
        int colidx = mCurrentPlaylistCursor.getColumnIndexOrThrow(BaseColumns._ID);
        for (int i = 0; i < size; i++) {
            mCursorIdxs[i] = mCurrentPlaylistCursor.getLong(colidx);
            mCurrentPlaylistCursor.moveToNext();
        }
        mCurrentPlaylistCursor.moveToFirst();
        try {
            int removed = 0;
            for (int i = mNowPlaying.length - 1; i >= 0; i--) {
                long trackid = mNowPlaying[i];
                int crsridx = Arrays.binarySearch(mCursorIdxs, trackid);
                if (crsridx < 0) {
                    removed += mService.removeTrack(trackid);
                }
            }
            if (removed > 0) {
                mNowPlaying = mService.getQueue();
                mSize = mNowPlaying.length;
                if (mSize == 0) {
                    mCursorIdxs = null;
                    return;
                }
            }
        } catch (RemoteException ex) {
            mNowPlaying = new long[0];
        }
    }

    @Override
    public int getCount() {
        return mSize;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        if (oldPosition == newPosition)
            return true;

        if (mNowPlaying == null || mCursorIdxs == null || newPosition >= mNowPlaying.length) {
            return false;
        }

        // The cursor doesn't have any duplicates in it, and is not ordered
        // in queue-order, so we need to figure out where in the cursor we
        // should be.

        long newid = mNowPlaying[newPosition];
        int crsridx = Arrays.binarySearch(mCursorIdxs, newid);
        mCurrentPlaylistCursor.moveToPosition(crsridx);
        return true;
    }

    @Override
    public String getString(int column) {
        try {
            return mCurrentPlaylistCursor.getString(column);
        } catch (Exception ex) {
            onChange(true);
            return "";
        }
    }

    @Override
    public short getShort(int column) {
        return mCurrentPlaylistCursor.getShort(column);
    }

    @Override
    public int getInt(int column) {
        try {
            return mCurrentPlaylistCursor.getInt(column);
        } catch (Exception ex) {
            onChange(true);
            return 0;
        }
    }

    @Override
    public long getLong(int column) {
        try {
            return mCurrentPlaylistCursor.getLong(column);
        } catch (Exception ex) {
            onChange(true);
            return 0;
        }
    }

    @Override
    public float getFloat(int column) {
        return mCurrentPlaylistCursor.getFloat(column);
    }

    @Override
    public double getDouble(int column) {
        return mCurrentPlaylistCursor.getDouble(column);
    }

    @Override
    public int getType(int column) {
        return mCurrentPlaylistCursor.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return mCurrentPlaylistCursor.isNull(column);
    }

    @Override
    public String[] getColumnNames() {
        return mProjection;
    }

    @Override
	@SuppressWarnings("deprecation")
    public void deactivate() {
        if (mCurrentPlaylistCursor != null)
            mCurrentPlaylistCursor.deactivate();
    }

    @Override
    public boolean requery() {
        makeNowPlayingCursor();
        return true;
    }

    private final String[] mProjection;

    private Cursor mCurrentPlaylistCursor;

    private int mSize;

    private long[] mNowPlaying;

    private long[] mCursorIdxs;

    private final Context context;

    private final IApolloService mService;
}
