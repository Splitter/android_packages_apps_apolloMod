
package com.andrew.apolloMod.ui.adapters;

import static com.andrew.apolloMod.Constants.EXTERNAL;
import static com.andrew.apolloMod.Constants.SIZE_THUMB;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;
import static com.andrew.apolloMod.Constants.TYPE_ARTIST;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.fragments.list.PlaylistListFragment;
import com.andrew.apolloMod.ui.fragments.list.TracksFragment;
import com.andrew.apolloMod.views.ViewHolderList;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

/**
 * @author Andrew Neal
 */
public class PlaylistListAdapter extends SimpleDragSortCursorAdapter {

	private AnimationDrawable mPeakOneAnimation, mPeakTwoAnimation;

    private WeakReference<ViewHolderList> holderReference;
    
    private Context mContext;
    
    private ImageProvider mImageProvider;
    
    private long mPlaylistId;

    public PlaylistListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, long id) {
        super(context, layout, c, from, to, flags);
        mPlaylistId = id;
    	mContext = context;
    	mImageProvider = ImageProvider.getInstance( (Activity) mContext );
    }

    @Override
    public void drop(int from, int to) {
    	super.drop(from, to);
        if (from != to && mPlaylistId >= 0) {
        	try{
                Playlists.Members.moveItem(mContext.getContentResolver(),mPlaylistId, from, to);
            }catch(Exception e){
                Log.e("FAILED", e.getMessage());
            }
        }
    }
    
    @Override
    public void remove(int which) {


        int cursorPos = getCursorPosition(which);
        mCursor.moveToPosition(cursorPos);
        long id = mCursor.getLong(PlaylistListFragment.mMediaIdIndex);
        String mName = mCursor.getString(PlaylistListFragment.mTitleIndex);
        if (mPlaylistId >= 0) {
            Uri uri = Playlists.Members.getContentUri(EXTERNAL, mPlaylistId);
            mContext.getContentResolver().delete(uri, Playlists.Members.AUDIO_ID + "=" + id,
                    null);
            String message = mContext.getString(R.string.track_removed_from_playlist, mName);
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    	super.remove(which);
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
        String trackName = mCursor.getString(PlaylistListFragment.mTitleIndex);
        viewholder.mViewHolderLineOne.setText(trackName);

        // Artist name
        String artistName = mCursor.getString(PlaylistListFragment.mArtistIndex);
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
        long audioid = mCursor.getLong(TracksFragment.mMediaIdIndex);
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
