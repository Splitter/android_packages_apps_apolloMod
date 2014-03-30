/**
 * 
 */

package com.andrew.apolloMod.ui.widgets;

import static com.andrew.apolloMod.Constants.SIZE_THUMB;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.activities.QuickQueue;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.MusicUtils;

/**
 * @author Andrew Neal
 */
public class BottomActionBar extends LinearLayout implements OnLongClickListener {
	 
    public BottomActionBar(Context context) {
        super(context);
    }

    public BottomActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnLongClickListener(this);
    }

    public BottomActionBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Updates the bottom ActionBar's info
     * 
     * @param activity
     * @throws RemoteException
     */
    public void updateBottomActionBar(Activity activity) {
        View bottomActionBar = activity.findViewById(R.id.bottom_action_bar);
        if (bottomActionBar == null) {
            return;
        }

        if (MusicUtils.mService != null && MusicUtils.getCurrentAudioId() != -1) {
        	ImageButton mFavs = (ImageButton)bottomActionBar
        			.findViewById(R.id.bottom_action_bar_favorites);
        	if(MusicUtils.isFavorite(activity, MusicUtils.getCurrentAudioId())){
        		mFavs.setImageResource(R.drawable.apollo_holo_light_favorite_selected);
        	}
        	else{
            	Theme theme = activity.getTheme();
        		TypedValue typedvalueattr = new TypedValue();
        		theme.resolveAttribute(R.attr.AudioFavoritesButton, typedvalueattr, true); 
        		mFavs.setImageResource(typedvalueattr.resourceId);
        	}
            
            // Track name
            TextView mTrackName = (TextView)bottomActionBar
                    .findViewById(R.id.bottom_action_bar_track_name);
            mTrackName.setText(MusicUtils.getTrackName());
            
            // Track name
            TextView mTrackNameOnly = (TextView)bottomActionBar
                    .findViewById(R.id.bottom_action_bar_track_name_only);
            mTrackNameOnly.setText(MusicUtils.getTrackName());

            // Artist name
            TextView mArtistName = (TextView)bottomActionBar
                    .findViewById(R.id.bottom_action_bar_artist_name);
            mArtistName.setText(MusicUtils.getArtistName());

            // Album art
            ImageView mAlbumArt = (ImageView)bottomActionBar
                    .findViewById(R.id.bottom_action_bar_album_art);
            

            ImageInfo mInfo = new ImageInfo();
            mInfo.type = TYPE_ALBUM;
            mInfo.size = SIZE_THUMB;
            mInfo.source = SRC_FIRST_AVAILABLE;
            mInfo.data = new String[]{ String.valueOf(MusicUtils.getCurrentAlbumId()) , MusicUtils.getArtistName(), MusicUtils.getAlbumName() };
            
            ImageProvider.getInstance( activity ).loadImage( mAlbumArt , mInfo );
            
        }
    }

    @Override
    public boolean onLongClick(View v) {
        Context context = v.getContext();
        context.startActivity(new Intent(context, QuickQueue.class));
        return true;
    }
    
}
