
package com.andrew.apolloMod.app.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.service.ApolloService;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class AppWidget11 extends AppWidgetProvider {
    static final String TAG = "MusicAppWidgetProvider1x1";

    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate1x1";

    private static AppWidget11 sInstance;

    public static synchronized AppWidget11 getInstance() {
        if (sInstance == null) {
            sInstance = new AppWidget11();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);

        // Send broadcast intent to any running ApolloService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(ApolloService.SERVICECMD);
        updateIntent.putExtra(ApolloService.CMDNAME, AppWidget11.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.onebyone_app_widget);

        views.setImageViewResource(R.id.one_by_one_albumart, View.GONE);

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
                    || ApolloService.PLAYSTATE_CHANGED.equals(what)) {
                performUpdate(service, null);
            }
        }
    }

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(ApolloService service, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(service.getPackageName(),
                R.layout.onebyone_app_widget);

        // Set album art
        Bitmap bitmap = service.getAlbumBitmap();
        if (bitmap != null) {
            views.setViewVisibility(R.id.one_by_one_albumart, View.VISIBLE);
            views.setImageViewBitmap(R.id.one_by_one_albumart, bitmap);
        } else {
            views.setViewVisibility(R.id.one_by_one_albumart, View.INVISIBLE);
        }
        // Set correct contentDescription
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setContentDescription(R.id.one_by_one_albumart,
                service.getResources().getString(R.string.nowplaying));
        } else {
            views.setContentDescription(R.id.one_by_one_albumart,
                service.getResources().getString(R.string.app_name));
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

        intent = new Intent(ApolloService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.one_by_one_albumart, pendingIntent);

    }
}
