
package com.andrew.apolloMod.ui.adapters.grid;

import static com.andrew.apolloMod.Constants.TYPE_ARTIST;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.GridViewAdapter;
import com.andrew.apolloMod.ui.fragments.grid.ArtistsFragment;

public class ArtistAdapter extends GridViewAdapter {

    public ArtistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData(Cursor mCursor){

    	mLineOneText = mCursor.getString(ArtistsFragment.mArtistNameIndex);
        int albums_plural = mCursor.getInt(ArtistsFragment.mArtistNumAlbumsIndex);
        boolean unknown = mLineOneText == null || mLineOneText.equals(MediaStore.UNKNOWN_STRING);
        mLineTwoText = MusicUtils.makeAlbumsLabel(mContext, albums_plural, 0, unknown);        
        mGridType = TYPE_ARTIST;        
        mImageData = new String[]{mLineOneText};
        mPlayingId = MusicUtils.getCurrentArtistId();
        mCurrentId = mCursor.getLong(ArtistsFragment.mArtistIdIndex);
        
    }
}
