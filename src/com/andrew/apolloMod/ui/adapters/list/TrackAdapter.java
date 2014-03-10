
package com.andrew.apolloMod.ui.adapters.list;

import static com.andrew.apolloMod.Constants.TYPE_ARTIST;
import android.content.Context;
import android.database.Cursor;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;
import com.andrew.apolloMod.ui.fragments.list.TracksFragment;

public class TrackAdapter extends ListViewAdapter {


    public TrackAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(TracksFragment.mTitleIndex);
    	
    	mLineTwoText = mCursor.getString(TracksFragment.mArtistIndex);
    	
        mImageData = new String[]{ mLineTwoText };
        
        mPlayingId = MusicUtils.getCurrentAudioId();
        mCurrentId = mCursor.getLong(TracksFragment.mMediaIdIndex);

        mListType = TYPE_ARTIST;   	
    }
}
