
package com.andrew.apolloMod.ui.adapters.list;

import static com.andrew.apolloMod.Constants.TYPE_ARTIST;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;

import com.andrew.apolloMod.helpers.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;

public class AlbumListAdapter extends ListViewAdapter {

	public AlbumListAdapter(Context context, int layout, Cursor c,	String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
	}

	@Override
	public void setupViewData(Cursor mCursor) {
    	mLineOneText = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));    	
    	mLineTwoText = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));    	
        mImageData = new String[]{ mLineTwoText };        
        mPlayingId = MusicUtils.getCurrentAudioId();
        mCurrentId = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
        mListType = TYPE_ARTIST;
	}
}
