
package com.andrew.apolloMod.ui.adapters.list;

import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import android.content.Context;
import android.database.Cursor;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;
import com.andrew.apolloMod.ui.fragments.list.ArtistAlbumsFragment;

public class ArtistAlbumAdapter extends ListViewAdapter {

    public ArtistAlbumAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(ArtistAlbumsFragment.mAlbumNameIndex);
    	
        int songs_plural = mCursor.getInt(ArtistAlbumsFragment.mSongCountIndex);
    	mLineTwoText =MusicUtils.makeAlbumsLabel(mContext, 0, songs_plural, true );
    	
        String artistName = mCursor.getString(ArtistAlbumsFragment.mArtistNameIndex);        
        String albumId = mCursor.getString(ArtistAlbumsFragment.mAlbumIdIndex);
        mImageData = new String[]{ albumId , artistName, mLineOneText };
        
        mPlayingId = MusicUtils.getCurrentAlbumId();
        mCurrentId = mCursor.getLong(ArtistAlbumsFragment.mAlbumIdIndex);
        
        mListType = TYPE_ALBUM;   	
    }
}
