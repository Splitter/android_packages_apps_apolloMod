/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apolloMod.app.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.activities.AudioPlayerHolder;
import com.andrew.apolloMod.activities.MusicLibrary;
import com.andrew.apolloMod.service.ApolloService;
import static com.andrew.apolloMod.Constants.WIDGET_STYLE;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class AppWidget42 extends AppWidgetProvider {

    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate4x2";

    private static AppWidget42 sInstance;

    public static synchronized AppWidget42 getInstance() {
        if (sInstance == null) {
            sInstance = new AppWidget42();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);

        // Send broadcast intent to any running ApolloService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(ApolloService.SERVICECMD);
        updateIntent.putExtra(ApolloService.CMDNAME, AppWidget42.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    	String widget_type = sp.getString( WIDGET_STYLE, context.getResources().getString(R.string.widget_style_light) );

    	final RemoteViews views = new RemoteViews(context.getPackageName(),
                (widget_type.equals(context.getResources().getString(R.string.widget_style_light))?R.layout.fourbytwo_app_widget:R.layout.fourbytwo_app_widget_dark));
        

        linkButtons(context, views, false /* not playing */);
        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to
        // all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this
     * widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this
                .getClass()));
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link ApolloService}
     */
    public void notifyChange(ApolloService service, String what) {
        if (hasInstances(service)) {
            if (ApolloService.META_CHANGED.equals(what)
                    || ApolloService.PLAYSTATE_CHANGED.equals(what)
                    || ApolloService.REPEATMODE_CHANGED.equals(what)
                    || ApolloService.SHUFFLEMODE_CHANGED.equals(what)) {
                performUpdate(service, null);
            }
        }
    }

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(ApolloService service, int[] appWidgetIds) {
    	
    	Context mContext = service.getApplicationContext();
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    	String widget_type = sp.getString( WIDGET_STYLE, mContext.getResources().getString(R.string.widget_style_light) );

    	final RemoteViews views = new RemoteViews(mContext.getPackageName(),
                (widget_type.equals(mContext.getResources().getString(R.string.widget_style_light))?R.layout.fourbytwo_app_widget:R.layout.fourbytwo_app_widget_dark));
        
        CharSequence artistName = service.getArtistName();
        CharSequence albumName = service.getAlbumName();
        CharSequence trackName = service.getTrackName();
        views.setTextViewText(R.id.four_by_two_artistname, artistName);
        views.setTextViewText(R.id.four_by_two_albumname, albumName);
        views.setTextViewText(R.id.four_by_two_trackname, trackName);

        // Set album art
        Bitmap bitmap = service.getAlbumBitmap();
        if (bitmap != null) {
            views.setViewVisibility(R.id.four_by_two_albumart, View.VISIBLE);
            views.setImageViewBitmap(R.id.four_by_two_albumart, bitmap);
        } else {
            views.setViewVisibility(R.id.four_by_two_albumart, View.GONE);
        }

        // Set correct drawable and contentDescription for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.four_by_two_control_play,
            		 (widget_type.equals(mContext.getResources().getString(R.string.widget_style_light))?R.drawable.apollo_holo_light_pause:R.drawable.apollo_holo_dark_pause));
            views.setContentDescription(R.id.four_by_two_albumart,
                service.getResources().getString(R.string.nowplaying));
        } else {
            views.setImageViewResource(R.id.four_by_two_control_play,
            		(widget_type.equals(mContext.getResources().getString(R.string.widget_style_light))?R.drawable.apollo_holo_light_play:R.drawable.apollo_holo_dark_play));
            views.setContentDescription(R.id.four_by_two_albumart,
                service.getResources().getString(R.string.app_name));
        }

        // Set correct drawable for repeat state
        switch (service.getRepeatMode()) {
            case ApolloService.REPEAT_ALL:
                views.setImageViewResource(R.id.four_by_two_control_repeat,
                        R.drawable.apollo_holo_light_repeat_all);
                break;
            case ApolloService.REPEAT_CURRENT:
                views.setImageViewResource(R.id.four_by_two_control_repeat,
                        R.drawable.apollo_holo_light_repeat_one);
                break;
            default:
                views.setImageViewResource(R.id.four_by_two_control_repeat,
                		(widget_type.equals(mContext.getResources().getString(R.string.widget_style_light))?R.drawable.apollo_holo_light_repeat_normal:R.drawable.apollo_holo_dark_repeat_normal));
                break;
        }

        // Set correct drawable for shuffle state
        switch (service.getShuffleMode()) {
            case ApolloService.SHUFFLE_NONE:
                views.setImageViewResource(R.id.four_by_two_control_shuffle,
                		(widget_type.equals(mContext.getResources().getString(R.string.widget_style_light))?R.drawable.apollo_holo_light_shuffle_normal:R.drawable.apollo_holo_dark_shuffle_normal));
                break;
            case ApolloService.SHUFFLE_AUTO:
                views.setImageViewResource(R.id.four_by_two_control_shuffle,
                        R.drawable.apollo_holo_light_shuffle_on);
                break;
            default:
                views.setImageViewResource(R.id.four_by_two_control_shuffle,
                        R.drawable.apollo_holo_light_shuffle_on);
                break;
        }
        // Link actions buttons to intents
        linkButtons(service, views, playing);

        pushUpdate(service, appWidgetIds, views);

    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivity},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {

        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context, ApolloService.class);

        if (playerActive) {
            intent = new Intent(context, AudioPlayerHolder.class);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.four_by_two_albumart, pendingIntent);
            views.setOnClickPendingIntent(R.id.four_by_two_info, pendingIntent);
        } else {
            intent = new Intent(context, MusicLibrary.class);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.four_by_two_albumart, pendingIntent);
            views.setOnClickPendingIntent(R.id.four_by_two_info, pendingIntent);
        }

        intent = new Intent(ApolloService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.four_by_two_control_play, pendingIntent);

        intent = new Intent(ApolloService.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.four_by_two_control_next, pendingIntent);

        intent = new Intent(ApolloService.PREVIOUS_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.four_by_two_control_prev, pendingIntent);

        intent = new Intent(ApolloService.CYCLEREPEAT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.four_by_two_control_repeat, pendingIntent);

        intent = new Intent(ApolloService.TOGGLESHUFFLE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.four_by_two_control_shuffle, pendingIntent);
    }
}
