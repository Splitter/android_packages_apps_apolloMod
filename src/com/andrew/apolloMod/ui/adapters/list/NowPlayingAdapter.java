
package com.andrew.apolloMod.ui.adapters.list;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.widget.Toast;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.DragSortListViewAdapter;

public class NowPlayingAdapter extends DragSortListViewAdapter {

    public NowPlayingAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
    	
        mLineTwoText = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
    	
        mImageData = new String[]{ mLineTwoText };
        
        mPlayingId = MusicUtils.getCurrentAudioId();
        mCurrentId = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
    }
    
    @Override
    public void drop(int from, int to) {
    	super.drop(from, to);
        MusicUtils.moveQueueItem(from, to);
    }
    
    @Override
    public void remove(int which) {
        int cursorPos = getCursorPosition(which);
        mCursor.moveToPosition(cursorPos);
        long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
        String mName = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
        MusicUtils.removeTrack(id);
        String message = mContext.getString(R.string.track_removed_from_playlist, mName);
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    	super.remove(which);
    }
}
