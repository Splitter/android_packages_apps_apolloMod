/**
 * 
 */

package com.andrew.apolloMod.ui.fragments.list;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.andrew.apolloMod.NowPlayingCursor;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.AddIdCursorLoader;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.RecentlyAddedAdapter;

import static com.andrew.apolloMod.Constants.NUMWEEKS;

/**
 * @author Andrew Neal
 */
public class RecentlyAddedFragment extends Fragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener {

    // Adapter
    private RecentlyAddedAdapter mRecentlyAddedAdapter;

    // ListView
    private ListView mListView;

    // Cursor
    private Cursor mCursor;

    // Audio columns
    public static int mTitleIndex, mAlbumIndex, mAlbumIdIndex, mArtistIndex, mMediaIdIndex;

    // Bundle
    public RecentlyAddedFragment() {
    }

    public RecentlyAddedFragment(Bundle args) {
        setArguments(args);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Adapter
        mRecentlyAddedAdapter = new RecentlyAddedAdapter(getActivity(), R.layout.listview_items,
                null, new String[] {}, new int[] {}, 0);
        mListView.setOnCreateContextMenuListener(this);
        mListView.setAdapter(mRecentlyAddedAdapter);
        mListView.setOnItemClickListener(this);

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
                 BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
         };
         StringBuilder where = new StringBuilder();
         String sortOrder = MediaColumns.DATE_ADDED + " DESC";
         Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
         int X = MusicUtils.getIntPref(getActivity(), NUMWEEKS, 5) * 3600 * 24 * 7;
         where = new StringBuilder();
         where.append(MediaColumns.TITLE + " != ''");
         where.append(" AND " + AudioColumns.IS_MUSIC + "=1");
         where.append(" AND " + MediaColumns.DATE_ADDED + ">"
                 + (System.currentTimeMillis() / 1000 - X));
         return new AddIdCursorLoader(getActivity(), uri, projection, where.toString(), null, sortOrder);
        
        //return new RecentlyAddedLoader(getActivity());
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
        mRecentlyAddedAdapter.changeCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mRecentlyAddedAdapter != null)
            mRecentlyAddedAdapter.changeCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putAll(getArguments() != null ? getArguments() : new Bundle());
        super.onSaveInstanceState(outState);
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
                mRecentlyAddedAdapter.notifyDataSetChanged();
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
