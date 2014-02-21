/**
 * 
 */

package com.andrew.apolloMod.ui.fragments.list;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrew.apolloMod.NowPlayingCursor;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.RefreshableFragment;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.TrackAdapter;

import static com.andrew.apolloMod.Constants.EXTERNAL;
import static com.andrew.apolloMod.Constants.INTENT_ADD_TO_PLAYLIST;
import static com.andrew.apolloMod.Constants.INTENT_PLAYLIST_LIST;
import static com.andrew.apolloMod.Constants.MIME_TYPE;
import static com.andrew.apolloMod.Constants.PLAYLIST_FAVORITES;
import static com.andrew.apolloMod.Constants.PLAYLIST_QUEUE;

/**
 * @author Andrew Neal
 */
public class TracksFragment extends RefreshableFragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    private TrackAdapter mTrackAdapter;

    // ListView
    private ListView mListView;

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
    public TracksFragment() {
    }

    public TracksFragment(Bundle args) {
        setArguments(args);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        isEditMode();

        // Adapter
        mTrackAdapter = new TrackAdapter(getActivity(), R.layout.listview_items, null,
                new String[] {}, new int[] {}, 0);
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
        View root = inflater.inflate(R.layout.listview, container, false);
        mListView = (ListView)root.findViewById(android.R.id.list);

        // Align the track list with the header, in other words,OCD.
        TextView mHeader = (TextView)root.findViewById(R.id.title);
        int eight = (int)getActivity().getResources().getDimension(
                R.dimen.list_separator_padding_left_right);
        mHeader.setPadding(eight, 0, 0, 0);

        // Set the header while in @TracksBrowser
        String header = getActivity().getResources().getString(R.string.track_header);
        int left = getActivity().getResources().getInteger(R.integer.listview_padding_left);
        int right = getActivity().getResources().getInteger(R.integer.listview_padding_right);
        ApolloUtils.listHeader(this, root, header);
        ApolloUtils.setListPadding(this, mListView, left, 0, right, 0);

        // Hide the extra spacing from the Bottom ActionBar in the queue
        // Fragment in @AudioPlayerHolder
        if (getArguments() != null) {
            mPlaylistId = getArguments().getLong(BaseColumns._ID);
            String mimeType = getArguments().getString(MIME_TYPE);
            if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
                switch ((int)mPlaylistId) {
                    case (int)PLAYLIST_QUEUE:
                        LinearLayout emptyness = (LinearLayout)root.findViewById(R.id.empty_view);
                        emptyness.setVisibility(View.GONE);
                }
            }
        }
        else{

        	RelativeLayout  shuffle = (RelativeLayout)root.findViewById(R.id.shuffle_wrapper);
        	shuffle.setVisibility(View.VISIBLE);
        	shuffle.setOnClickListener(new RelativeLayout.OnClickListener() {  
                public void onClick(View v)
                {

                    Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
                    String[] projection = new String[] {
                        BaseColumns._ID
                    };
                    String selection = AudioColumns.IS_MUSIC + "=1";
                    String sortOrder = "RANDOM()";
                    Cursor cursor = MusicUtils.query(getActivity(), uri, projection, selection, null, sortOrder);
                    if (cursor != null) {
                        MusicUtils.shuffleAll(getActivity(), cursor);
                        cursor.close();
                        cursor = null;
                    }
                }
             });
        	
        }
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
        if (getArguments() != null) {
            mPlaylistId = getArguments().getLong(BaseColumns._ID);
            String mimeType = getArguments().getString(MIME_TYPE);
            if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
                where = new StringBuilder();
                where.append(AudioColumns.IS_MUSIC + "=1");
                where.append(" AND " + MediaColumns.TITLE + " != ''");
                switch ((int)mPlaylistId) {
                    case (int)PLAYLIST_QUEUE:
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
                        break;
                    case (int)PLAYLIST_FAVORITES:
                        long favorites_id = MusicUtils.getFavoritesId(getActivity());
                        projection = new String[] {
                                Playlists.Members._ID, Playlists.Members.AUDIO_ID,
                                MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
                        };
                        uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
                        sortOrder = Playlists.Members.DEFAULT_SORT_ORDER;
                        break;
                    default:
                        if (id < 0)
                            return null;
                        projection = new String[] {
                                Playlists.Members._ID, Playlists.Members.AUDIO_ID,
                                MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST,
                                AudioColumns.DURATION
                        };

                        uri = Playlists.Members.getContentUri(EXTERNAL, mPlaylistId);
                        sortOrder = Playlists.Members.DEFAULT_SORT_ORDER;
                        break;
                }
            } else if (Audio.Genres.CONTENT_TYPE.equals(mimeType)) {
                long genreId = getArguments().getLong(BaseColumns._ID);
                uri = Genres.Members.getContentUri(EXTERNAL, genreId);
                projection = new String[] {
                        BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM,
                        AudioColumns.ARTIST
                };
                where = new StringBuilder();
                where.append(AudioColumns.IS_MUSIC + "=1").append(
                        " AND " + MediaColumns.TITLE + " != ''");
                sortOrder = Genres.Members.DEFAULT_SORT_ORDER;
            } else {
                if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
                    long albumId = getArguments().getLong(BaseColumns._ID);
                    where.append(" AND " + AudioColumns.ALBUM_ID + "=" + albumId);
                    sortOrder = Audio.Media.TRACK + ", " + sortOrder;
                } else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
                    sortOrder = MediaColumns.TITLE;
                    long artist_id = getArguments().getLong(BaseColumns._ID);
                    where.append(" AND " + AudioColumns.ARTIST_ID + "=" + artist_id);
                }
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

        if (getArguments() != null
                && Playlists.CONTENT_TYPE.equals(getArguments().getString(MIME_TYPE))
                && (getArguments().getLong(BaseColumns._ID) >= 0 || getArguments().getLong(
                        BaseColumns._ID) == PLAYLIST_FAVORITES)) {
            mMediaIdIndex = data.getColumnIndexOrThrow(Playlists.Members.AUDIO_ID);
            mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
            mAlbumIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM);
            // FIXME
            // mArtistIndex =
            // data.getColumnIndexOrThrow(Playlists.Members.ARTIST);
        } else if (getArguments() != null
                && Genres.CONTENT_TYPE.equals(getArguments().getString(MIME_TYPE))) {
            mMediaIdIndex = data.getColumnIndexOrThrow(BaseColumns._ID);
            mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
            mArtistIndex = data.getColumnIndexOrThrow(AudioColumns.ARTIST);
            mAlbumIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM);
        } else if (getArguments() != null
                && Artists.CONTENT_TYPE.equals(getArguments().getString(MIME_TYPE))) {
            mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
            // mArtistIndex is "line2" of the ListView
            mArtistIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM);
        } else if (getArguments() != null
                && Albums.CONTENT_TYPE.equals(getArguments().getString(MIME_TYPE))) {
            mMediaIdIndex = data.getColumnIndexOrThrow(BaseColumns._ID);
            mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
            mArtistIndex = data.getColumnIndexOrThrow(AudioColumns.ARTIST);
        } else {
            mMediaIdIndex = data.getColumnIndexOrThrow(BaseColumns._ID);
            mTitleIndex = data.getColumnIndexOrThrow(MediaColumns.TITLE);
            mArtistIndex = data.getColumnIndexOrThrow(AudioColumns.ARTIST);
            mAlbumIndex = data.getColumnIndexOrThrow(AudioColumns.ALBUM);

            //TODO: rewrite fragment to make it more efficient so this section can be removed

            if (mPlaylistId == PLAYLIST_QUEUE) {
                long[] mNowPlaying = MusicUtils.getQueue();

            	String[] audioCols = new String[] { BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ARTIST, AudioColumns.ALBUM};
                
                MatrixCursor playlistCursor = new MatrixCursor(audioCols);
            	for(int i = 0; i < mNowPlaying.length; i++){
            		data.moveToPosition(-1);
            		while (data.moveToNext()) {
	                    long audioid = data.getLong(mMediaIdIndex);
	                	if( audioid == mNowPlaying[i]) {
	                        String trackName = data.getString(TracksFragment.mTitleIndex);
	                        String artistName = data.getString(TracksFragment.mArtistIndex);
	                        String albumName = data.getString(TracksFragment.mAlbumIndex);
	                		playlistCursor.addRow(new Object[] {audioid, trackName, artistName, albumName });

	                	}
	                }
            	}
                mMediaIdIndex = playlistCursor.getColumnIndexOrThrow(BaseColumns._ID);
                mTitleIndex = playlistCursor.getColumnIndexOrThrow(MediaColumns.TITLE);
                mArtistIndex = playlistCursor.getColumnIndexOrThrow(AudioColumns.ARTIST);
                mAlbumIndex = playlistCursor.getColumnIndexOrThrow(AudioColumns.ALBUM);
                mTrackAdapter.changeCursor(playlistCursor);
                mListView.invalidateViews();
                mCursor = playlistCursor;
	            return;
            }
        }
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
                // Scroll to the currently playing track in the queue
                if (mPlaylistId == PLAYLIST_QUEUE)
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
        } else if (mPlaylistId == PLAYLIST_QUEUE) {
            MusicUtils.removeTrack(id);
            reloadQueueCursor();
        } else if (mPlaylistId == PLAYLIST_FAVORITES) {
            MusicUtils.removeFromFavorites(getActivity(), id);
        }
        mListView.invalidateViews();
    }

    /**
     * Reload the queue after we remove a track
     */
    private void reloadQueueCursor() {
        if (mPlaylistId == PLAYLIST_QUEUE) {
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
            
            String[] audioCols = new String[] { BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ARTIST};
            
            MatrixCursor playlistCursor = new MatrixCursor(audioCols);
        	for(int i = 0; i < mNowPlaying.length; i++){
        		mCursor.moveToPosition(-1);
        		while (mCursor.moveToNext()) {
                    long audioid = mCursor.getLong(mMediaIdIndex);
                	if( audioid == mNowPlaying[i]) {
                        String trackName = mCursor.getString(TracksFragment.mTitleIndex);
                        String artistName = mCursor.getString(TracksFragment.mArtistIndex);
                        String albumName = mCursor.getString(TracksFragment.mAlbumIndex);
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

    /**
     * Check if we're viewing the contents of a playlist
     */
    public void isEditMode() {
        if (getArguments() != null) {
            String mimetype = getArguments().getString(MIME_TYPE);
            if (Audio.Playlists.CONTENT_TYPE.equals(mimetype)) {
                mPlaylistId = getArguments().getLong(BaseColumns._ID);
                switch ((int)mPlaylistId) {
                    case (int)PLAYLIST_QUEUE:
                        mEditMode = true;
                        break;
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
