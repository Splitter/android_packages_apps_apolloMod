
package com.andrew.apolloMod.ui.fragments.grid;

import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.ArtistColumns;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.ui.adapters.grid.ArtistAdapter;
import com.andrew.apolloMod.ui.fragments.base.GridViewFragment;
import static com.andrew.apolloMod.Constants.TYPE_ARTIST;

public class ArtistsFragment extends GridViewFragment{

    public void setupFragmentData(){
    	mAdapter = new ArtistAdapter(getActivity(), R.layout.gridview_items, null, 
    									new String[] {}, new int[] {}, 0); 
    	mProjection = new String []{
                BaseColumns._ID, ArtistColumns.ARTIST, ArtistColumns.NUMBER_OF_ALBUMS
        };
        mUri = Audio.Artists.EXTERNAL_CONTENT_URI;
        mSortOrder = Audio.Artists.DEFAULT_SORT_ORDER;
        mFragmentGroupId = 1;
        mType = TYPE_ARTIST;
    }
}
