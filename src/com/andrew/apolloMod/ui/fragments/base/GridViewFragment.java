package com.andrew.apolloMod.ui.fragments.base;

import static com.andrew.apolloMod.Constants.INTENT_ADD_TO_PLAYLIST;
import static com.andrew.apolloMod.Constants.INTENT_PLAYLIST_LIST;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.base.GridViewAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;

public abstract class GridViewFragment extends Fragment implements LoaderCallbacks<Cursor>,
OnItemClickListener {

	private GridView mGridView;
	
	protected GridViewAdapter mAdapter;

    protected Cursor mCursor;

    private final int PLAY_SELECTION = 0;

    private final int ADD_TO_PLAYLIST = 1;

    private final int SEARCH = 2;
    
    protected int mFragmentGroupId = 0;

    protected String mCurrentId, mSortOrder = null, mType = null;
    
    protected String[] mProjection = null;
    
    protected Uri mUri = null;
    
    public GridViewFragment() {
    }

    public GridViewFragment(Bundle bundle) {
        setArguments(bundle);
    }

    /*
     * To be overrode in child classes to setup fragment data
     */
    public abstract void setupFragmentData();
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupFragmentData();
        mGridView.setOnCreateContextMenuListener(this);
        mGridView.setOnItemClickListener(this);
        mGridView.setAdapter(mAdapter);
        mGridView.setTextFilterEnabled(true);
        getLoaderManager().initLoader(0, null, this);
    }    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.gridview, container, false);
        mGridView = (GridView)root.findViewById(R.id.gridview);
        return root;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(mFragmentGroupId, PLAY_SELECTION, 0, getResources().getString(R.string.play_all));
        menu.add(mFragmentGroupId, ADD_TO_PLAYLIST, 0, getResources().getString(R.string.add_to_playlist));
        menu.add(mFragmentGroupId, SEARCH, 0, getResources().getString(R.string.search));

        mCurrentId = mCursor.getString(mCursor.getColumnIndexOrThrow(BaseColumns._ID));

        menu.setHeaderView(ApolloUtils.setHeaderLayout(mType, mCursor, getActivity()));
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	if(item.getGroupId() == mFragmentGroupId){
	        switch (item.getItemId()) {
	            case PLAY_SELECTION: {
	                long[] list = MusicUtils.getSongList(mType, getActivity(), Long.parseLong(mCurrentId));
	                MusicUtils.playAll(getActivity(), list, 0);
	                break;
	            }
	            case ADD_TO_PLAYLIST: {
	                Intent intent = new Intent(INTENT_ADD_TO_PLAYLIST);
	                long[] list = MusicUtils.getSongList(mType, getActivity(), Long.parseLong(mCurrentId));
	                intent.putExtra(INTENT_PLAYLIST_LIST, list);
	                getActivity().startActivity(intent);
	                break;
	            }
	            case SEARCH: {
	                MusicUtils.doSearch(getActivity(), mCursor, mType);
	                break;
	            }
	            default:
	                break;
	        }
    	}
        return super.onContextItemSelected(item);
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        ApolloUtils.startTracksBrowser(mType, id, mCursor, getActivity());
    }

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
	     return new CursorLoader(getActivity(), mUri, mProjection, null, null, mSortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Check for database errors
        if (data == null) {
            return;
        }
        if(mCursor!=null)
        	mCursor.close();
        mAdapter.changeCursor(data);
        mCursor = data;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
        if (mAdapter != null)
            mAdapter.changeCursor(null);		
	}
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
        super.onSaveInstanceState(outState);
    }
    
    /**
     * Update the list as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGridView != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        filter.addAction(ApolloService.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(mMediaStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }

}
