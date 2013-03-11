/**
 * 
 */

package com.andrew.apolloMod.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apolloMod.R;

/**
 * @author Andrew Neal
 */
public class ViewHolderQueue {

    public final ImageView mArtistImage, mPeakOne, mPeakTwo;

    public final ImageView mAlbumArt;

    public final TextView mTrackName;

    public ViewHolderQueue(View view) {
        mArtistImage = (ImageView)view.findViewById(R.id.queue_artist_image);
        mAlbumArt = (ImageView)view.findViewById(R.id.queue_album_art);
        mTrackName = (TextView)view.findViewById(R.id.queue_track_name);
        mPeakOne = (ImageView)view.findViewById(R.id.peak_one);
        mPeakTwo = (ImageView)view.findViewById(R.id.peak_two);
    }

}
