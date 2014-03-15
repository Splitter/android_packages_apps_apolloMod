
package com.andrew.apolloMod.ui.adapters.list;

import static com.andrew.apolloMod.Constants.EXTERNAL;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;
import android.widget.Toast;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.DragSortListViewAdapter;

public class PlaylistListAdapter extends DragSortListViewAdapter {
   
    private long mPlaylistId;

    public PlaylistListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, long id) {
        super(context, layout, c, from, to, flags);
        mPlaylistId = id;
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
    	
        mLineTwoText = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
    	
        mImageData = new String[]{ mLineTwoText };
        
        mPlayingId = MusicUtils.getCurrentAudioId();
        mCurrentId = mCursor.getLong(mCursor.getColumnIndexOrThrow(Playlists.Members.AUDIO_ID));
    }
   
    @Override
    public void drop(int from, int to) {
    	super.drop(from, to);
        if (from != to && mPlaylistId >= 0) {
        	try{
                Playlists.Members.moveItem(mContext.getContentResolver(),mPlaylistId, from, to);
            }catch(Exception e){
                Log.e("FAILED", e.getMessage());
            }
        }
    }
    
    @Override
    public void remove(int which) {
        int cursorPos = getCursorPosition(which);
        mCursor.moveToPosition(cursorPos);
        long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(Playlists.Members.AUDIO_ID));
        String mName = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
        if (mPlaylistId >= 0) {
            Uri uri = Playlists.Members.getContentUri(EXTERNAL, mPlaylistId);
            mContext.getContentResolver().delete(uri, Playlists.Members.AUDIO_ID + "=" + id,
                    null);
            String message = mContext.getString(R.string.track_removed_from_playlist, mName);
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    	super.remove(which);
    }
}
