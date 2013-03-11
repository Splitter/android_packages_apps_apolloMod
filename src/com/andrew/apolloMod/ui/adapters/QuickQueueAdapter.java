
package com.andrew.apolloMod.ui.adapters;

import static com.andrew.apolloMod.Constants.SIZE_THUMB;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import static com.andrew.apolloMod.Constants.TYPE_ARTIST;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.ui.fragments.grid.QuickQueueFragment;
import com.andrew.apolloMod.views.ViewHolderQueue;

import java.lang.ref.WeakReference;

/**
 * @author Andrew Neal
 */
public class QuickQueueAdapter extends SimpleCursorAdapter {

    private WeakReference<ViewHolderQueue> holderReference;

    private Context mContext;
    
    private ImageProvider mImageProvider;
    
    public QuickQueueAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
    	mContext = context;
    	mImageProvider = ImageProvider.getInstance( (Activity) mContext );
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);

        Cursor mCursor = (Cursor) getItem(position);
        // ViewHolderQueue
        final ViewHolderQueue viewholder;

        if (view != null) {

            viewholder = new ViewHolderQueue(view);
            holderReference = new WeakReference<ViewHolderQueue>(viewholder);
            view.setTag(holderReference.get());

        } else {
            viewholder = (ViewHolderQueue)convertView.getTag();
        }

        // Artist Name
        String artistName = mCursor.getString(QuickQueueFragment.mArtistIndex);

        // Album name
        String albumName = mCursor.getString(QuickQueueFragment.mAlbumIndex);

        // Track name
        String trackName = mCursor.getString(QuickQueueFragment.mTitleIndex);
        holderReference.get().mTrackName.setText(trackName);

        // Album ID
        String albumId = mCursor.getString(QuickQueueFragment.mAlbumIdIndex);

        ImageInfo mInfo = new ImageInfo();
        mInfo.type = TYPE_ALBUM;
        mInfo.size = SIZE_THUMB;
        mInfo.source = SRC_FIRST_AVAILABLE;
        mInfo.data = new String[]{ albumId , artistName, albumName };        
        mImageProvider.loadImage( viewholder.mAlbumArt, mInfo );

        mInfo = new ImageInfo();
        mInfo.type = TYPE_ARTIST;
        mInfo.size = SIZE_THUMB;
        mInfo.source = SRC_FIRST_AVAILABLE;
        mInfo.data = new String[]{ artistName };
        mImageProvider.loadImage( viewholder.mArtistImage, mInfo );

        return view;
    }
}
