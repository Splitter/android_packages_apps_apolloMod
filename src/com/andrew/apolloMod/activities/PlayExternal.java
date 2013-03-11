/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.andrew.apolloMod.IApolloService;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ServiceToken;

import java.io.File;
import java.net.URLDecoder;

/**
 * An activity that lets external browsers launching music inside Apollo
 */
public class PlayExternal extends Activity
    implements ServiceConnection, DialogInterface.OnCancelListener {

    private static final String TAG = "PlayExternal";

    private ServiceToken mToken;
    private Uri mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the external file to play
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        mUri = intent.getData();
        if (mUri == null) {
            finish();
            return;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        MusicUtils.mService = IApolloService.Stub.asInterface(obj);
        play(this.mUri);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.mService = null;
    }

    @Override
    protected void onStart() {
        // Bind to Service
        mToken = MusicUtils.bindToService(this, this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unbind
        if (MusicUtils.mService != null)
            MusicUtils.unbindFromService(mToken);
        super.onStop();
    }

    private void play(Uri uri) {
        try {
            final String file = URLDecoder.decode( uri.toString(), "UTF-8");
            final String name = new File(file).getName();

            // Try to resolve the file to a media id
            final long id = MusicUtils.mService.getIdFromPath(file);
            if( id == -1 ) {
                // Open the stream, But we will not have album information
                openFile(file);
            }
            else {
                // Show a dialog asking the user for play or queue the song
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(this, R.style.Theme_Light_Translucent_Dialog);
                builder.setTitle(R.string.app_name);
                builder.setMessage(getString(R.string.play_external_question_msg, name));

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    playOrEnqueuFile(file, id, false);
                                    break;

                                case DialogInterface.BUTTON_NEUTRAL:
                                    playOrEnqueuFile(file, id, true);
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;

                                default:
                                    break;
                            }
                        } finally {
                            finish();
                        }
                    }
                };
                builder.setPositiveButton(R.string.play_external_question_button_play, listener);
                builder.setNeutralButton(R.string.play_external_question_button_queue, listener);
                builder.setNegativeButton(R.string.play_external_question_button_cancel, listener);

                Dialog dialog = builder.create();
                dialog.setOnCancelListener(this);
                dialog.show();
            }

        } catch (Exception e) {
            Toast.makeText(
                    getApplicationContext(),
                    R.string.play_external_error,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, String.format("Failed to play external file: ", uri.toString()), e);
            try {
                Thread.sleep(1000L);
            }catch (Exception e2) {}
            finish();
        }

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    private void playOrEnqueuFile(String file, long id, boolean enqueue) {
        final long[] list = new long[] {id};
        if (!enqueue) {
            //Remove the actual queue
            MusicUtils.removeAllTracks();
            MusicUtils.playAll(getApplicationContext(), list, 0);
        }
        else {
            MusicUtils.addToCurrentPlaylist(getApplicationContext(), list);
        }

        // Show now playing
        Intent intent = new Intent(this, AudioPlayerHolder.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openFile(String file) throws RemoteException {
        // Stop, load and play
        MusicUtils.mService.stop();
        MusicUtils.mService.openFile(file);
        MusicUtils.mService.play();

        // Show now playing
        Intent nowPlayingIntent = new Intent(this, AudioPlayerHolder.class);
        startActivity(nowPlayingIntent);
    }

}
