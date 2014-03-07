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
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Playlists;
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
import com.andrew.apolloMod.ui.adapters.PlaylistListAdapter;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import static com.andrew.apolloMod.Constants.EXTERNAL;
import static com.andrew.apolloMod.Constants.INTENT_ADD_TO_PLAYLIST;
import static com.andrew.apolloMod.Constants.INTENT_PLAYLIST_LIST;
import static com.andrew.apolloMod.Constants.MIME_TYPE;
import static com.andrew.apolloMod.Constants.PLAYLIST_FAVORITES;

/**
 * @author Andrew Neal
 */
public class PlaylistListFragment extends RefreshableFragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    private PlaylistListAdapter mTrackAdapter;

    // ListView
    private DragSortListView mListView;

    // Cursor
    private Cursor mCursor;

    // Playlist ID
    private long mPlaylistId = -1;

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

    private boolean mEditMode = false;

    // Audio columns
    public static int mTitleIndex, mAlbumIndex, mArtistIndex, mMediaIdIndex;

    // Bundle
    public PlaylistListFragment() {
    }

    public PlaylistListFragment(Bundle args) {
        setArguments(args);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        isEditMode();

        // Adapter
        mTrackAdapter = new PlaylistListAdapter(getActivity(), R.layout.dragsort_listview_items, null,
                new String[] {}, new int[] {}, 0, mPlaylistId);
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mTrackAdapter);

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
        if (id < 0)
            return null;
        String[] projection = new String[] {
                Playlists.Members._ID, Playlists.Members.AUDIO_ID,
                MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST,
                AudioColumns.DURATION
        };
        StringBuilder where = new StringBuilder();
        String sortOrder = Playlists.Members.PLAY_ORDER;;
        where.append(AudioColumns.IS_MUSIC + "=1").append(" AND " + MediaColumns.TITLE + " != ''");
        
        Uri uri = Playlists.Members.getContentUri(EXTERNAL, mPlaylistId);
        
        
        if (getArguments() != null) {
            mPlaylistId = getArguments().getLong(BaseColumns._ID);
            where = new StringBuilder();
            where.append(AudioColumns.IS_MUSIC + "=1");
            where.append(" AND " + MediaColumns.TITLE + " != ''");
            if(mPlaylistId == PLAYLIST_FAVORITES){
                long favorites_id = MusicUtils.getFavoritesId(getActivity());
                projection = new String[] {
                        Playlists.Members._ID, Playlists.Members.AUDIO_ID,
                        MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
                };
                uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
            }         
        }
        return new CursorLoader(getActivity(), uri, projection, where.toString(), null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Check for database errors
        if (data == null) {
            return;
        }

        mMediaIdIndex = data.getColumnIndexOrThrow(Playlists.Members.AUDIO_ID);
        mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
        mAlbumIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM);        
        mArtistIndex = data.getColumnIndexOrThrow(AudioColumns.ARTIST);        
        mTrackAdapter.changeCursor(data);
        mListView.invalidateViews();
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mTrackAdapter != null)
            mTrackAdapter.changeCursor(null);
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
        if (mEditMode) {
            menu.add(0, REMOVE, 0, R.string.remove);
        }
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
            }
        }

    };

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        filter.addAction(ApolloService.QUEUE_CHANGED);
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
        if (mPlaylistId >= 0) {
            Uri uri = Playlists.Members.getContentUri(EXTERNAL, mPlaylistId);
            getActivity().getContentResolver().delete(uri, Playlists.Members.AUDIO_ID + "=" + id,
                    null);
        } else if (mPlaylistId == PLAYLIST_FAVORITES) {
            MusicUtils.removeFromFavorites(getActivity(), id);
        }
        mListView.invalidateViews();
    }

    
    /**
     * Check if we're viewing the contents of a playlist
     */
    public void isEditMode() {
        if (getArguments() != null) {
            String mimetype = getArguments().getString(MIME_TYPE);
            if (Audio.Playlists.CONTENT_TYPE.equals(mimetype)) {
                mPlaylistId = getArguments().getLong(BaseColumns._ID);
                switch ((int)mPlaylistId) {
                    case (int)PLAYLIST_FAVORITES:
                        mEditMode = true;
                        break;
                    default:
                        if (mPlaylistId > 0) {
                            mEditMode = true;
                        }
                        break;
                }
            }
        }
    }
}
