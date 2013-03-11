/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apolloMod.activities;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ServiceToken;

import static com.andrew.apolloMod.Constants.ALBUM_KEY;
import static com.andrew.apolloMod.Constants.ARTIST_ID;
import static com.andrew.apolloMod.Constants.ARTIST_KEY;
import static com.andrew.apolloMod.Constants.MIME_TYPE;

public class QueryBrowserActivity extends ListActivity implements ServiceConnection {
    private QueryListAdapter mAdapter;

    private boolean mAdapterSent;

    private String mFilterString = "";

    private ServiceToken mToken;

    public QueryBrowserActivity() {
    }

    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mAdapter = (QueryListAdapter)getLastNonConfigurationInstance();
        mToken = MusicUtils.bindToService(this, this);
        // defer the real work until we're bound to the service
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (mAdapter != null) {
            getQueryCursor(mAdapter.getQueryHandler(), null);
        }

        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;

        if (Intent.ACTION_VIEW.equals(action)) {
            // this is something we got from the search bar
            Uri uri = intent.getData();
            String path = uri.toString();
            if (path.startsWith("content://media/external/audio/media/")) {
                // This is a specific file
                String id = uri.getLastPathSegment();
                long[] list = new long[] {
                    Long.valueOf(id)
                };
                MusicUtils.playAll(this, list, 0);
                finish();
                return;
            } else if (path.startsWith("content://media/external/audio/albums/")) {
                // This is an album, show the songs on it
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                i.putExtra("album", uri.getLastPathSegment());
                startActivity(i);
                finish();
                return;
            } else if (path.startsWith("content://media/external/audio/artists/")) {
                intent = new Intent(Intent.ACTION_VIEW);

                Bundle bundle = new Bundle();
                bundle.putString(MIME_TYPE, Audio.Artists.CONTENT_TYPE);
                bundle.putString(ARTIST_KEY, uri.getLastPathSegment());
                bundle.putLong(BaseColumns._ID,
                        ApolloUtils.getArtistId(uri.getLastPathSegment(), ARTIST_ID, this));

                intent.setClass(this, TracksBrowser.class);
                intent.putExtras(bundle);
                startActivity(intent);
                return;
            }
        }

        mFilterString = intent.getStringExtra(SearchManager.QUERY);
        if (MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(action)) {
            String focus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS);
            String artist = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST);
            String album = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM);
            String title = intent.getStringExtra(MediaStore.EXTRA_MEDIA_TITLE);
            if (focus != null) {
                if (focus.startsWith("audio/") && title != null) {
                    mFilterString = title;
                } else if (focus.equals(Audio.Albums.ENTRY_CONTENT_TYPE)) {
                    if (album != null) {
                        mFilterString = album;
                        if (artist != null) {
                            mFilterString = mFilterString + " " + artist;
                        }
                    }
                } else if (focus.equals(Audio.Artists.ENTRY_CONTENT_TYPE)) {
                    if (artist != null) {
                        mFilterString = artist;
                    }
                }
            }
        }

        setContentView(R.layout.listview);
        mTrackList = getListView();
        mTrackList.setTextFilterEnabled(true);
        if (mAdapter == null) {
            mAdapter = new QueryListAdapter(getApplication(), this, R.layout.listview_items, null, // cursor
                    new String[] {}, new int[] {}, 0);
            setListAdapter(mAdapter);
            if (TextUtils.isEmpty(mFilterString)) {
                getQueryCursor(mAdapter.getQueryHandler(), null);
            } else {
                mTrackList.setFilterText(mFilterString);
                mFilterString = null;
            }
        } else {
            mAdapter.setActivity(this);
            setListAdapter(mAdapter);
            mQueryCursor = mAdapter.getCursor();
            if (mQueryCursor != null) {
                init(mQueryCursor);
            } else {
                getQueryCursor(mAdapter.getQueryHandler(), mFilterString);
            }
        }

        LinearLayout emptyness = (LinearLayout)findViewById(R.id.empty_view);
        emptyness.setVisibility(View.GONE);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(mToken);
        // If we have an adapter and didn't send it off to another activity yet,
        // we should
        // close its cursor, which we do by assigning a null cursor to it. Doing
        // this
        // instead of closing the cursor directly keeps the framework from
        // accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            mAdapter.changeCursor(null);
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
        try {
            setListAdapter(null);
        } catch (NullPointerException e) {

        }
        mAdapter = null;
        super.onDestroy();
    }

    public void init(Cursor c) {

        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(c);

        if (mQueryCursor == null) {
            setListAdapter(null);
            return;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Dialog doesn't allow us to wait for a result, so we need to store
        // the info we need for when the dialog posts its result
        mQueryCursor.moveToPosition(position);
        if (mQueryCursor.isBeforeFirst() || mQueryCursor.isAfterLast()) {
            return;
        }
        String selectedType = mQueryCursor.getString(mQueryCursor
                .getColumnIndexOrThrow(Audio.Media.MIME_TYPE));

        if ("artist".equals(selectedType)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            TextView tv1 = (TextView)v.findViewById(R.id.listview_item_line_one);
            String artistName = tv1.getText().toString();

            Bundle bundle = new Bundle();
            bundle.putString(MIME_TYPE, Audio.Artists.CONTENT_TYPE);
            bundle.putString(ARTIST_KEY, artistName);
            bundle.putLong(BaseColumns._ID, id);

            intent.setClass(this, TracksBrowser.class);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        } else if ("album".equals(selectedType)) {
            TextView tv1 = (TextView)v.findViewById(R.id.listview_item_line_one);
            TextView tv2 = (TextView)v.findViewById(R.id.listview_item_line_two);

            String artistName = tv2.getText().toString();
            String albumName = tv1.getText().toString();

            Bundle bundle = new Bundle();
            bundle.putString(MIME_TYPE, Audio.Albums.CONTENT_TYPE);
            bundle.putString(ARTIST_KEY, artistName);
            bundle.putString(ALBUM_KEY, albumName);
            bundle.putLong(BaseColumns._ID, id);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(this, TracksBrowser.class);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        } else if (position >= 0 && id >= 0) {
            long[] list = new long[] {
                id
            };
            MusicUtils.playAll(this, list, 0);
        } else {
            Log.e("QueryBrowser", "invalid position/id: " + position + "/" + id);
        }
    }

    private Cursor getQueryCursor(AsyncQueryHandler async, String filter) {
        if (filter == null) {
            filter = "";
        }
        String[] ccols = new String[] {
                BaseColumns._ID, Audio.Media.MIME_TYPE, Audio.Artists.ARTIST, Audio.Albums.ALBUM,
                Audio.Media.TITLE, "data1", "data2"
        };

        Uri search = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(filter));

        Cursor ret = null;
        if (async != null) {
            async.startQuery(0, null, search, ccols, null, null, null);
        } else {
            ret = MusicUtils.query(this, search, ccols, null, null, null);
        }
        return ret;
    }

    static class QueryListAdapter extends SimpleCursorAdapter {
        private QueryBrowserActivity mActivity = null;

        private final AsyncQueryHandler mQueryHandler;

        private String mConstraint = null;

        private boolean mConstraintIsValid = false;

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                mActivity.init(cursor);
            }
        }

        QueryListAdapter(Context context, QueryBrowserActivity currentactivity, int layout,
                Cursor cursor, String[] from, int[] to, int flags) {
            super(context, layout, cursor, from, to, flags);
            mActivity = currentactivity;
            mQueryHandler = new QueryHandler(context.getContentResolver());
        }

        public void setActivity(QueryBrowserActivity newactivity) {
            mActivity = newactivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            TextView tv1 = (TextView)view.findViewById(R.id.listview_item_line_one);
            tv1.setTextColor(Color.BLACK);
            TextView tv2 = (TextView)view.findViewById(R.id.listview_item_line_two);
            tv2.setTextColor(Color.BLACK);
            ImageView iv = (ImageView)view.findViewById(R.id.listview_item_image);
            iv.setVisibility(View.GONE);
            FrameLayout fl = (FrameLayout)view.findViewById(R.id.track_list_context_frame);
            fl.setVisibility(View.GONE);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            if (p == null) {
                // seen this happen, not sure why
                DatabaseUtils.dumpCursor(cursor);
                return;
            }
            p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            String mimetype = cursor.getString(cursor.getColumnIndexOrThrow(Audio.Media.MIME_TYPE));

            if (mimetype == null) {
                mimetype = "audio/";
            }
            if (mimetype.equals("artist")) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(Audio.Artists.ARTIST));
                String displayname = name;
                boolean isunknown = false;
                if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                    displayname = context.getString(R.string.unknown);
                    isunknown = true;
                }
                tv1.setText(displayname);

                int numalbums = cursor.getInt(cursor.getColumnIndexOrThrow("data1"));
                int numsongs = cursor.getInt(cursor.getColumnIndexOrThrow("data2"));

                String songs_albums = MusicUtils.makeAlbumsLabel(context, numalbums, numsongs,
                        isunknown);

                tv2.setText(songs_albums);

            } else if (mimetype.equals("album")) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(Audio.Albums.ALBUM));
                String displayname = name;
                if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                    displayname = context.getString(R.string.unknown);
                }
                tv1.setText(displayname);

                name = cursor.getString(cursor.getColumnIndexOrThrow(Audio.Artists.ARTIST));
                displayname = name;
                if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                    displayname = context.getString(R.string.unknown);
                }
                tv2.setText(displayname);

            } else if (mimetype.startsWith("audio/") || mimetype.equals("application/ogg")
                    || mimetype.equals("application/x-ogg")) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(Audio.Media.TITLE));
                tv1.setText(name);

                String displayname = cursor.getString(cursor
                        .getColumnIndexOrThrow(Audio.Artists.ARTIST));
                if (displayname == null || displayname.equals(MediaStore.UNKNOWN_STRING)) {
                    displayname = context.getString(R.string.unknown);
                }
                name = cursor.getString(cursor.getColumnIndexOrThrow(Audio.Albums.ALBUM));
                if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                    name = context.getString(R.string.unknown);
                }
                tv2.setText(displayname + " - " + name);
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != mActivity.mQueryCursor) {
                mActivity.mQueryCursor = cursor;
                super.changeCursor(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (mConstraintIsValid
                    && ((s == null && mConstraint == null) || (s != null && s.equals(mConstraint)))) {
                return getCursor();
            }
            Cursor c = mActivity.getQueryCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }
    }

    private ListView mTrackList;

    private Cursor mQueryCursor;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
