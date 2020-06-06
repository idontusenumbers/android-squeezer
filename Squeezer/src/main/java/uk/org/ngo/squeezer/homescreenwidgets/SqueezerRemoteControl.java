package uk.org.ngo.squeezer.homescreenwidgets;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.itemlist.HomeActivity;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.PlayerNotFoundException;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.event.PlayerStateChanged;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link SqueezerRemoteControlConfigureActivity SqueezerRemoteControlConfigureActivity}
 */
public class SqueezerRemoteControl extends AppWidgetProvider {

    private static final String TAG = SqueezerRemoteControl.class.getName();


    private static final String SQUEEZER_REMOTE_OPEN = "squeezeRemoteOpen";
    private static final String SQUEEZER_REMOTE_POWER = "squeezeRemotePower";
    private static final String SQUEEZER_REMOTE_PAUSE_PLAY = "squeezeRemotePausePlay";
    private static final String SQUEEZER_REMOTE_NEXT = "squeezeNext";
    private static final String SQUEEZER_REMOTE_PREVIOUS = "squeezePrevious";
    public static final String PLAYER_ID = "playerId";


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        String playerId = SqueezerRemoteControlConfigureActivity.loadPlayerId(context, appWidgetId);
        String playerName = SqueezerRemoteControlConfigureActivity.loadPlayerName(context, appWidgetId);

        // Construct the RemoteViews object

        // See the dimensions and
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);

        // Get min width and height.
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        RemoteViews views = getRemoteViews(context, minWidth, minHeight);
        appWidgetManager.updateAppWidget(appWidgetId, views);

//        views.setOnClickPendingIntent(R.id.squeezerRemote_playerButton, getPendingSelfIntent(context, SQUEEZER_REMOTE_POWER, playerId, SqueezerRemoteControl.class));

        Log.d(TAG, "wiring up widget for player " + playerName + " with id " + playerId);
        views.setTextViewText(R.id.squeezerRemote_playerButton, playerName);
        views.setOnClickPendingIntent(R.id.squeezerRemote_playerButton, getPendingSelfIntent(context, SQUEEZER_REMOTE_OPEN, playerId));
        views.setOnClickPendingIntent(R.id.squeezerRemote_power, getPendingSelfIntent(context, SQUEEZER_REMOTE_POWER, playerId));
        views.setOnClickPendingIntent(R.id.squeezerRemote_pausePlay, getPendingSelfIntent(context, SQUEEZER_REMOTE_PAUSE_PLAY, playerId));
        views.setOnClickPendingIntent(R.id.squeezerRemote_next, getPendingSelfIntent(context, SQUEEZER_REMOTE_NEXT, playerId));
        views.setOnClickPendingIntent(R.id.squeezerRemote_previous, getPendingSelfIntent(context, SQUEEZER_REMOTE_PREVIOUS, playerId));

        // Load button images; these are here instead of the XML because there's no way to use vectors for older API versions within XML
        views.setImageViewBitmap(R.id.squeezerRemote_next, Util.vectorToBitmap(context, R.drawable.ic_action_next));
        views.setImageViewBitmap(R.id.squeezerRemote_pausePlay, Util.vectorToBitmap(context, R.drawable.ic_action_play));
        views.setImageViewBitmap(R.id.squeezerRemote_power, Util.vectorToBitmap(context, R.drawable.ic_action_power_settings_new));
        views.setImageViewBitmap(R.id.squeezerRemote_previous, Util.vectorToBitmap(context, R.drawable.ic_action_previous));


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static PendingIntent getPendingSelfIntent(Context context, String action, String playerId) {
        Intent intent = new Intent(context, SqueezerRemoteControl.class);
        intent.setAction(action);
        intent.putExtra(PLAYER_ID, playerId);

        return PendingIntent.getBroadcast(context, playerId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        updateAppWidget(context, appWidgetManager, appWidgetId);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    /**
     * Determine appropriate view based on row or column provided.
     *
     * @param minWidth
     * @param minHeight
     * @return
     */
    private static RemoteViews getRemoteViews(Context context, int minWidth, int minHeight) {
        // First find out rows and columns based on width provided.
        int rows = getCellsForSize(minHeight);
        int columns = getCellsForSize(minWidth);
        // Now you changing layout base on you column count
        // In this code from 1 column to 4
        // you can make code for more columns on your own.
        switch (columns) {
            case 1:
                return new RemoteViews(context.getPackageName(), R.layout.squeezer_remote_control_1column);
            case 2:
                return new RemoteViews(context.getPackageName(), R.layout.squeezer_remote_control_2column);
            case 3:
                return new RemoteViews(context.getPackageName(), R.layout.squeezer_remote_control_3column);
            case 4:
                return new RemoteViews(context.getPackageName(), R.layout.squeezer_remote_control_4column);
            case 5:
            default:
                return new RemoteViews(context.getPackageName(), R.layout.squeezer_remote_control_5column);
        }
    }

    /**
     * Returns number of cells needed for given size of the widget.
     *
     * @param size Widget size in dp.
     * @return Size in number of cells.
     */
    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }

    public void runOnService(final Context context, final ServiceHandler handler) {

        IBinder service = peekService(context, new Intent(context, SqueezeService.class));

        if (service != null) {
            Log.i(TAG, "servicePeek found ISqueezeService");
            showToastExceptionIfExists(context, runHandlerAndCatchException(handler, (ISqueezeService) service));
        } else {

            boolean bound = context.getApplicationContext().bindService(new Intent(context, SqueezeService.class), new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service1) {
                    final ServiceConnection serviceConnection = this;

                    if (name != null && service1 instanceof ISqueezeService) {
                        Log.i(TAG, "onServiceConnected connected to ISqueezeService");
                        final ISqueezeService squeezeService = (ISqueezeService) service1;

                        // Might already be connected, try first
                        if (runHandlerAndCatchException(handler, squeezeService) == null) {
                            // Handler was called successfully; service no longer needed
                            context.unbindService(serviceConnection);
                        } else {
                            Log.i(TAG, "ISqueezeService probably wasn't connected, connecting...");
                            // Probably wasn't connected. Connect and try again
                            squeezeService.getEventBus().register(new Object() {
                                public void onEvent(Object event) {
                                    if (event instanceof PlayerStateChanged) {
                                        Log.i(TAG, "Reconnected, trying again");
                                        showToastExceptionIfExists(context, runHandlerAndCatchException(handler, squeezeService));
                                        squeezeService.getEventBus().unregister(this);
                                        // Handler was called successfully; service no longer needed
                                        context.unbindService(serviceConnection);
                                    }
                                }
                            });
                            squeezeService.startConnect();
                        }
                    }
                }

                public void onServiceDisconnected(ComponentName name) {

                }
            }, Context.BIND_AUTO_CREATE);

            if (!bound)
                Log.e(TAG, "Squeezer service not bound");
        }
    }

    private void showToastExceptionIfExists(Context context, @Nullable Exception possibleException) {
        if (possibleException != null) {
            Toast.makeText(context, possibleException.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private @Nullable
    Exception runHandlerAndCatchException(ServiceHandler handler, ISqueezeService squeezeService) {
        try {
            handler.run(squeezeService);
            return null;
        } catch (Exception ex) {
            Log.e(TAG, "Exception while handling serviceHandler", ex);
            return ex;
        }
    }

    public void runOnPlayer(final Context context, final String playerId, final ServicePlayerHandler handler) {
        runOnService(context, service -> handler.run(service, service.getPlayer(playerId)));
    }

    @FunctionalInterface
    private interface ServiceHandler {
        void run(ISqueezeService service) throws Exception;
    }

    @FunctionalInterface
    private interface ServicePlayerHandler {
        void run(ISqueezeService service, Player player) throws Exception;
    }

    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        final String playerId = intent.getStringExtra(PLAYER_ID);


        Log.d(TAG, "recieved intent with action " + action + " and playerid " + playerId);

        if (SQUEEZER_REMOTE_OPEN.equals(action)) {
            runOnService(context, service -> {
                Log.d(TAG, "setting active player: " + playerId);
                service.setActivePlayer(service.getPlayer(playerId));
                Handler handler = new Handler();
                float animationDelay = Settings.Global.getFloat(context.getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
                handler.postDelayed(() -> HomeActivity.show(context), (long) (300 * animationDelay));
            });

        } else if (SQUEEZER_REMOTE_POWER.equals(action)) {
            runOnPlayer(context, playerId, ISqueezeService::togglePower);
        } else if (SQUEEZER_REMOTE_PAUSE_PLAY.equals(action)) {
            runOnPlayer(context, playerId, ISqueezeService::togglePausePlay);
        } else if (SQUEEZER_REMOTE_NEXT.equals(action)) {
            runOnPlayer(context, playerId, ISqueezeService::nextTrack);
        } else if (SQUEEZER_REMOTE_PREVIOUS.equals(action)) {
            runOnPlayer(context, playerId, ISqueezeService::previousTrack);
        }
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            SqueezerRemoteControlConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

}
