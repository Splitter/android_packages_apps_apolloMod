
package com.andrew.apolloMod.ui.adapters.list;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apolloMod.helpers.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.GridViewAdapter;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import com.andrew.apolloMod.providers.HistoryStore.RecentColumns;

public class RecentlyPlayedAdapter extends GridViewAdapter {

    public RecentlyPlayedAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(mCursor.getColumnIndexOrThrow(RecentColumns.TITLE));

    	mLineTwoText = mCursor.getString(mCursor.getColumnIndexOrThrow(RecentColumns.ARTIST));

        String albumName = mCursor.getString(mCursor.getColumnIndexOrThrow(RecentColumns.ALBUM));
        String albumId = mCursor.getString(mCursor.getColumnIndexOrThrow(RecentColumns.ALBUM_ID));
        mImageData = new String[]{ albumId , mLineTwoText, albumName };
        
        mPlayingId = MusicUtils.getCurrentAudioId();
        mCurrentId = mCursor.getLong(mCursor.getColumnIndexOrThrow(RecentColumns._ID));
        mGridType = TYPE_ALBUM;
    }
}
