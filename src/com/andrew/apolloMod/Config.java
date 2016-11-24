/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apolloMod;

/**
 * App-wide constants.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class Config {

    /* This class is never initiated. */
    public Config() {
    }

    /**
     * My personal Last.fm API key, please use your own.
     */
    public static final String LASTFM_API_KEY = "0bec3f7ec1f914d7c960c12a916c8fb3";

    /**
     * Used to distinguish album art from artist images
     */
    public static final String ALBUM_ART_SUFFIX = "album";

    /**
     * The ID of an artist, album, genre, or playlist passed to the profile
     * activity
     */
    public static final String ID = "id";

    /**
     * The name of an artist, album, genre, or playlist passed to the profile
     * activity
     */
    public static final String NAME = "name";

    /**
     * The name of an artist passed to the profile activity
     */
    public static final String ARTIST_NAME = "artist_name";

    /**
     * The year an album was released passed to the profile activity
     */
    public static final String ALBUM_YEAR = "album_year";

    /**
     * The MIME type passed to a the profile activity
     */
    public static final String MIME_TYPE = "mime_type";

    /**
     * Play from search intent
     */
    public static final String PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
}
