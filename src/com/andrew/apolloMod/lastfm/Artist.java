/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers All rights
 * reserved. Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met: - Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following disclaimer. -
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. THIS SOFTWARE IS
 * PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.andrew.apolloMod.lastfm;

import android.content.Context;

import com.andrew.apolloMod.Config;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bean that contains artist information.<br/>
 * This class contains static methods that executes API methods relating to
 * artists.<br/>
 * Method names are equivalent to the last.fm API method names.
 * 
 * @author Janni Kovacs
 */
public class Artist extends MusicEntry {

    protected final static ItemFactory<Artist> FACTORY = new ArtistFactory();

    protected Artist(final String name, final String url) {
        super(name, url);
    }

    /**
     * Retrieves detailed artist info for the given artist or mbid entry.
     * 
     * @param artistOrMbid Name of the artist or an mbid
     * @param apiKey The API key
     * @return detailed artist info
     */
    public final static Artist getInfo(final Context context, final String artistOrMbid,
            final String apiKey) {
        return getInfo(context, artistOrMbid, Locale.getDefault(), apiKey);
    }

    /**
     * Retrieves detailed artist info for the given artist or mbid entry.
     * 
     * @param artistOrMbid Name of the artist or an mbid
     * @param locale The language to fetch info in, or <code>null</code>
     * @param username The username for the context of the request, or
     *            <code>null</code>. If supplied, the user's playcount for this
     *            artist is included in the response
     * @param apiKey The API key
     * @return detailed artist info
     */
    public final static Artist getInfo(final Context context, final String artistOrMbid,
            final Locale locale, final String apiKey) {
        final Map<String, String> mParams = new WeakHashMap<String, String>();
        mParams.put("artist", artistOrMbid);
        if (locale != null && locale.getLanguage().length() != 0) {
            mParams.put("lang", locale.getLanguage());
        }
        final Result mResult = Caller.getInstance(context).call("artist.getInfo", apiKey, mParams);
        return ResponseBuilder.buildItem(mResult, Artist.class);
    }

    /**
     * Use the last.fm corrections data to check whether the supplied artist has
     * a correction to a canonical artist. This method returns a new
     * {@link Artist} object containing the corrected data, or <code>null</code>
     * if the supplied Artist was not found.
     * 
     * @param artist The artist name to correct
     * @return a new {@link Artist}, or <code>null</code>
     */
    public final static Artist getCorrection(final Context context, final String artist) {
        Result result = null;
        try {
            result = Caller.getInstance(context).call("artist.getCorrection",
                    Config.LASTFM_API_KEY, "artist", artist);
            if (!result.isSuccessful()) {
                return null;
            }
            final DomElement correctionElement = result.getContentElement().getChild("correction");
            if (correctionElement == null) {
                return new Artist(artist, null);
            }
            final DomElement artistElem = correctionElement.getChild("artist");
            return FACTORY.createItemFromElement(artistElem);
        } catch (final Exception ignored) {
            return null;
        }
    }

    /**
     * Get {@link Image}s for this artist in a variety of sizes.
     * 
     * @param artistOrMbid The artist name in question
     * @return a list of {@link Image}s
     */
    public final static PaginatedResult<Image> getImages(final Context context,
            final String artistOrMbid) {
        return getImages(context, artistOrMbid, -1, -1, Config.LASTFM_API_KEY);
    }

    /**
     * Get {@link Image}s for this artist in a variety of sizes.
     * 
     * @param artistOrMbid The artist name in question
     * @param page Which page of limit amount to display
     * @param limit How many to return. Defaults and maxes out at 50
     * @param apiKey A Last.fm API key
     * @return a list of {@link Image}s
     */
    public final static PaginatedResult<Image> getImages(final Context context,
            final String artistOrMbid, final int page, final int limit, final String apiKey) {
        final Map<String, String> params = new WeakHashMap<String, String>();
        params.put("artist", artistOrMbid);
        MapUtilities.nullSafePut(params, "page", page);
        MapUtilities.nullSafePut(params, "limit", limit);
        Result result = null;
        try {
            result = Caller.getInstance(context).call("artist.getImages", apiKey, params);
        } catch (final Exception ignored) {
            return null;
        }
        return ResponseBuilder.buildPaginatedResult(result, Image.class);
    }

    private final static class ArtistFactory implements ItemFactory<Artist> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Artist createItemFromElement(final DomElement element) {
            if (element == null) {
                return null;
            }
            final Artist artist = new Artist(null, null);
            MusicEntry.loadStandardInfo(artist, element);
            return artist;
        }
    }
}
