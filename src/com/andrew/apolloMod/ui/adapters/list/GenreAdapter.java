
package com.andrew.apolloMod.ui.adapters.list;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio;

import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.base.ListViewAdapter;

public class GenreAdapter extends ListViewAdapter {
    public GenreAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }
    public void setupViewData( Cursor mCursor ){
        String genreName = mCursor.getString(mCursor.getColumnIndexOrThrow(Audio.Genres.NAME));
        mLineOneText = MusicUtils.parseGenreName( mContext , genreName );
    }
}
