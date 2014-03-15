
package com.andrew.apolloMod.ui.fragments.list;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.list.PlaylistAdapter;
import com.andrew.apolloMod.ui.fragments.base.ListViewFragment;
import static com.andrew.apolloMod.Constants.INTENT_KEY_RENAME;
import static com.andrew.apolloMod.Constants.INTENT_RENAME_PLAYLIST;
import static com.andrew.apolloMod.Constants.TYPE_PLAYLIST;


public class PlaylistsFragment extends ListViewFragment{
	
    private static final int DELETE_PLAYLIST = 12;

    private static final int RENAME_PLAYLIST = 13;
    
    public void setupFragmentData(){
        mAdapter = new PlaylistAdapter(getActivity(), R.layout.listview_items, null,
                										new String[] {}, new int[] {}, 0);
    	mProjection = new String[] {
                BaseColumns._ID, PlaylistsColumns.NAME
        };
        mSortOrder = Audio.Playlists.DEFAULT_SORT_ORDER;
        mUri = Audio.Playlists.EXTERNAL_CONTENT_URI;
        mFragmentGroupId = 5;
        mType = TYPE_PLAYLIST;
        mTitleColumn = PlaylistsColumns.NAME;         
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo)menuInfo;
        menu.add(mFragmentGroupId, PLAY_SELECTION, 0, getResources().getString(R.string.play_all));
        if (mi.id >= 0) {
            menu.add(mFragmentGroupId, RENAME_PLAYLIST, 0, getResources().getString(R.string.rename_playlist));
            menu.add(mFragmentGroupId, DELETE_PLAYLIST, 0, getResources().getString(R.string.delete_playlist));
        }
        mCurrentId = mCursor.getString(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
        String title = mCursor.getString(mCursor.getColumnIndexOrThrow(PlaylistsColumns.NAME));
        menu.setHeaderTitle(title);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	if(item.getGroupId() == mFragmentGroupId){
	        AdapterContextMenuInfo mi = (AdapterContextMenuInfo)item.getMenuInfo();
	        switch (item.getItemId()) {
	            case PLAY_SELECTION: {
	                long[] list = MusicUtils.getSongListForPlaylist(getActivity(),
	                        Long.parseLong(mCurrentId));
	                MusicUtils.playAll(getActivity(), list, 0);
	                break;
	            }
	            case RENAME_PLAYLIST: {
	                Intent intent = new Intent(INTENT_RENAME_PLAYLIST);
	                intent.putExtra(INTENT_KEY_RENAME, mi.id);
	                getActivity().startActivity(intent);
	                break;
	            }
	            case DELETE_PLAYLIST: {
	                Uri uri = ContentUris.withAppendedId(
	                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mi.id);
	                getActivity().getContentResolver().delete(uri, null, null);
	                break;
	            }
	            default:
	                break;
	        }
			return true;
		}
        return super.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
    	ApolloUtils.startTracksBrowser(mType, id, mCursor, getActivity());
    }
}
