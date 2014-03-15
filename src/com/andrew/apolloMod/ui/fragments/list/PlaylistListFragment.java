
package com.andrew.apolloMod.ui.fragments.list;

import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.MediaColumns;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.list.PlaylistListAdapter;
import com.andrew.apolloMod.ui.fragments.base.DragSortListViewFragment;
import static com.andrew.apolloMod.Constants.EXTERNAL;
import static com.andrew.apolloMod.Constants.PLAYLIST_FAVORITES;
import static com.andrew.apolloMod.Constants.TYPE_PLAYLIST;

public class PlaylistListFragment extends DragSortListViewFragment{
    // Playlist ID
    private long mPlaylistId = -1;
    
    public PlaylistListFragment(Bundle args) {
        setArguments(args);
        mPlaylistId = getArguments().getLong(BaseColumns._ID);
    }

	@Override
	public void setupFragmentData() {
		 mAdapter = new PlaylistListAdapter(getActivity(), R.layout.dragsort_listview_items, null,
	                new String[] {}, new int[] {}, 0, mPlaylistId);

	     StringBuilder where = new StringBuilder();
	     where.append(AudioColumns.IS_MUSIC + "=1").append(" AND " + MediaColumns.TITLE + " != ''");
	     mWhere = where.toString(); 
	     mSortOrder = Playlists.Members.PLAY_ORDER;
         if(mPlaylistId == PLAYLIST_FAVORITES){
             long favorites_id = MusicUtils.getFavoritesId(getActivity());
        	 mProjection = new String[] {
                     Playlists.Members._ID, Playlists.Members.AUDIO_ID,
                     MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
             };
        	 mUri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
         }
         else{
        	 mProjection = new String[] {
                     Playlists.Members._ID, Playlists.Members.AUDIO_ID,
                     MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
             };
        	 mUri = Playlists.Members.getContentUri(EXTERNAL, mPlaylistId);        	 
         }
         mMediaIdColumn = Playlists.Members.AUDIO_ID;
         mType = TYPE_PLAYLIST;
         mFragmentGroupId = 90;
	}

    /**
     * @param which
     */
    public void removePlaylistItem(int which) {
        mCursor.moveToPosition(which);
        long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(Playlists.Members.AUDIO_ID));
        if (mPlaylistId >= 0) {
            Uri uri = Playlists.Members.getContentUri(EXTERNAL, mPlaylistId);
            getActivity().getContentResolver().delete(uri, Playlists.Members.AUDIO_ID + "=" + id,
                    null);
        } else if (mPlaylistId == PLAYLIST_FAVORITES) {
            MusicUtils.removeFromFavorites(getActivity(), id);
        }
        mListView.invalidateViews();
    }
}
