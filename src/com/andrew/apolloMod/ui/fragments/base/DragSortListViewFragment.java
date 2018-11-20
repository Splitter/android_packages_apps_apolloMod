/**
 * 
 */

package com.andrew.apolloMod.ui.fragments.base;

import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.base.DragSortListViewAdapter;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import static com.andrew.apolloMod.Constants.INTENT_ADD_TO_PLAYLIST;
import static com.andrew.apolloMod.Constants.INTENT_PLAYLIST_LIST;


public abstract class DragSortListViewFragment extends RefreshableFragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    protected DragSortListViewAdapter mAdapter;

    // ListView
    protected DragSortListView mListView;

    // Cursor
    protected Cursor mCursor;

    // Selected position
    protected int mSelectedPosition;

    // Used to set ringtone
    protected long mSelectedId;

    // Options
    protected final int PLAY_SELECTION = 0;

    protected final int USE_AS_RINGTONE = 1;

    protected final int ADD_TO_PLAYLIST = 2;

    protected final int SEARCH = 3;

    protected final int REMOVE = 4;
    
    protected int mFragmentGroupId = 0;

    protected String mSortOrder = null, mWhere = null,
    				 mType = null, mMediaIdColumn = null;
    
    protected String[] mProjection = null;
    
    protected Uri mUri = null;

    // Bundle
    public DragSortListViewFragment() {
    }

    public DragSortListViewFragment(Bundle args) {
        setArguments(args);
    }

    /*
     * To be overrode in child classes to setup fragment data
     */
    public abstract void setupFragmentData();
    /*
     * To be overrode in child classes to remove item from list
     */
    public abstract void removePlaylistItem(int which);
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupFragmentData();
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);
        DragSortController controller = new DragSortController(mListView);
        controller.setDragHandleId(R.id.listview_drag_handle);
        controller.setRemoveEnabled(true);
        controller.setRemoveMode(1);
        mListView.setFloatViewManager(controller);
        mListView.setOnTouchListener(controller);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void refresh() {
        if( mListView != null ) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dragsort_listview, container, false);
        mListView = (DragSortListView)root.findViewById(R.id.list_view);
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	return new CursorLoader(getActivity(), mUri, mProjection, mWhere, null, mSortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Check for database errors
        if (data == null) {
            return;
        }
        mAdapter.reset();
        mAdapter.changeCursor(data);
        mListView.invalidateViews();
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.changeCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if( mFragmentGroupId != 0 ){
	    	menu.add(mFragmentGroupId, PLAY_SELECTION, 0, getResources().getString(R.string.play_all));
	        menu.add(mFragmentGroupId, ADD_TO_PLAYLIST, 0, getResources().getString(R.string.add_to_playlist));
	        menu.add(mFragmentGroupId, USE_AS_RINGTONE, 0, getResources().getString(R.string.use_as_ringtone));
	        menu.add(mFragmentGroupId, REMOVE, 0, R.string.remove);
	        menu.add(mFragmentGroupId, SEARCH, 0, getResources().getString(R.string.search));
	        AdapterContextMenuInfo mi = (AdapterContextMenuInfo)menuInfo;
	        mSelectedPosition = mi.position;
	        mCursor.moveToPosition(mSelectedPosition);
	        try {
	            mSelectedId = mCursor.getLong(mCursor.getColumnIndexOrThrow(mMediaIdColumn));
	        } catch (IllegalArgumentException ex) {
	            mSelectedId = mi.id;
	        }
	        String title = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
	        menu.setHeaderTitle(title);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	if( item.getGroupId() == mFragmentGroupId ){
	        switch (item.getItemId()) {
	            case PLAY_SELECTION:
	                int position = mSelectedPosition;
	                MusicUtils.playAll(getActivity(), mCursor, position);
	                break;
	            case ADD_TO_PLAYLIST: {
	                Intent intent = new Intent(INTENT_ADD_TO_PLAYLIST);
	                long[] list = new long[] {
	                    mSelectedId
	                };
	                intent.putExtra(INTENT_PLAYLIST_LIST, list);
	                getActivity().startActivity(intent);
	                break;
	            }
	            case USE_AS_RINGTONE:
	                MusicUtils.setRingtone(getActivity(), mSelectedId);
	                break;
	            case SEARCH: {
	                MusicUtils.doSearch(getActivity(), mCursor, mType);
	                break;
	            }
	            case REMOVE: {
	                removePlaylistItem(mSelectedPosition);
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
        MusicUtils.playAll(getActivity(), mCursor, position);
    }

    /**
     * Update the list as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mListView != null) {
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
