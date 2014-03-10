
package com.andrew.apolloMod.ui.adapters.grid;

import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import android.content.Context;
import android.database.Cursor;

import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.GridViewAdapter;
import com.andrew.apolloMod.ui.fragments.grid.AlbumsFragment;

public class AlbumAdapter extends GridViewAdapter {

    public AlbumAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData(Cursor mCursor){

    	mLineOneText = mCursor.getString(AlbumsFragment.mAlbumNameIndex);
        mLineTwoText = mCursor.getString(AlbumsFragment.mArtistNameIndex);     
        mGridType = TYPE_ALBUM;        
        mImageData =  new String[]{ mCursor.getString(AlbumsFragment.mAlbumIdIndex) , mLineTwoText, mLineOneText };
        mPlayingId = MusicUtils.getCurrentAlbumId();
        mCurrentId = mCursor.getLong(AlbumsFragment.mAlbumIdIndex);
        
    }
}
