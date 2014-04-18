
package com.andrew.apolloMod.ui.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.MusicUtils;
import com.andrew.apolloMod.providers.RecentsLoader;
import com.andrew.apolloMod.ui.fragments.list.RecentlyAddedFragment;
import com.andrew.apolloMod.ui.fragments.list.RecentlyPlayedFragment;

import static com.andrew.apolloMod.Constants.SIZE_THUMB;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;

public class RecentsSplitFragment  extends Fragment implements LoaderCallbacks<Cursor>{
	
	private LinearLayout mWrapper;
	
	private Cursor mCursor;
	
    private ImageProvider mImageProvider;
	
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    	mImageProvider = ImageProvider.getInstance( (Activity) getActivity() );
    }    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.recents_main, container, false);
        mWrapper = (LinearLayout )root.findViewById(R.id.recents_wrapper);
        return root;
    }

	@Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {    
        return new RecentsLoader(getActivity());
    }

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mWrapper.removeAllViews();
		mWrapper.addView(View.inflate(getActivity(), R.layout.shadow, null));

		mCursor = cursor;
		View row = null;
		mCursor.moveToPosition(-1);
		int count = 0;
		boolean added = false;
		row = View.inflate(getActivity(), R.layout.recently_added_heading, null);
		View wrapper = row.findViewById(R.id.recently_added_wrapper);
		wrapper.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					FragmentTransaction trans = getFragmentManager()
					.beginTransaction();
					trans.replace(R.id.root_frame, new RecentlyAddedFragment());
					trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
					trans.addToBackStack(null);
					trans.commit();
				}
			});
		mWrapper.addView(row);
		
		while (mCursor.moveToNext()) {
			if(mCursor.getString(mCursor.getColumnIndex(BaseColumns._ID)).equals("-1")){
				count = 0;
				if(!added){
					mWrapper.addView(row);
					added = true;
				}
				row = View.inflate(getActivity(), R.layout.recently_added_heading, null);
				((TextView)row.findViewById(R.id.recents_textview)).setText(R.string.recents_played);
				wrapper = row.findViewById(R.id.recently_added_wrapper);
				wrapper.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							FragmentTransaction trans = getFragmentManager()
							.beginTransaction();
							trans.replace(R.id.root_frame, new RecentlyPlayedFragment());
							trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
							trans.addToBackStack(null);
							trans.commit();
						}
					});
				
				
				
				mWrapper.addView(row);									
				continue;
			}
			if (count % 2 == 0) {
				row = View.inflate(getActivity(), R.layout.recents_row, null);
				View col1 = row.findViewById(R.id.recent_column1);
				col1.setVisibility(View.VISIBLE);
				col1.setTag(mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID)));
				col1.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						MusicUtils.addToCurrentPlaylist(getActivity(), new long[]{(Long)v.getTag()});
					}
				});
				TextView lineOne = (TextView) row.findViewById(R.id.recent_line_one1);
				TextView lineTwo = (TextView) row.findViewById(R.id.recent_line_two1);
				lineOne.setText(mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE)));
				lineTwo.setText(mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST)));
				added = false;
				
				String artistName = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
				String albumName = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM));
		        String albumId = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID));
		        ImageInfo mInfo = new ImageInfo();
	            mInfo.type = TYPE_ALBUM;
	            mInfo.size = SIZE_THUMB;
	            mInfo.source = SRC_FIRST_AVAILABLE;
	            mInfo.data = new String[]{ albumId , artistName, albumName };
	            mImageProvider.loadImage( (ImageView) row.findViewById(R.id.recent_image1), mInfo ); 
			}	
			else{
				View col2 = row.findViewById(R.id.recent_column2);
				col2.setVisibility(View.VISIBLE);
				col2.setTag(mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID)));
				col2.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						MusicUtils.addToCurrentPlaylist(getActivity(), new long[]{(Long)v.getTag()});
					}
				});
				TextView lineOne = (TextView) row.findViewById(R.id.recent_line_one2);
				TextView lineTwo = (TextView) row.findViewById(R.id.recent_line_two2);
				lineOne.setText(mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE)));
				lineTwo.setText(mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST)));
				mWrapper.addView(row);
				added = true;
				
				String artistName = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
				String albumName = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM));
		        String albumId = mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID));
		        ImageInfo mInfo = new ImageInfo();
	            mInfo.type = TYPE_ALBUM;
	            mInfo.size = SIZE_THUMB;
	            mInfo.source = SRC_FIRST_AVAILABLE;
	            mInfo.data = new String[]{ albumId , artistName, albumName };
	            mImageProvider.loadImage( (ImageView) row.findViewById(R.id.recent_image2), mInfo ); 
			}
			count++;
		}	
		if(!added){
			mWrapper.addView(row);
		}
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		
	}
}
