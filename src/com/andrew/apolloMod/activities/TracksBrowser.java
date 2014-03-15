/**
 * 
 */

package com.andrew.apolloMod.activities;

import android.app.SearchManager;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.andrew.apolloMod.IApolloService;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.adapters.PagerAdapter;
import com.andrew.apolloMod.ui.fragments.BottomActionBarFragment;
import com.andrew.apolloMod.ui.fragments.list.AlbumListFragment;
import com.andrew.apolloMod.ui.fragments.list.ArtistAlbumsFragment;
import com.andrew.apolloMod.ui.fragments.list.ArtistListFragment;
import com.andrew.apolloMod.ui.fragments.list.GenreListFragment;
import com.andrew.apolloMod.ui.fragments.list.PlaylistListFragment;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.service.ServiceToken;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import static com.andrew.apolloMod.Constants.*;

/**
 * @author Andrew Neal
 * @Note This displays specific track or album listings
 */
public class TracksBrowser extends FragmentActivity implements ServiceConnection {
	
	 // Bundle
    private Bundle bundle;

    private Intent intent;

    private String mimeType;

    private ServiceToken mToken;
    
    private int RESULT_LOAD_IMAGE = 1;
    
    private ImageProvider mImageProvider;
    
    private ViewPager mViewPager = null;
    
    private ImageButton mChangeButton;
    
    private SlidingUpPanelLayout mPanel;
    
    BottomActionBarFragment mBActionbar;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        
        // Landscape mode on phone isn't ready
        if (!ApolloUtils.isTablet(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Control Media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Layout
        setContentView(R.layout.track_browser);
        registerForContextMenu(findViewById(R.id.half_artist_image));

        mBActionbar =(BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottomactionbar_new);
        mBActionbar.setUpQueueSwitch(this);
        
        mPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mPanel.setAnchorPoint(0);        
        mPanel.setDragView(findViewById(R.id.bottom_action_bar_dragview));
        mPanel.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
        mPanel.setAnchorPoint(0.0f);
        mPanel.setPanelSlideListener(new PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (slideOffset < 0.2) {
                    mBActionbar.onExpanded();
                    if (getActionBar().isShowing()) {
                        getActionBar().hide();
                    }
                } else {
                    mBActionbar.onCollapsed();
                    if (!getActionBar().isShowing()) {
                        getActionBar().show();
                    }
                }
            }
            @Override
            public void onPanelExpanded(View panel) {}
            @Override
            public void onPanelCollapsed(View panel) {}
            @Override
            public void onPanelAnchored(View panel) {}
        });
        //ImageCache
    	mImageProvider = ImageProvider.getInstance( this );
        // Important!
        whatBundle(icicle);
        // Update the colorstrip color
        initColorstrip();
        // Update the ActionBar
        initActionBar();
        // Update the half_and_half layout
        initUpperHalf();
        // Important!
        initPager();
    }
    

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    	if(mPanel.isExpanded()){
            mPanel.collapsePane();
    	}
    	else{
    		super.onBackPressed();
    	}
    }    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    	if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
    		
        	menu.setHeaderTitle(R.string.image_edit_artists);
        	getMenuInflater().inflate(R.menu.context_artistimage, menu); 
        	
        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
        	
        	menu.setHeaderTitle(R.string.image_edit_albums);
        	getMenuInflater().inflate(R.menu.context_albumimage, menu); 
        	
        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {        	
        	menu.setHeaderTitle(R.string.image_edit_playlist);
        	getMenuInflater().inflate(R.menu.context_playlist_genreimage, menu);         	
        }
        else{        	
        	menu.setHeaderTitle(R.string.image_edit_genre);
        	getMenuInflater().inflate(R.menu.context_playlist_genreimage, menu);        	
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	ImageInfo mInfo = null;      
        switch (item.getItemId()) {
            case R.id.image_edit_gallery:
            	Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            	startActivityForResult(i, RESULT_LOAD_IMAGE);
            	return true;
            case R.id.image_edit_file:            	
                mInfo = new ImageInfo();
                mInfo.type = TYPE_ALBUM;
                mInfo.size = SIZE_NORMAL;
                mInfo.source = SRC_FILE;
                mInfo.data = new String[]{ getAlbumId(), getArtist(), getAlbum() };                
                mImageProvider.loadImage((ImageView)findViewById(R.id.half_artist_image), mInfo );
                return true;
            case R.id.image_edit_lastfm:           	
                mInfo = new ImageInfo();
                mInfo.size = SIZE_NORMAL;
                mInfo.source = SRC_LASTFM;                
    	        if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) { 
                    mInfo.type = TYPE_ARTIST;
                    mInfo.data = new String[]{ getArtist() };
    	        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
                    mInfo.type = TYPE_ALBUM;
                    mInfo.data = new String[]{ getAlbumId(), getArtist(), getAlbum() };
    	        } 
                mImageProvider.loadImage((ImageView)findViewById(R.id.half_artist_image), mInfo );
                return true;
            case R.id.image_edit_web:
            	onSearchWeb();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    public void onSearchWeb(){
    	String query = "";
    	if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
    		query = getArtist();
        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
        	query = getAlbum() + " " + getArtist();
        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
        	query = bundle.getString(PLAYLIST_NAME);
        }
        else{
            Long id = bundle.getLong(BaseColumns._ID);
            query = MusicUtils.parseGenreName(this, MusicUtils.getGenreName(this, id, true));
        }
        final Intent googleSearch = new Intent(Intent.ACTION_WEB_SEARCH);
        googleSearch.putExtra(SearchManager.QUERY, query);
        startActivity(googleSearch);	
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == FragmentActivity.RESULT_OK && requestCode == RESULT_LOAD_IMAGE  && data != null)
	    {
        	Uri selectedImage = data.getData();
	        String[] filePathColumn = { MediaStore.Images.Media.DATA };
	        Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
	        cursor.moveToFirst();
	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        String picturePath = cursor.getString(columnIndex);
	        cursor.close();
        	
            ImageInfo mInfo = new ImageInfo();
	        if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) { 
	            mInfo.type = TYPE_ARTIST;
	            mInfo.data = new String[]{ getArtist(), picturePath };    
	        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
	            mInfo.type = TYPE_ALBUM;
	            mInfo.data = new String[]{ getAlbumId(), getAlbum(), getArtist(), picturePath };
	        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
	            mInfo.type = TYPE_PLAYLIST;
	            mInfo.data = new String[]{ bundle.getString(PLAYLIST_NAME), picturePath };
	        }
	        else{ 
	        	Long id = bundle.getLong(BaseColumns._ID);
	            mInfo.type = TYPE_GENRE;
	            mInfo.data = new String[]{  MusicUtils.parseGenreName(this, MusicUtils.getGenreName(this, id, true)), picturePath };
	        }
	        
            mInfo.size = SIZE_NORMAL;
            mInfo.source = SRC_GALLERY;          
            mImageProvider.loadImage((ImageView)findViewById(R.id.half_artist_image), mInfo );
	        
	    }
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putAll(bundle);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder obj) {
        MusicUtils.mService = IApolloService.Stub.asInterface(obj);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        MusicUtils.mService = null;
    }

    /**
     * Update next BottomActionBar as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
        }
    };

    @Override
    protected void onStart() {
        // Bind to Service
        mToken = MusicUtils.bindToService(this, this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.META_CHANGED);
        registerReceiver(mMediaStatusReceiver, filter);
        setTitle();
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unbind
        if (MusicUtils.mService != null)
            MusicUtils.unbindFromService(mToken);
        unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @param icicle
     * @return what Bundle we're dealing with
     */
    public void whatBundle(Bundle icicle) {
        intent = getIntent();
        bundle = icicle != null ? icicle : intent.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        if (bundle.getString(INTENT_ACTION) == null) {
            bundle.putString(INTENT_ACTION, intent.getAction());
        }
        if (bundle.getString(MIME_TYPE) == null) {
            bundle.putString(MIME_TYPE, intent.getType());
        }
        mimeType = bundle.getString(MIME_TYPE);
    }

    /**
     * For the theme chooser
     */
    private void initColorstrip() {

        RelativeLayout mColorstrip2 = (RelativeLayout)findViewById(R.id.bottom_colorstrip);
        mColorstrip2.setBackgroundColor(getResources().getColor(R.color.holo_blue_dark));
    }

    /**
     * Set the ActionBar title
     */
    private void initActionBar() {
        ApolloUtils.showUpTitleOnly(getActionBar());
    }

    private void onToggleButton(){
    	if(mViewPager!=null){
    		int cur = mViewPager.getCurrentItem();
    		if(cur == 0){
    			mChangeButton.setImageResource(R.drawable.view_more_song);
    			mViewPager.setCurrentItem(1);
    			TextView lineTwoView = (TextView)findViewById(R.id.half_artist_image_text_line_two);
    	        String lineTwo = MusicUtils.makeAlbumsLabel(this, 0, Integer.parseInt(getNumSongs()), true);
    			lineTwoView.setText(lineTwo);
    		}else{
    			mChangeButton.setImageResource(R.drawable.view_more_album);
    			mViewPager.setCurrentItem(0);
    			TextView lineTwoView = (TextView)findViewById(R.id.half_artist_image_text_line_two);
    			String lineTwo = MusicUtils.makeAlbumsLabel(this, Integer.parseInt(getNumAlbums()), 0, false);
    	        lineTwoView.setText(lineTwo);
    		}
    	}    	
    }
    
    /**
     * Sets up the @half_and_half.xml layout
     */
    private void initUpperHalf() {
    	ImageInfo mInfo = new ImageInfo();
    	mInfo.source = SRC_FIRST_AVAILABLE;
        mInfo.size = SIZE_NORMAL;
    	final ImageView imageView = (ImageView)findViewById(R.id.half_artist_image);
    	String lineOne = "";
    	String lineTwo = "";

        if (ApolloUtils.isArtist(mimeType)) {
        	mChangeButton = (ImageButton)findViewById(R.id.view_more);
        	mChangeButton.setVisibility(View.VISIBLE);
        	mChangeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                	onToggleButton();
                }
            });
        	String mArtist = getArtist();
            mInfo.type = TYPE_ARTIST;
            mInfo.data = new String[]{ mArtist };  
            lineOne = mArtist;
            lineTwo = MusicUtils.makeAlbumsLabel(this, Integer.parseInt(getNumAlbums()), 0, false);
        }else if (ApolloUtils.isAlbum(mimeType)) {
        	String mAlbum = getAlbum(), mArtist = getArtist();
            mInfo.type = TYPE_ALBUM;
            mInfo.data = new String[]{ getAlbumId(), mAlbum, mArtist };                
            lineOne = mAlbum;
            lineTwo = mArtist;
        } else if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
        	String plyName = bundle.getString(PLAYLIST_NAME);
        	mInfo.type = TYPE_PLAYLIST;
            mInfo.data = new String[]{ plyName };               
            lineOne = plyName;
        }
        else{ 
        	String genName = MusicUtils.parseGenreName(this,
        			MusicUtils.getGenreName(this, bundle.getLong(BaseColumns._ID), true));
        	mInfo.type = TYPE_GENRE;
            mInfo.size = SIZE_NORMAL;
            mInfo.data = new String[]{ genName };             
            lineOne = genName;
        }

        mImageProvider.loadImage( imageView, mInfo );        
        TextView lineOneView = (TextView)findViewById(R.id.half_artist_image_text);
        lineOneView.setText(lineOne);
        TextView lineTwoView = (TextView)findViewById(R.id.half_artist_image_text_line_two);
        lineTwoView.setText(lineTwo);
    }

    /**
     * Initiate ViewPager and PagerAdapter
     */
    private void initPager() {
        // Initiate PagerAdapter
        PagerAdapter mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        if (ApolloUtils.isArtist(mimeType))
            // Show all albums for an artist
            mPagerAdapter.addFragment(new ArtistAlbumsFragment(bundle));
        // Show the tracks for an artist or album
        if(Playlists.CONTENT_TYPE.equals(mimeType)){
            mPagerAdapter.addFragment(new PlaylistListFragment(bundle));
        }
        else if(Genres.CONTENT_TYPE.equals(mimeType)){
        	mPagerAdapter.addFragment(new GenreListFragment(bundle));
        }
        else if(ApolloUtils.isArtist(mimeType)){
        	mPagerAdapter.addFragment(new ArtistListFragment(bundle));
        }
        else if(Audio.Albums.CONTENT_TYPE.equals(mimeType)){
        	mPagerAdapter.addFragment(new AlbumListFragment(bundle));
        }

        // Set up ViewPager
        mViewPager = (ViewPager)findViewById(R.id.viewPager);
        mViewPager.setPageMargin(getResources().getInteger(R.integer.viewpager_margin_width));
        mViewPager.setPageMarginDrawable(R.drawable.viewpager_margin);
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(new PageListener());
    }

    private class PageListener extends SimpleOnPageChangeListener{
        public void onPageSelected(int cur) {            
        	if(cur == 0){
    			ImageButton mCButton = (ImageButton)findViewById(R.id.view_more);
    			mCButton.setImageResource(R.drawable.view_more_album);
    			TextView lineTwoView = (TextView)findViewById(R.id.half_artist_image_text_line_two);
    			String lineTwo = MusicUtils.makeAlbumsLabel(TracksBrowser.this, Integer.parseInt(getNumAlbums()), 0, false);
    	        lineTwoView.setText(lineTwo);
    		}else{
            	ImageButton mCButton = (ImageButton)findViewById(R.id.view_more);
    			mCButton.setImageResource(R.drawable.view_more_song);
    			TextView lineTwoView = (TextView)findViewById(R.id.half_artist_image_text_line_two);
    	        String lineTwo = MusicUtils.makeAlbumsLabel(TracksBrowser.this, 0, Integer.parseInt(getNumSongs()), true);
    			lineTwoView.setText(lineTwo);
    		}
	    }
	}
    
    /**
     * @return artist name from Bundle
     */
    public String getArtist() {
        if (bundle.getString(ARTIST_KEY) != null)
            return bundle.getString(ARTIST_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return album name from Bundle
     */
    public String getAlbum() {
        if (bundle.getString(ALBUM_KEY) != null)
            return bundle.getString(ALBUM_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return album name from Bundle
     */
    public String getAlbumId() {
        if (bundle.getString(ALBUM_ID_KEY) != null)
            return bundle.getString(ALBUM_ID_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return number of albums from Bundle
     */
    public String getNumSongs() {
    	String[] projection = {
                BaseColumns._ID, ArtistColumns.ARTIST, ArtistColumns.NUMBER_OF_TRACKS
        };
    	Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;        
        Long id = ApolloUtils.getArtistId(getArtist(), ARTIST_ID, this);
        Cursor cursor = null;
        try{
        	cursor = this.getContentResolver().query(uri, projection, BaseColumns._ID+ "=" + DatabaseUtils.sqlEscapeString(String.valueOf(id)), null, null);
        }
        catch(Exception e){
        	e.printStackTrace();        	
        }
        if(cursor == null)
        	return String.valueOf(0);
        int mArtistNumAlbumsIndex = cursor.getColumnIndexOrThrow(ArtistColumns.NUMBER_OF_TRACKS);
        if(cursor.getCount()>0){
	    	cursor.moveToFirst();
	        String numAlbums = cursor.getString(mArtistNumAlbumsIndex);	
			cursor.close();
	        if(numAlbums != null){
	        	return numAlbums;
	        }
        }        
        return String.valueOf(0);
    }
    
    /**
     * @return number of albums from Bundle
     */
    public String getNumAlbums() {
        if (bundle.getString(NUMALBUMS) != null)
            return bundle.getString(NUMALBUMS);
        String[] projection = {
                BaseColumns._ID, ArtistColumns.ARTIST, ArtistColumns.NUMBER_OF_ALBUMS
        };
        Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;        
        Long id = ApolloUtils.getArtistId(getArtist(), ARTIST_ID, this);
        Cursor cursor = null;
        try{
        	cursor = this.getContentResolver().query(uri, projection, BaseColumns._ID+ "=" + DatabaseUtils.sqlEscapeString(String.valueOf(id)), null, null);
        }
        catch(Exception e){
        	e.printStackTrace();        	
        }
        if(cursor == null)
        	return String.valueOf(0);
        int mArtistNumAlbumsIndex = cursor.getColumnIndexOrThrow(ArtistColumns.NUMBER_OF_ALBUMS);
        if(cursor.getCount()>0){
	    	cursor.moveToFirst();
	        String numAlbums = cursor.getString(mArtistNumAlbumsIndex);	
			cursor.close();
	        if(numAlbums != null){
	        	return numAlbums;
	        }
        }        
        return String.valueOf(0);
    }

    /**
     * @return genre name from Bundle
     */
    public String getGenre() {
        if (bundle.getString(GENRE_KEY) != null)
            return bundle.getString(GENRE_KEY);
        return getResources().getString(R.string.app_name);
    }

    /**
     * @return playlist name from Bundle
     */
    public String getPlaylist() {
        if (bundle.getString(PLAYLIST_NAME) != null)
            return bundle.getString(PLAYLIST_NAME);
        return getResources().getString(R.string.app_name);
    }

    /**
     * Set the correct title
     */
    private void setTitle() {
        String name;
        long id;
        if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            switch ((int)id) {
                case (int)PLAYLIST_QUEUE:
                    setTitle(R.string.nowplaying);
                    return;
                case (int)PLAYLIST_FAVORITES:
                    setTitle(R.string.favorite);
                    return;
                default:
                    if (id < 0) {
                        setTitle(R.string.app_name);
                        return;
                    }
            }
            name = MusicUtils.getPlaylistName(this, id);
        } else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name =  getString (R.string.artist_page_title)+MusicUtils.getArtistName(this, id, true);
        } else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name =  getString (R.string.album_page_title)+MusicUtils.getAlbumName(this, id, true);
        } else if (Audio.Genres.CONTENT_TYPE.equals(mimeType)) {
            id = bundle.getLong(BaseColumns._ID);
            name = MusicUtils.parseGenreName(this, MusicUtils.getGenreName(this, id, true));
        } else {
            setTitle(R.string.app_name);
            return;
        }
        setTitle(name);
    }
}
