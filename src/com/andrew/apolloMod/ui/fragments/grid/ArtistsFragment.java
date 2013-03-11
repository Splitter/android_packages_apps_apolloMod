/**
 * 
 */

package com.andrew.apolloMod.ui.fragments.grid;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.activities.TracksBrowser;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.ArtistAdapter;

import static com.andrew.apolloMod.Constants.*;

/**
 * @author Andrew Neal
 * @Note This is the first tab
 */
public class ArtistsFragment extends Fragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    private ArtistAdapter mArtistAdapter;

    // GridView
    private GridView mGridView;

    // Cursor
    private Cursor mCursor;

    // Options
    private final int PLAY_SELECTION = 0;

    private final int ADD_TO_PLAYLIST = 1;

    private final int SEARCH = 2;

    // Artist ID
    private String mCurrentArtistId;

    // Album ID
    private String mCurrentAlbumId;

    // Audio columns
    public static int mArtistIdIndex, mArtistNameIndex, mArtistNumAlbumsIndex;

    public ArtistsFragment() {
    }

    public ArtistsFragment(Bundle bundle) {
        setArguments(bundle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // ArtistAdapter
        mArtistAdapter = new ArtistAdapter(getActivity(), R.layout.gridview_items, null,
                new String[] {}, new int[] {}, 0);
        mGridView.setOnCreateContextMenuListener(this);
        mGridView.setOnItemClickListener(this);
        mGridView.setAdapter(mArtistAdapter);
        mGridView.setTextFilterEnabled(true);

        // Important!
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.gridview, container, false);
        mGridView = ((GridView)root.findViewById(R.id.gridview));
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                BaseColumns._ID, ArtistColumns.ARTIST, ArtistColumns.NUMBER_OF_ALBUMS
        };
        Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;
        String sortOrder = Audio.Artists.DEFAULT_SORT_ORDER;
        return new CursorLoader(getActivity(), uri, projection, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Check for database errors
        if (data == null) {
            return;
        }

        mArtistIdIndex = data.getColumnIndexOrThrow(BaseColumns._ID);
        mArtistNameIndex = data.getColumnIndexOrThrow(ArtistColumns.ARTIST);
        mArtistNumAlbumsIndex = data.getColumnIndexOrThrow(ArtistColumns.NUMBER_OF_ALBUMS);
        mArtistAdapter.changeCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mArtistAdapter != null)
            mArtistAdapter.changeCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        tracksBrowser(id);
    }

    /**
     * @param id
     */
    private void tracksBrowser(long id) {

        String artistName = mCursor.getString(mArtistNameIndex);
        String artistNulAlbums = mCursor.getString(mArtistNumAlbumsIndex);        

        Bundle bundle = new Bundle();
        bundle.putString(MIME_TYPE, Audio.Artists.CONTENT_TYPE);
        bundle.putString(ARTIST_KEY, artistName);
        bundle.putString(NUMALBUMS, artistNulAlbums);
        
        bundle.putLong(BaseColumns._ID, id);

        ApolloUtils.setArtistId(artistName, id, ARTIST_ID, getActivity());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(getActivity(), TracksBrowser.class);
        intent.putExtras(bundle);
        getActivity().startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, PLAY_SELECTION, 0, getResources().getString(R.string.play_all));
        menu.add(0, ADD_TO_PLAYLIST, 0, getResources().getString(R.string.add_to_playlist));
        menu.add(0, SEARCH, 0, getResources().getString(R.string.search));

        mCurrentArtistId = mCursor.getString(mArtistIdIndex);
        mCurrentAlbumId = mCursor.getString(mCursor.getColumnIndexOrThrow(BaseColumns._ID));

        menu.setHeaderView(setHeaderLayout());
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                long[] list = mCurrentArtistId != null ? MusicUtils.getSongListForArtist(
                        getActivity(), Long.parseLong(mCurrentArtistId)) : MusicUtils
                        .getSongListForAlbum(getActivity(), Long.parseLong(mCurrentAlbumId));
                MusicUtils.playAll(getActivity(), list, 0);
                break;
            }
            case ADD_TO_PLAYLIST: {
                Intent intent = new Intent(INTENT_ADD_TO_PLAYLIST);
                long[] list = mCurrentArtistId != null ? MusicUtils.getSongListForArtist(
                        getActivity(), Long.parseLong(mCurrentArtistId)) : MusicUtils
                        .getSongListForAlbum(getActivity(), Long.parseLong(mCurrentAlbumId));
                intent.putExtra(INTENT_PLAYLIST_LIST, list);
                getActivity().startActivity(intent);
                break;
            }
            case SEARCH: {
                MusicUtils.doSearch(getActivity(), mCursor, mArtistNameIndex);
                break;
            }
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Update the list as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGridView != null) {
                mArtistAdapter.notifyDataSetChanged();
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
     * @return A custom ContextMenu header
     */
    public View setHeaderLayout() {
        // Get artist name
        final String artistName = mCursor.getString(mArtistNameIndex);

        // Inflate the header View
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View header = inflater.inflate(R.layout.context_menu_header, null, false);

        // Artist image
        final ImageView mHanderImage = (ImageView)header.findViewById(R.id.header_image);
        
        ImageInfo mInfo = new ImageInfo();
        mInfo.type = TYPE_ARTIST;
        mInfo.size = SIZE_THUMB;
        mInfo.source = SRC_FIRST_AVAILABLE;
        mInfo.data = new String[]{ artistName};
        
        ImageProvider.getInstance(getActivity()).loadImage( mHanderImage, mInfo );

        // Set artist name
        TextView headerText = (TextView)header.findViewById(R.id.header_text);
        headerText.setText(artistName);
        headerText.setBackgroundColor(getResources().getColor(R.color.transparent_black));
        return header;
    }
}
