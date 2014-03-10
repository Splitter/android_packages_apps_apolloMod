
package com.andrew.apolloMod.ui.adapters.list;

import static com.andrew.apolloMod.Constants.TYPE_ARTIST;
import android.content.Context;
import android.database.Cursor;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;
import com.andrew.apolloMod.ui.fragments.list.SongsFragment;

public class SonglistAdapter extends ListViewAdapter {

    public SonglistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(SongsFragment.mTitleIndex);
    	
    	mLineTwoText = mCursor.getString(SongsFragment.mArtistIndex);
    	
        mImageData = new String[]{ mLineTwoText };
        
        mPlayingId = MusicUtils.getCurrentAudioId();
        mCurrentId = mCursor.getLong(SongsFragment.mMediaIdIndex);
        
        mListType = TYPE_ARTIST;   	
    }

}
