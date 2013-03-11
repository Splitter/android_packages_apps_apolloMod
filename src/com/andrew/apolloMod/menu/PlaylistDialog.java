/*
 *              Copyright (C) 2011 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apolloMod.menu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;

import static com.andrew.apolloMod.Constants.INTENT_CREATE_PLAYLIST;
import static com.andrew.apolloMod.Constants.INTENT_KEY_DEFAULT_NAME;
import static com.andrew.apolloMod.Constants.INTENT_KEY_RENAME;
import static com.andrew.apolloMod.Constants.INTENT_PLAYLIST_LIST;
import static com.andrew.apolloMod.Constants.INTENT_RENAME_PLAYLIST;
import static com.andrew.apolloMod.Constants.PLAYLIST_NAME_FAVORITES;

public class PlaylistDialog extends FragmentActivity implements TextWatcher,
        OnCancelListener, OnShowListener {

    private AlertDialog mPlaylistDialog;

    private String action;

    private EditText mPlaylist;

    private String mDefaultName, mOriginalName;

    private long mRenameId;

    private long[] mList = new long[] {};

    private final OnClickListener mRenamePlaylistListener = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {

            String name = mPlaylist.getText().toString();
            MusicUtils.renamePlaylist(PlaylistDialog.this, mRenameId, name);
            finish();
        }
    };

    private final OnClickListener mCreatePlaylistListener = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {

            String name = mPlaylist.getText().toString();
            if (name != null && name.length() > 0) {
                int id = idForplaylist(name);
                if (id >= 0) {
                    MusicUtils.clearPlaylist(PlaylistDialog.this, id);
                    MusicUtils.addToPlaylist(PlaylistDialog.this, mList, id);
                } else {
                    long new_id = MusicUtils.createPlaylist(PlaylistDialog.this, name);
                    if (new_id >= 0) {
                        MusicUtils.addToPlaylist(PlaylistDialog.this, mList, new_id);
                    }
                }
                finish();
            }
        }
    };

    @Override
    public void afterTextChanged(Editable s) {

        // don't care about this one
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        // don't care about this one
    }

    @Override
    public void onCancel(DialogInterface dialog) {

        if (dialog == mPlaylistDialog) {
            finish();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);
        setContentView(new LinearLayout(this));

        action = getIntent().getAction();

        mRenameId = icicle != null ? icicle.getLong(INTENT_KEY_RENAME) : getIntent().getLongExtra(
                INTENT_KEY_RENAME, -1);
        mList = icicle != null ? icicle.getLongArray(INTENT_PLAYLIST_LIST) : getIntent()
                .getLongArrayExtra(INTENT_PLAYLIST_LIST);
        if (INTENT_RENAME_PLAYLIST.equals(action)) {
            mOriginalName = nameForId(mRenameId);
            mDefaultName = icicle != null ? icicle.getString(INTENT_KEY_DEFAULT_NAME)
                    : mOriginalName;
        } else if (INTENT_CREATE_PLAYLIST.equals(action)) {
            mDefaultName = icicle != null ? icicle.getString(INTENT_KEY_DEFAULT_NAME)
                    : makePlaylistName();
            mOriginalName = mDefaultName;
        }

        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();

        mPlaylistDialog = new AlertDialog.Builder(this).create();
        mPlaylistDialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (action != null && mRenameId >= 0 && mOriginalName != null || mDefaultName != null) {

            mPlaylist = new EditText(this);
            mPlaylist.setSingleLine(true);
            mPlaylist.setText(mDefaultName);
            mPlaylist.setSelection(mDefaultName.length());
            mPlaylist.addTextChangedListener(this);

            mPlaylistDialog.setIcon(android.R.drawable.ic_dialog_info);
            String promptformat;
            String prompt = "";
            if (INTENT_RENAME_PLAYLIST.equals(action)) {
                promptformat = getString(R.string.rename_playlist);
                prompt = String.format(promptformat, mOriginalName, mDefaultName);
            } else if (INTENT_CREATE_PLAYLIST.equals(action)) {
                promptformat = getString(R.string.new_playlist);
                prompt = String.format(promptformat, mDefaultName);
            }

            mPlaylistDialog.setTitle(prompt);
            mPlaylistDialog.setView(mPlaylist, (int)(8 * dm.density), (int)(8 * dm.density),
                    (int)(8 * dm.density), (int)(4 * dm.density));
            if (INTENT_RENAME_PLAYLIST.equals(action)) {
                mPlaylistDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.save),
                        mRenamePlaylistListener);
                mPlaylistDialog.setOnShowListener(this);
            } else if (INTENT_CREATE_PLAYLIST.equals(action)) {
                mPlaylistDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.save),
                        mCreatePlaylistListener);
            }
            mPlaylistDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                    new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            finish();
                        }
                    });
            mPlaylistDialog.setOnCancelListener(this);
            mPlaylistDialog.show();
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    public void onPause() {

        if (mPlaylistDialog != null && mPlaylistDialog.isShowing()) {
            mPlaylistDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {

        if (INTENT_RENAME_PLAYLIST.equals(action)) {
            outcicle.putString(INTENT_KEY_DEFAULT_NAME, mPlaylist.getText().toString());
            outcicle.putLong(INTENT_KEY_RENAME, mRenameId);
        } else if (INTENT_CREATE_PLAYLIST.equals(action)) {
            outcicle.putString(INTENT_KEY_DEFAULT_NAME, mPlaylist.getText().toString());
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {

        if (dialog == mPlaylistDialog) {
            setSaveButton();
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        setSaveButton();
    }

    private int idForplaylist(String name) {

        Cursor cursor = MusicUtils.query(this, Audio.Playlists.EXTERNAL_CONTENT_URI, new String[] {
            Audio.Playlists._ID
        }, Audio.Playlists.NAME + "=?", new String[] {
            name
        }, Audio.Playlists.NAME, 0);
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
            cursor.close();
        }

        return id;
    }

    private String makePlaylistName() {

        String template = getString(R.string.new_playlist_name_template);
        int num = 1;

        String[] cols = new String[] {
            Audio.Playlists.NAME
        };
        ContentResolver resolver = getContentResolver();
        String whereclause = Audio.Playlists.NAME + " != ''";
        Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols,
                whereclause, null, Audio.Playlists.NAME);

        if (cursor == null)
            return null;

        String suggestedname;
        suggestedname = String.format(template, num++);

        // Need to loop until we've made 1 full pass through without finding a
        // match. Looping more than once shouldn't happen very often, but will
        // happen if you have playlists named
        // "New Playlist 1"/10/2/3/4/5/6/7/8/9, where making only one pass would
        // result in "New Playlist 10" being erroneously picked for the new
        // name.
        boolean done = false;
        while (!done) {
            done = true;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String playlistname = cursor.getString(0);
                if (playlistname.compareToIgnoreCase(suggestedname) == 0) {
                    suggestedname = String.format(template, num++);
                    done = false;
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        return suggestedname;
    };

    private String nameForId(long id) {

        Cursor cursor = MusicUtils.query(this, Audio.Playlists.EXTERNAL_CONTENT_URI, new String[] {
            Audio.Playlists.NAME
        }, Audio.Playlists._ID + "=?", new String[] {
            Long.valueOf(id).toString()
        }, Audio.Playlists.NAME);
        String name = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                name = cursor.getString(0);
            }
            cursor.close();
        }
        return name;
    }

    private void setSaveButton() {

        String typedname = mPlaylist.getText().toString();
        Button button = mPlaylistDialog.getButton(Dialog.BUTTON_POSITIVE);
        if (button == null)
            return;
        if (typedname.trim().length() == 0 || PLAYLIST_NAME_FAVORITES.equals(typedname)) {
            button.setEnabled(false);
        } else {
            button.setEnabled(true);
            if (idForplaylist(typedname) >= 0 && !mOriginalName.equals(typedname)) {
                button.setText(R.string.overwrite);
            } else {
                button.setText(R.string.save);
            }
        }
        button.invalidate();
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (mPlaylistDialog != null) {
            mPlaylistDialog.show();
        }
    }
}
