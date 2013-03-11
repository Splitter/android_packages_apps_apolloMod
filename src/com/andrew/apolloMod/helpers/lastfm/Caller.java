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

import static com.andrew.apolloMod.helpers.utils.StringUtilities.encode;
import static com.andrew.apolloMod.helpers.utils.StringUtilities.map;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.andrew.apolloMod.helpers.lastfm.Result.Status;

/**
 * The <code>Caller</code> class handles the low-level communication between the
 * client and last.fm.<br/>
 * Direct usage of this class should be unnecessary since all method calls are
 * available via the methods in the <code>Artist</code>, <code>Album</code>,
 * <code>User</code>, etc. classes. If specialized calls which are not covered
 * by the Java API are necessary this class may be used directly.<br/>
 * Supports the setting of a custom {@link Proxy} and a custom
 * <code>User-Agent</code> HTTP header.
 * 
 * @author Janni Kovacs
 */
public class Caller {

    private static final String PARAM_API_KEY = "api_key";

    private static final String DEFAULT_API_ROOT = "http://ws.audioscrobbler.com/2.0/";

    private static final Caller instance = new Caller();

    private final String apiRootUrl = DEFAULT_API_ROOT;

    private final String userAgent = "Apollo";

    private Result lastResult;

    private Caller() {
    }

    /**
     * Returns the single instance of the <code>Caller</code> class.
     * 
     * @return a <code>Caller</code>
     */
    public static Caller getInstance() {
        return instance;
    }

    public Result call(String method, String apiKey, String... params) throws RuntimeException {
        return call(method, apiKey, map(params));
    }

    public Result call(String method, String apiKey, Map<String, String> params)
            throws RuntimeException {
        return call(method, apiKey, params, null);
    }

    public Result call(String method, Session session, String... params) {
        return call(method, session.getApiKey(), map(params), session);
    }

    public Result call(String method, Session session, Map<String, String> params) {
        return call(method, session.getApiKey(), params, session);
    }

    /**
     * Performs the web-service call. If the <code>session</code> parameter is
     * <code>non-null</code> then an authenticated call is made. If it's
     * <code>null</code> then an unauthenticated call is made.<br/>
     * The <code>apiKey</code> parameter is always required, even when a valid
     * session is passed to this method.
     * 
     * @param method The method to call
     * @param apiKey A Last.fm API key
     * @param params Parameters
     * @param session A Session instance or <code>null</code>
     * @return the result of the operation
     */
    private Result call(String method, String apiKey, Map<String, String> params, Session session) {
        params = new HashMap<String, String>(params); // create new Map in case
                                                      // params is an immutable
                                                      // Map
        InputStream inputStream = null;

        // no entry in cache, load from web
        if (inputStream == null) {
            // fill parameter map with apiKey and session info
            params.put(PARAM_API_KEY, apiKey);
            if (session != null) {
                params.put("sk", session.getKey());
            }
            try {
                HttpURLConnection urlConnection = openPostConnection(method, params);
                inputStream = getInputStreamFromConnection(urlConnection);

                if (inputStream == null) {
                    this.lastResult = Result.createHttpErrorResult(urlConnection.getResponseCode(),
                            urlConnection.getResponseMessage());
                    return lastResult;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Result result = createResultFromInputStream(inputStream);
            this.lastResult = result;
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new {@link HttpURLConnection}, sets the proxy, if available,
     * and sets the User-Agent property.
     * 
     * @param url URL to connect to
     * @return a new connection.
     * @throws IOException if an I/O exception occurs.
     */
    public HttpURLConnection openConnection(String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection urlConnection;
        urlConnection = (HttpURLConnection)u.openConnection();
        urlConnection.setRequestProperty("User-Agent", userAgent);
        urlConnection.setUseCaches(true);
        return urlConnection;
    }

    private HttpURLConnection openPostConnection(String method, Map<String, String> params)
            throws IOException {
        HttpURLConnection urlConnection = openConnection(apiRootUrl);
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(true);
        OutputStream outputStream = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        String post = buildPostBody(method, params);
        writer.write(post);
        writer.close();
        return urlConnection;
    }

    private InputStream getInputStreamFromConnection(HttpURLConnection connection)
            throws IOException {
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_FORBIDDEN
                || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
            return connection.getErrorStream();
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        }

        return null;
    }

    private Result createResultFromInputStream(InputStream inputStream) throws SAXException,
            IOException {
        Document document = newDocumentBuilder().parse(
                new InputSource(new InputStreamReader(inputStream, "UTF-8")));
        Element root = document.getDocumentElement(); // lfm element
        String statusString = root.getAttribute("status");
        Status status = "ok".equals(statusString) ? Status.OK : Status.FAILED;
        if (status == Status.FAILED) {
            Element errorElement = (Element)root.getElementsByTagName("error").item(0);
            int errorCode = Integer.parseInt(errorElement.getAttribute("code"));
            String message = errorElement.getTextContent();
            return Result.createRestErrorResult(errorCode, message);
        } else {
            return Result.createOkResult(document);
        }
    }

    private DocumentBuilder newDocumentBuilder() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            return builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // better never happens
            throw new RuntimeException(e);
        }
    }

    private String buildPostBody(String method, Map<String, String> params, String... strings) {
        StringBuilder builder = new StringBuilder(100);
        builder.append("method=");
        builder.append(method);
        builder.append('&');
        for (Iterator<Entry<String, String>> it = params.entrySet().iterator(); it.hasNext();) {
            Entry<String, String> entry = it.next();
            builder.append(entry.getKey());
            builder.append('=');
            builder.append(encode(entry.getValue()));
            if (it.hasNext() || strings.length > 0)
                builder.append('&');
        }
        int count = 0;
        for (String string : strings) {
            builder.append(count % 2 == 0 ? string : encode(string));
            count++;
            if (count != strings.length) {
                if (count % 2 == 0) {
                    builder.append('&');
                } else {
                    builder.append('=');
                }
            }
        }
        return builder.toString();
    }
}
