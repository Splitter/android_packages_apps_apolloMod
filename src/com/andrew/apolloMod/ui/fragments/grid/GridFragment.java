package com.andrew.apolloMod.ui.fragments.grid;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;

public class GridFragment extends Fragment implements LoaderCallbacks<Cursor>,
OnItemClickListener {

    private SimpleCursorAdapter mAdapter;

    // GridView
    private GridView mGridView;

    // Cursor
    private Cursor mCursor;

    // Options
    private final int PLAY_SELECTION = 0;

    private final int ADD_TO_PLAYLIST = 1;

    private final int SEARCH = 2;
    
    public int GRIDVIEW_FRAGMENT_ID = 1;

    // Artist ID
    private String mCurrentArtistId;

    // Album ID
    private String mCurrentAlbumId;

    // Audio columns
    public static int mArtistIdIndex, mArtistNameIndex, mArtistNumAlbumsIndex;

    public GridFragment() {
    }

    public GridFragment(Bundle bundle) {
        setArguments(bundle);
    }
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}

}
