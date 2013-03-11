/**
 * 
 */

package com.andrew.apolloMod.ui.fragments.list;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.activities.TracksBrowser;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.GenreAdapter;

import static com.andrew.apolloMod.Constants.GENRE_KEY;
import static com.andrew.apolloMod.Constants.MIME_TYPE;

/**
 * @author Andrew Neal
 * @Note This is the fifth and final tab
 */
public class GenresFragment extends Fragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    private GenreAdapter mGenreAdapter;

    // ListView
    private ListView mListView;

    // Cursor
    private Cursor mCursor;

    // Current genre Id
    private String mCurrentGenreId;

    // Options
    private final int PLAY_SELECTION = 14;

    // Audio columns
    public static int mGenreIdIndex, mGenreNameIndex;

    // Bundle
    public GenresFragment() {
    }

    public GenresFragment(Bundle args) {
        setArguments(args);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // GenreAdapter
        mGenreAdapter = new GenreAdapter(getActivity(), R.layout.listview_items, null,
                new String[] {}, new int[] {}, 0);
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mGenreAdapter);

        // Important!
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.listview, container, false);
        mListView = (ListView)root.findViewById(android.R.id.list);
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = new String[] {
                Audio.Genres._ID, Audio.Genres.NAME
        };
        Uri uri = Audio.Genres.EXTERNAL_CONTENT_URI;
        String sortOrder = Audio.Genres.DEFAULT_SORT_ORDER;
        return new CursorLoader(getActivity(), uri, projection, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Check for database errors
        if (data == null) {
            return;
        }

        mGenreIdIndex = data.getColumnIndexOrThrow(Audio.Genres._ID);
        mGenreNameIndex = data.getColumnIndexOrThrow(Audio.Genres.NAME);
        mGenreAdapter.changeCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mGenreAdapter != null)
            mGenreAdapter.changeCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        tracksBrowser(position, id);
    }

    private void tracksBrowser(int index, long id) {

        String genreKey = mCursor.getString(mGenreNameIndex);

        Bundle bundle = new Bundle();
        bundle.putString(MIME_TYPE, Audio.Genres.CONTENT_TYPE);
        bundle.putString(GENRE_KEY, genreKey);
        bundle.putLong(BaseColumns._ID, id);

        Intent intent = new Intent(getActivity(), TracksBrowser.class);
        intent.putExtras(bundle);
        getActivity().startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, PLAY_SELECTION, 0, getResources().getString(R.string.play_all));

        mCurrentGenreId = mCursor.getString(mCursor.getColumnIndexOrThrow(BaseColumns._ID));

        String title = mCursor.getString(mGenreNameIndex);
        menu.setHeaderTitle(title);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION:
                long[] list = MusicUtils.getSongListForGenre(getActivity(),
                        Long.parseLong(mCurrentGenreId));
                MusicUtils.playAll(getActivity(), list, 0);
                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }
}
