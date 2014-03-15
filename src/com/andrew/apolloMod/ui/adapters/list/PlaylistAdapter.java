
package com.andrew.apolloMod.ui.adapters.list;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;

public class PlaylistAdapter extends ListViewAdapter {    
    
    public PlaylistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
    	mLineOneText = mCursor.getString(mCursor.getColumnIndexOrThrow(PlaylistsColumns.NAME));
    }    
}
