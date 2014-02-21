
package com.andrew.apolloMod.ui.adapters;

import static com.andrew.apolloMod.Constants.SIZE_THUMB;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import static com.andrew.apolloMod.Constants.TYPE_ARTIST;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.fragments.list.SongsFragment;
import com.andrew.apolloMod.ui.fragments.list.TracksFragment;
import com.andrew.apolloMod.views.ViewHolderList;

/**
 * @author Andrew Neal
 */
public class SonglistAdapter extends SimpleCursorAdapter {

    private AnimationDrawable mPeakOneAnimation, mPeakTwoAnimation;

    private WeakReference<ViewHolderList> holderReference;
    
    private Context mContext;
    
    private ImageProvider mImageProvider;

    public SonglistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    	mContext = context;
    	mImageProvider = ImageProvider.getInstance( (Activity) mContext );
    }

    /**
     * Used to quickly our the ContextMenu
     */
    private final View.OnClickListener showContextMenu = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.showContextMenu();
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);

        Cursor mCursor = (Cursor) getItem(position);
        // ViewHolderList
        ViewHolderList viewholder;

        if (view != null) {

            viewholder = new ViewHolderList(view);
            holderReference = new WeakReference<ViewHolderList>(viewholder);
            view.setTag(holderReference.get());

        } else {
            viewholder = (ViewHolderList)convertView.getTag();
        }

        // Track name
        String trackName = mCursor.getString(SongsFragment.mTitleIndex);
        viewholder.mViewHolderLineOne.setText(trackName);

        // Artist name
        String artistName = mCursor.getString(SongsFragment.mArtistIndex);
        holderReference.get().mViewHolderLineTwo.setText(artistName);
        
        ImageInfo mInfo = new ImageInfo();
        mInfo.type = TYPE_ARTIST;
        mInfo.size = SIZE_THUMB;
        mInfo.source = SRC_FIRST_AVAILABLE;
        mInfo.data = new String[]{ artistName };
        
        mImageProvider.loadImage( viewholder.mViewHolderImage, mInfo );

        holderReference.get().mQuickContext.setOnClickListener(showContextMenu);

        // Now playing indicator
        long currentaudioid = MusicUtils.getCurrentAudioId();
        long audioid = mCursor.getLong(SongsFragment.mMediaIdIndex);
        if (currentaudioid == audioid) {
            holderReference.get().mPeakOne.setImageResource(R.anim.peak_meter_1);
            holderReference.get().mPeakTwo.setImageResource(R.anim.peak_meter_2);
            mPeakOneAnimation = (AnimationDrawable)holderReference.get().mPeakOne.getDrawable();
            mPeakTwoAnimation = (AnimationDrawable)holderReference.get().mPeakTwo.getDrawable();
            try {
                if (MusicUtils.mService.isPlaying()) {
                    mPeakOneAnimation.start();
                    mPeakTwoAnimation.start();
                } else {
                    mPeakOneAnimation.stop();
                    mPeakTwoAnimation.stop();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            holderReference.get().mPeakOne.setImageResource(0);
            holderReference.get().mPeakTwo.setImageResource(0);
        }
        return view;
    }
}
