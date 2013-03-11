/**
 * 
 */

package com.andrew.apolloMod.ui.fragments.grid;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
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
import android.widget.GridView;
import android.widget.LinearLayout;

import com.andrew.apolloMod.NowPlayingCursor;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.AddIdCursorLoader;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.QuickQueueAdapter;

/**
 * @author Andrew Neal
 */
public class QuickQueueFragment extends Fragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    private QuickQueueAdapter mQuickQueueAdapter;

    // GridView
    private GridView mGridView;

    // Cursor
    private Cursor mCursor;

    // Selected position
    private int mSelectedPosition;

    // Options
    private final int PLAY_SELECTION = 0;

    private final int REMOVE = 1;

    // Audio columns
    public static int mTitleIndex, mAlbumIndex, mArtistIndex, mMediaIdIndex, mAlbumIdIndex;

    // Bundle
    public QuickQueueFragment() {
    }

    public QuickQueueFragment(Bundle args) {
        setArguments(args);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Adapter
        mQuickQueueAdapter = new QuickQueueAdapter(getActivity(), R.layout.quick_queue_items, null,
                new String[] {}, new int[] {}, 0);
        mGridView.setOnCreateContextMenuListener(this);
        mGridView.setOnItemClickListener(this);
        mGridView.setAdapter(mQuickQueueAdapter);

        // Important!
        getLoaderManager().initLoader(0, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.quick_queue, container, false);
        mGridView = (GridView)root.findViewById(R.id.gridview);
        mGridView.setNumColumns(1);

        LinearLayout mQueueHolder = (LinearLayout)root.findViewById(R.id.quick_queue_holder);
        mQueueHolder.setBackgroundColor(getResources().getColor(R.color.transparent_black));
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	String[] projection = new String[] {
                BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST,
        };
        StringBuilder selection = new StringBuilder();
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = Audio.Media.DEFAULT_SORT_ORDER;
        uri = Audio.Media.EXTERNAL_CONTENT_URI;
        long[] mNowPlaying = MusicUtils.getQueue();
        if (mNowPlaying.length == 0)
            return null;
        selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        if (mNowPlaying == null || mNowPlaying.length <= 0)
            return null;
        for (long queue_id : mNowPlaying) {
            selection.append(queue_id + ",");
        }
        selection.deleteCharAt(selection.length() - 1);
        selection.append(")");
    	
        return new AddIdCursorLoader(getActivity(), uri, projection, selection.toString(), null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Check for database errors
        if (data == null) {
            return;
        }

        mMediaIdIndex = data.getColumnIndexOrThrow(BaseColumns._ID);
        mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
        mArtistIndex = data.getColumnIndexOrThrow(AudioColumns.ARTIST);
        mAlbumIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM);
        mAlbumIdIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM_ID);
        mQuickQueueAdapter.changeCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mQuickQueueAdapter != null)
            mQuickQueueAdapter.changeCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, PLAY_SELECTION, 0, getResources().getString(R.string.play_all));
        menu.add(0, REMOVE, 0, getResources().getString(R.string.remove));

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo)menuInfo;
        mSelectedPosition = mi.position;
        mCursor.moveToPosition(mSelectedPosition);

        String title = mCursor.getString(mTitleIndex);
        menu.setHeaderTitle(title);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION:
                int position = mSelectedPosition;
                MusicUtils.playAll(getActivity(), mCursor, position);
                getActivity().finish();
                break;
            case REMOVE:
                removePlaylistItem(mSelectedPosition);
                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (mCursor instanceof NowPlayingCursor) {
            if (MusicUtils.mService != null) {
                MusicUtils.setQueuePosition(position);
            }
        }
        MusicUtils.playAll(getActivity(), mCursor, position);
        getActivity().finish();
    }

    /**
     * @param which
     */
    private void removePlaylistItem(int which) {
        mCursor.moveToPosition(which);
        long id = mCursor.getLong(mMediaIdIndex);
        MusicUtils.removeTrack(id);
        reloadQueueCursor();
        mGridView.invalidateViews();
    }

    /**
     * Reload the queue after we remove a track
     */
    private void reloadQueueCursor() {
        String[] projection = new String[] {
                BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST,
        };
        StringBuilder selection = new StringBuilder();
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = Audio.Media.DEFAULT_SORT_ORDER;
        uri = Audio.Media.EXTERNAL_CONTENT_URI;
        long[] mNowPlaying = MusicUtils.getQueue();
        if (mNowPlaying.length == 0)
            return;
        selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        if (mNowPlaying == null || mNowPlaying.length <= 0)
            return;
        for (long queue_id : mNowPlaying) {
            selection.append(queue_id + ",");
        }
        selection.deleteCharAt(selection.length() - 1);
        selection.append(")");

        mCursor = MusicUtils.query(getActivity(), uri, projection, selection.toString(), null,
                sortOrder);
        mQuickQueueAdapter.changeCursor(mCursor);
    }

    /**
     * Update the list as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGridView != null) {
                mQuickQueueAdapter.notifyDataSetChanged();
                // Scroll to the currently playing track in the queue
                mGridView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGridView.setSelection(MusicUtils.getQueuePosition());
                    }
                }, 100);
            }
        }

    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        filter.addAction(ApolloService.QUEUE_CHANGED);
        getActivity().registerReceiver(mMediaStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }
}
