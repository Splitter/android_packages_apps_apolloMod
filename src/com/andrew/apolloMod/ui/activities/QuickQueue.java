/**
 * 
 */

package com.andrew.apolloMod.ui.activities;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentActivity;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.ui.fragments.grid.QuickQueueFragment;

import static com.andrew.apolloMod.Constants.CURRENT_THEME;
import static com.andrew.apolloMod.Constants.MIME_TYPE;
import static com.andrew.apolloMod.Constants.PLAYLIST_QUEUE;

/**
 * @author Andrew Neal
 */
public class QuickQueue extends FragmentActivity {

    @Override
    protected void onCreate(Bundle icicle) {
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	String type = sp.getString(CURRENT_THEME, getResources().getString(R.string.theme_light));                    
		if(type.equals(getResources().getString(R.string.theme_light)))
			setTheme(R.style.ApolloTheme_Light);                 
		else if(type.equals(getResources().getString(R.string.theme_black)))
			setTheme(R.style.ApolloTheme_Black);
		else
			setTheme(R.style.ApolloTheme_Dark);
        // This needs to be called first
        super.onCreate(icicle);

        // Control Media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Bundle bundle = new Bundle();
        bundle.putString(MIME_TYPE, Audio.Playlists.CONTENT_TYPE);
        bundle.putLong(BaseColumns._ID, PLAYLIST_QUEUE);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new QuickQueueFragment(bundle)).commit();
    }
}
