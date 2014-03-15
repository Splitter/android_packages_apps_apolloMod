
package com.andrew.apolloMod.ui.adapters.grid;

import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio.AlbumColumns;

import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.GridViewAdapter;

public class AlbumAdapter extends GridViewAdapter {

    public AlbumAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData(Cursor mCursor){
        
    	mLineOneText = mCursor.getString(mCursor.getColumnIndexOrThrow(AlbumColumns.ALBUM));
        mLineTwoText = mCursor.getString(mCursor.getColumnIndexOrThrow(AlbumColumns.ARTIST));     
        mGridType = TYPE_ALBUM;        
        mImageData =  new String[]{ mCursor.getString(mCursor.getColumnIndexOrThrow(BaseColumns._ID)) , mLineTwoText, mLineOneText };
        mPlayingId = MusicUtils.getCurrentAlbumId();
        mCurrentId = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
        
    }
}
