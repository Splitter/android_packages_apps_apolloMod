/**
 * 
 */

package com.andrew.apolloMod.ui.fragments.list;

import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
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

import com.andrew.apolloMod.NowPlayingCursor;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.RefreshableFragment;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.NowPlayingAdapter;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import static com.andrew.apolloMod.Constants.INTENT_ADD_TO_PLAYLIST;
import static com.andrew.apolloMod.Constants.INTENT_PLAYLIST_LIST;

/**
 * @author Andrew Neal
 */
public class NowPlayingFragment extends RefreshableFragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    private NowPlayingAdapter mTrackAdapter;

    // ListView
    private DragSortListView mListView;

    // Cursor
    private Cursor mCursor;

    // Selected position
    private int mSelectedPosition;

    // Used to set ringtone
    private long mSelectedId;

    // Options
    private final int PLAY_SELECTION = 6;

    private final int USE_AS_RINGTONE = 7;

    private final int ADD_TO_PLAYLIST = 8;

    private final int SEARCH = 9;

    private final int REMOVE = 10;

    // Audio columns
    public static int mTitleIndex, mAlbumIndex, mArtistIndex, mMediaIdIndex;

    // Bundle
    public NowPlayingFragment() {
    }

    public NowPlayingFragment(Bundle args) {
        setArguments(args);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Adapter
        mTrackAdapter = new NowPlayingAdapter(getActivity(), R.layout.dragsort_listview_items, null,
                new String[] {}, new int[] {}, 0);
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mTrackAdapter);
        //mListView.setDropListener(mDropListener);

        // Important!
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void refresh() {
        // The data need to be refreshed
        if( mListView != null ) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dragsort_listview, container, false);
        mListView = (DragSortListView)root.findViewById(R.id.list_view);
        DragSortController controller = new DragSortController(mListView);
        controller.setDragHandleId(R.id.listview_drag_handle);
        controller.setRemoveEnabled(true);
        controller.setRemoveMode(1);
        mListView.setFloatViewManager(controller);
        mListView.setOnTouchListener(controller);
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = new String[] {
                BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
        };
        StringBuilder where = new StringBuilder();
        String sortOrder = Audio.Media.DEFAULT_SORT_ORDER;
        where.append(AudioColumns.IS_MUSIC + "=1").append(" AND " + MediaColumns.TITLE + " != ''");
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        where = new StringBuilder();
        where.append(AudioColumns.IS_MUSIC + "=1");
        where.append(" AND " + MediaColumns.TITLE + " != ''");
        uri = Audio.Media.EXTERNAL_CONTENT_URI;
        long[] mNowPlaying = MusicUtils.getQueue();
        if (mNowPlaying.length == 0)
            return null;
        where = new StringBuilder();
        where.append(BaseColumns._ID + " IN (");
        if (mNowPlaying == null || mNowPlaying.length <= 0)
            return null;
        for (long queue_id : mNowPlaying) {
            where.append(queue_id + ",");
        }
        where.deleteCharAt(where.length() - 1);
        where.append(")");
        return new CursorLoader(getActivity(), uri, projection, where.toString(), null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Check for database errors
        if (data == null) {
            return;
        }
        else {
            mMediaIdIndex = data.getColumnIndexOrThrow(BaseColumns._ID);
            mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
            mArtistIndex = data.getColumnIndexOrThrow(AudioColumns.ARTIST);
            mAlbumIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM);

            //TODO: rewrite fragment to make it more efficient so this section can be removed

            long[] mNowPlaying = MusicUtils.getQueue();

        	String[] audioCols = new String[] { BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ARTIST, AudioColumns.ALBUM};
            
            MatrixCursor playlistCursor = new MatrixCursor(audioCols);
        	for(int i = 0; i < mNowPlaying.length; i++){
        		data.moveToPosition(-1);
        		while (data.moveToNext()) {
                    long audioid = data.getLong(mMediaIdIndex);
                	if( audioid == mNowPlaying[i]) {
                        String trackName = data.getString(NowPlayingFragment.mTitleIndex);
                        String artistName = data.getString(NowPlayingFragment.mArtistIndex);
                        String albumName = data.getString(NowPlayingFragment.mAlbumIndex);
                		playlistCursor.addRow(new Object[] {audioid, trackName, artistName, albumName });

                	}
                }
        	}
            mMediaIdIndex = playlistCursor.getColumnIndexOrThrow(BaseColumns._ID);
            mTitleIndex = playlistCursor.getColumnIndexOrThrow(MediaColumns.TITLE);
            mArtistIndex = playlistCursor.getColumnIndexOrThrow(AudioColumns.ARTIST);
            mAlbumIndex = playlistCursor.getColumnIndexOrThrow(AudioColumns.ALBUM);
            data.close();
            if(mCursor!=null)
            	mCursor.close();
            mTrackAdapter.changeCursor(playlistCursor);
            mListView.invalidateViews();
            mCursor = playlistCursor;
            return;
            
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mTrackAdapter != null){
            if(mCursor!=null)
            	mCursor.close();
            mTrackAdapter.changeCursor(null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, PLAY_SELECTION, 0, getResources().getString(R.string.play_all));
        menu.add(0, ADD_TO_PLAYLIST, 0, getResources().getString(R.string.add_to_playlist));
        menu.add(0, USE_AS_RINGTONE, 0, getResources().getString(R.string.use_as_ringtone));
        menu.add(0, REMOVE, 0, R.string.remove);
        
        menu.add(0, SEARCH, 0, getResources().getString(R.string.search));

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo)menuInfo;
        mSelectedPosition = mi.position;
        mCursor.moveToPosition(mSelectedPosition);

        try {
            mSelectedId = mCursor.getLong(mMediaIdIndex);
        } catch (IllegalArgumentException ex) {
            mSelectedId = mi.id;
        }

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
            case REMOVE: {
                removePlaylistItem(mSelectedPosition);
                break;
            }
            case SEARCH: {
                MusicUtils.doSearch(getActivity(), mCursor, mTitleIndex);
                break;
            }
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
                return;
            }
        }
        MusicUtils.playAll(getActivity(), mCursor, position);
    }

    /**
     * Update the list as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mListView != null) {
                mTrackAdapter.notifyDataSetChanged();
                mListView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListView.setSelection(MusicUtils.getQueuePosition());
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
        filter.addAction(ApolloService.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(mMediaStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }

    /**
     * @param which
     */
    private void removePlaylistItem(int which) {

        mCursor.moveToPosition(which);
        long id = mCursor.getLong(mMediaIdIndex);
        MusicUtils.removeTrack(id);
        reloadQueueCursor();
        mListView.invalidateViews();
    }

    /**
     * Reload the queue after we remove a track
     */
    private void reloadQueueCursor() {
        String[] cols = new String[] {
                BaseColumns._ID, MediaColumns.TITLE, MediaColumns.DATA, AudioColumns.ALBUM,
                AudioColumns.ARTIST, AudioColumns.ARTIST_ID
        };
        StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + MediaColumns.TITLE + " != ''");
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        long[] mNowPlaying = MusicUtils.getQueue();
        if (mNowPlaying.length == 0) {
        }
        selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < mNowPlaying.length; i++) {
            selection.append(mNowPlaying[i]);
            if (i < mNowPlaying.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        mCursor = MusicUtils.query(getActivity(), uri, cols, selection.toString(), null, null);

        mMediaIdIndex =mCursor.getColumnIndexOrThrow(BaseColumns._ID);
        mTitleIndex = mCursor.getColumnIndexOrThrow(MediaColumns.TITLE);
        mArtistIndex = mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST);
        mAlbumIndex = mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM);
        
        String[] audioCols = new String[] { BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ARTIST, AudioColumns.ALBUM};
        
        MatrixCursor playlistCursor = new MatrixCursor(audioCols);
    	for(int i = 0; i < mNowPlaying.length; i++){
    		mCursor.moveToPosition(-1);
    		while (mCursor.moveToNext()) {
                long audioid = mCursor.getLong(mMediaIdIndex);
            	if( audioid == mNowPlaying[i]) {
                    String trackName = mCursor.getString(NowPlayingFragment.mTitleIndex);
                    String artistName = mCursor.getString(NowPlayingFragment.mArtistIndex);
                    String albumName = mCursor.getString(NowPlayingFragment.mAlbumIndex);
            		playlistCursor.addRow(new Object[] {audioid, trackName, artistName ,albumName});

            	}
            }
    	}
        mMediaIdIndex = playlistCursor.getColumnIndexOrThrow(BaseColumns._ID);
        mTitleIndex = playlistCursor.getColumnIndexOrThrow(MediaColumns.TITLE);
        mArtistIndex = playlistCursor.getColumnIndexOrThrow(AudioColumns.ARTIST);
        mAlbumIndex = playlistCursor.getColumnIndexOrThrow(AudioColumns.ALBUM);
        mCursor = playlistCursor;
        mTrackAdapter.changeCursor(playlistCursor);
            
        
    }

}
