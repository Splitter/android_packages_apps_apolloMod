/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.andrew.apolloMod.helpers.lastfm;

import java.util.HashMap;
import java.util.Map;

import com.andrew.apolloMod.helpers.DomElement;
import com.andrew.apolloMod.helpers.utils.StringUtilities;


/**
 * Wrapper class for Album related API calls and Album Bean.
 * 
 * @author Janni Kovacs
 */
public class Album extends MusicEntry {

    static final ItemFactory<Album> FACTORY = new AlbumFactory();

    private String artist;

    private Album(String name, String url, String artist) {
        super(name, url);
        this.artist = artist;
    }

    private Album(String name, String url, String mbid, int playcount, int listeners,
            boolean streamable, String artist) {
        super(name, url, mbid, playcount, listeners, streamable);
        this.artist = artist;
    }

    public String getArtist() {
        return artist;
    }

    /**
     * Get the metadata for an album on Last.fm using the album name or a
     * musicbrainz id. See playlist.fetch on how to get the album playlist.
     * 
     * @param artist Artist's name
     * @param albumOrMbid Album name or MBID
     * @param apiKey The API key
     * @return Album metadata
     */
    public static Album getInfo(String artist, String albumOrMbid, String apiKey) {
        return getInfo(artist, albumOrMbid, null, apiKey);
    }

    /**
     * Get the metadata for an album on Last.fm using the album name or a
     * musicbrainz id. See playlist.fetch on how to get the album playlist.
     * 
     * @param artist Artist's name
     * @param albumOrMbid Album name or MBID
     * @param username The username for the context of the request. If supplied,
     *            the user's playcount for this album is included in the
     *            response.
     * @param apiKey The API key
     * @return Album metadata
     */
    public static Album getInfo(String artist, String albumOrMbid, String username, String apiKey) {
        Map<String, String> params = new HashMap<String, String>();
        if (StringUtilities.isMbid(albumOrMbid)) {
            params.put("mbid", albumOrMbid);
        } else {
            params.put("artist", artist);
            params.put("album", albumOrMbid);
        }
        if (username != null) {
        	params.put("username", username);
		}
        Result result = Caller.getInstance().call("album.getInfo", apiKey, params);
        return ResponseBuilder.buildItem(result, Album.class);
    }

    private static class AlbumFactory implements ItemFactory<Album> {
        @Override
        public Album createItemFromElement(DomElement element) {
            Album album = new Album(null, null, null);
            MusicEntry.loadStandardInfo(album, element);
            if (element.hasChild("artist")) {
                album.artist = element.getChild("artist").getChildText("name");
                if (album.artist == null)
                    album.artist = element.getChildText("artist");
            }
            return album;
        }
    }
}
