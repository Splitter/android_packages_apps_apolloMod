
package com.andrew.apolloMod.ui.fragments.list;

import android.support.v4.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.MediaColumns;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.list.NowPlayingAdapter;
import com.andrew.apolloMod.ui.fragments.base.DragSortListViewFragment;
import static com.andrew.apolloMod.Constants.TYPE_SONG;

public class NowPlayingFragment extends DragSortListViewFragment{
	
	@Override
	public void setupFragmentData() {
		mAdapter = new NowPlayingAdapter(getActivity(), R.layout.dragsort_listview_items, null,
		        							new String[] {}, new int[] {}, 0);
		mProjection = new String[] {
		            BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
		    };
		mSortOrder = Audio.Media.DEFAULT_SORT_ORDER;
		mUri = Audio.Media.EXTERNAL_CONTENT_URI;
		StringBuilder where = new StringBuilder();
		long[] mNowPlaying = MusicUtils.getQueue();
		if (mNowPlaying.length <= 0){
			where.append(AudioColumns.IS_MUSIC + "=1").append(" AND " + MediaColumns.TITLE + " != ''");
		}
		else{
			where.append(BaseColumns._ID + " IN (");
			for (long queue_id : mNowPlaying) {
			    where.append(queue_id + ",");
			}
			where.deleteCharAt(where.length() - 1);
			where.append(")");			
		}
		mWhere = where.toString();
        mMediaIdColumn = BaseColumns._ID;
        mType = TYPE_SONG;
        mFragmentGroupId = 91;
	}

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null) {
            return;
        }
        long[] mNowPlaying = MusicUtils.getQueue();
    	String[] audioCols = new String[] { BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ARTIST, AudioColumns.ALBUM};
        MatrixCursor playlistCursor = new MatrixCursor(audioCols);
    	for(int i = 0; i < mNowPlaying.length; i++){
    		data.moveToPosition(-1);
    		while (data.moveToNext()) {
                long audioid = data.getLong(data.getColumnIndexOrThrow(BaseColumns._ID));
            	if( audioid == mNowPlaying[i]) {
                    String trackName = data.getString(data.getColumnIndexOrThrow(MediaColumns.TITLE));
                    String artistName = data.getString(data.getColumnIndexOrThrow(AudioColumns.ARTIST));
                    String albumName = data.getString(data.getColumnIndexOrThrow(AudioColumns.ALBUM));
            		playlistCursor.addRow(new Object[] {audioid, trackName, artistName, albumName });
            	}
            }
    	}
        data.close();
		mCursor = playlistCursor;
        super.onLoadFinished(loader, playlistCursor);
    }

    /**
     * @param which
     */
	@Override
    public void removePlaylistItem(int which) {
        mCursor.moveToPosition(which);
        long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
        MusicUtils.removeTrack(id);
        reloadQueueCursor();
        mListView.invalidateViews();
    }

    /**
     * Reload the queue after we remove a track
     */
    private void reloadQueueCursor() {
        String[] cols = new String[] {
                BaseColumns._ID, MediaColumns.TITLE, MediaColumns.DATA, AudioColumns.ALBUM,
                AudioColumns.ARTIST, AudioColumns.ARTIST_ID
        };
        StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + MediaColumns.TITLE + " != ''");
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        long[] mNowPlaying = MusicUtils.getQueue();
        if (mNowPlaying.length == 0) {
        }
        selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < mNowPlaying.length; i++) {
            selection.append(mNowPlaying[i]);
            if (i < mNowPlaying.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
		if(mCursor != null)
			mCursor.close();
        mCursor = MusicUtils.query(getActivity(), uri, cols, selection.toString(), null, null);
        String[] audioCols = new String[] { BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ARTIST, AudioColumns.ALBUM}; 
        MatrixCursor playlistCursor = new MatrixCursor(audioCols);
    	for(int i = 0; i < mNowPlaying.length; i++){
    		mCursor.moveToPosition(-1);
    		while (mCursor.moveToNext()) {
                long audioid = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
            	if( audioid == mNowPlaying[i]) {
                    String trackName = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
                    String artistName = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
                    String albumName = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM));
            		playlistCursor.addRow(new Object[] {audioid, trackName, artistName ,albumName});

            	}
            }
    	}
		if(mCursor != null)
			mCursor.close();
        mCursor = playlistCursor;
        mAdapter.changeCursor(playlistCursor);
    }
}
