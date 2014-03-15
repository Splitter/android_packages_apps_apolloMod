
package com.andrew.apolloMod.ui.adapters.list;

import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio.AlbumColumns;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;

public class ArtistAlbumAdapter extends ListViewAdapter {

    public ArtistAlbumAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(mCursor.getColumnIndexOrThrow(AlbumColumns.ALBUM));    	
        int songs_plural = mCursor.getInt(mCursor.getColumnIndexOrThrow(AlbumColumns.NUMBER_OF_SONGS));
    	mLineTwoText =MusicUtils.makeAlbumsLabel(mContext, 0, songs_plural, true );    	
        String artistName = mCursor.getString(mCursor.getColumnIndexOrThrow(AlbumColumns.ARTIST));        
        String albumId = mCursor.getString(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
        mImageData = new String[]{ albumId , artistName, mLineOneText };        
        mPlayingId = MusicUtils.getCurrentAlbumId();
        mCurrentId = Long.parseLong(albumId);        
        mListType = TYPE_ALBUM;   	
    }
}
