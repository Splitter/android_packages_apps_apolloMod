
package com.andrew.apolloMod.ui.adapters.list;

import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import android.content.Context;
import android.database.Cursor;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;
import com.andrew.apolloMod.ui.fragments.list.RecentlyAddedFragment;

public class RecentlyAddedAdapter extends ListViewAdapter {

    public RecentlyAddedAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(RecentlyAddedFragment.mTitleIndex);

    	mLineTwoText = mCursor.getString(RecentlyAddedFragment.mArtistIndex);

        String albumName = mCursor.getString(RecentlyAddedFragment.mAlbumIndex);
        String albumId = mCursor.getString(RecentlyAddedFragment.mAlbumIdIndex);
        mImageData = new String[]{ albumId , mLineTwoText, albumName };
        
        mPlayingId = MusicUtils.getCurrentAudioId();
        mCurrentId = mCursor.getLong(RecentlyAddedFragment.mMediaIdIndex);

        mListType = TYPE_ALBUM;
    	showContextEnabled = false;    	
    }
}
