
package com.andrew.apolloMod.ui.fragments.list;

import android.view.View;
import android.widget.AdapterView;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.MusicUtils;
import com.andrew.apolloMod.ui.adapters.list.RecentlyPlayedAdapter;
import com.andrew.apolloMod.ui.fragments.base.GridViewFragment;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;

import com.andrew.apolloMod.providers.HistoryStore;
import com.andrew.apolloMod.providers.HistoryStore.RecentColumns;

public class RecentlyPlayedFragment extends GridViewFragment{
	
    public void setupFragmentData(){
        mAdapter = new RecentlyPlayedAdapter(getActivity(), R.layout.gridview_items_recent,
                null, new String[] {}, new int[] {}, 0);
    	mProjection = new String[] {
    			"DISTINCT "+RecentColumns._ID, RecentColumns.TIME_PLAYED,
                RecentColumns.TITLE, RecentColumns.ARTIST,
                RecentColumns.ALBUM, RecentColumns.ALBUM_ID       
        };        
        mSortOrder = RecentColumns.TIME_PLAYED + " DESC";
        mUri = HistoryStore.CONTENT_URI;
        mType = TYPE_ALBUM;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        MusicUtils.playAll(getActivity(), mCursor, position);
    }
}
