
package com.andrew.apolloMod.ui.adapters.list;

import android.content.Context;
import android.database.Cursor;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;
import com.andrew.apolloMod.ui.fragments.list.PlaylistsFragment;

public class PlaylistAdapter extends ListViewAdapter {    
    
    public PlaylistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(PlaylistsFragment.mPlaylistNameIndex);
    }    
}
