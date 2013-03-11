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

import java.util.ArrayList;
import java.util.Collection;

import com.andrew.apolloMod.helpers.DomElement;


/**
 * <code>MusicEntry</code> is the abstract superclass for {@link Track},
 * {@link Artist} and {@link Album}. It encapsulates data and provides methods
 * used in all subclasses, for example: name, playcount, images and more.
 * 
 * @author Janni Kovacs
 */
public abstract class MusicEntry extends ImageHolder {

    protected String name;

    protected String url;

    protected String mbid;

    protected String id;

    /**
     * This property is only available on hype charts, like
     * {@link Chart#getHypedArtists(String)} or
     * {@link de.umass.lastfm.Group#getHype(String, String)}
     */
    protected int percentageChange;

    protected Collection<String> tags = new ArrayList<String>();

    protected MusicEntry(String name, String url) {
        this(name, url, null, -1, -1, false);
    }

    protected MusicEntry(String name, String url, String mbid, int playcount, int listeners,
            boolean streamable) {
        this.name = name;
        this.url = url;
        this.mbid = mbid;
    }

    public String getMbid() {
        return mbid;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Collection<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + "name='" + name + '\'' + ", id='" + id
                + '\'' + ", url='" + url + '\'' + ", mbid='" + mbid + '\'' + ']';
    }

    /**
     * Loads all generic information from an XML <code>DomElement</code> into
     * the given <code>MusicEntry</code> instance, i.e. the following tags:<br/>
     * <ul>
     * <li>playcount/plays</li>
     * <li>listeners</li>
     * <li>streamable</li>
     * <li>name</li>
     * <li>url</li>
     * <li>mbid</li>
     * <li>image</li>
     * <li>tags</li>
     * </ul>
     * 
     * @param entry An entry
     * @param element XML source element
     */
    protected static void loadStandardInfo(MusicEntry entry, DomElement element) {
        // copy
        entry.name = element.getChildText("name");
        entry.url = element.getChildText("url");
        entry.mbid = element.getChildText("mbid");
        // images
        ImageHolder.loadImages(entry, element);
    }
}
