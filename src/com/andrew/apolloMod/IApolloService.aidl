package com.andrew.apolloMod;

import android.graphics.Bitmap;

interface IApolloService
{
    void openFile(String path);
    void open(in long [] list, int position);
    long getIdFromPath(String path);
    int getQueuePosition();
    boolean isPlaying();
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    long duration();
    long position();
    long seek(long pos);
    String getTrackName();
    String getAlbumName();
    long getAlbumId();
    Bitmap getAlbumBitmap();
    String getArtistName();
    long getArtistId();
    void enqueue(in long [] list, int action);
    long [] getQueue();
    void setQueuePosition(int index);
    String getPath();
    long getAudioId();
    void setShuffleMode(int shufflemode);
    void notifyChange(String what);
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    void setRepeatMode(int repeatmode);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
	void addToFavorites(long id);
	void removeFromFavorites(long id);
	boolean isFavorite(long id);
    void toggleFavorite();
}

