
package com.andrew.apolloMod.ui.adapters.list;

import android.content.Context;
import android.database.Cursor;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;
import com.andrew.apolloMod.ui.fragments.list.GenresFragment;

public class GenreAdapter extends ListViewAdapter {

    public GenreAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setupViewData( Cursor mCursor ){
        String genreName = mCursor.getString(GenresFragment.mGenreNameIndex);
        mLineOneText = MusicUtils.parseGenreName( mContext , genreName );
    }
}
