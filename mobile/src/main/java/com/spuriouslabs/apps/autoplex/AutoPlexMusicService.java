package com.spuriouslabs.apps.autoplex;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.spuriouslabs.apps.autoplex.plex.AutoPlexMusicProvider;
import com.spuriouslabs.apps.autoplex.plex.Player;
import com.spuriouslabs.apps.autoplex.plex.PlexConnector;
import com.spuriouslabs.apps.autoplex.plex.utils.PlayableMenuItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.media.session.PlaybackState.STATE_BUFFERING;
import static android.media.session.PlaybackState.STATE_CONNECTING;
import static android.media.session.PlaybackState.STATE_ERROR;
import static android.media.session.PlaybackState.STATE_FAST_FORWARDING;
import static android.media.session.PlaybackState.STATE_NONE;
import static android.media.session.PlaybackState.STATE_PAUSED;
import static android.media.session.PlaybackState.STATE_PLAYING;
import static android.media.session.PlaybackState.STATE_REWINDING;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_NEXT;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_PREVIOUS;
import static android.media.session.PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.media.session.PlaybackState.STATE_STOPPED;
import static com.spuriouslabs.apps.autoplex.plex.AutoPlexMusicProvider.MEDIA_ID_ROOT;

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 * <p>
 * To implement a MediaBrowserService, you need to:
 * <p>
 * <ul>
 * <p>
 * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
 * related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
 * {@link android.service.media.MediaBrowserService#onLoadChildren};
 * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
 * with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
 * <p>
 * <li> Set a callback on the
 * {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
 * The callback will receive all the user's actions, like play, pause, etc;
 * <p>
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 * {@link android.media.MediaPlayer})
 * <p>
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * {@link android.media.session.MediaSession#setPlaybackState(android.media.session.PlaybackState)}
 * {@link android.media.session.MediaSession#setMetadata(android.media.MediaMetadata)} and
 * {@link android.media.session.MediaSession#setQueue(java.util.List)})
 * <p>
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 * <p>
 * </ul>
 * <p>
 * To make your app compatible with Android Auto, you also need to:
 * <p>
 * <ul>
 * <p>
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 * <p>
 * </ul>
 *
 * @see <a href="README.md">README.md</a> for more details.
 */
public class AutoPlexMusicService extends MediaBrowserService
{
	private static final String TAG = AutoPlexMusicService.class.getSimpleName();
	private static final long STOP_DELAY = TimeUnit.SECONDS.toMillis(30);
	private static final int STOP_CMD = 0x7c48;
	public static final int NOTIFICATION_ID = 412;
	// Request code for starting the UI.
	private static final int REQUEST_CODE = 99;

	private MediaSession media_session;
	private boolean service_started;
	private PlayableMenuItem current_media = null;
	public NotificationManagerCompat notification_manager;

	private Player player;
	private AutoPlexMusicProvider provider;

	@IntDef({STATE_NONE, STATE_STOPPED, STATE_PAUSED, STATE_PLAYING, STATE_FAST_FORWARDING,
			STATE_REWINDING, STATE_BUFFERING, STATE_ERROR, STATE_CONNECTING,
			STATE_SKIPPING_TO_PREVIOUS, STATE_SKIPPING_TO_NEXT, STATE_SKIPPING_TO_QUEUE_ITEM})
	@Retention(RetentionPolicy.SOURCE)
	public @interface State {}

	private @State int playback_state;

	private Handler delayed_stop_handler = new Handler(new Handler.Callback(){
		@Override
		public boolean handleMessage(Message msg)
		{
			if (msg == null || msg.what != STOP_CMD)
				return false;
			if (player.isPlaying()) {
				Log.d(TAG, "Stopping Service");
				stopSelf();
				service_started = false;
			}
			return false;
		}
	});

	@Override
	public void onCreate()
	{
		super.onCreate();

		Context ctx = getApplicationContext();

		media_session = new MediaSession(this, TAG);
		setSessionToken(media_session.getSessionToken());
		media_session.setCallback(new AutoPlexMediaSessionCallback());
		media_session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
				MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);


		provider = AutoPlexMusicProvider.get_instance(PlexConnector.getInstance(ctx));
		player = new Player(this, provider);
		player.setCallback(new Player.Callback() {
			@Override
			public void onCompletion()
			{
				handleStopRequest();
			}

			@Override
			public void onPlaybackStatusChanged(int state)
			{
				updatePlaybackState(null);
			}

			@Override
			public void onError(String error)
			{
				updatePlaybackState(error);
			}
		});


		Intent intent = new Intent(ctx, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pi = PendingIntent.getActivity(ctx, REQUEST_CODE, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		media_session.setSessionActivity(pi);
		notification_manager = NotificationManagerCompat.from(this);

		updatePlaybackState(null);
	}

	@Override
	public int onStartCommand(Intent start_intent, int flags, int start_id)
	{
		Log.d(TAG, "onStartCommand()");

		MediaButtonReceiver.handleIntent(
				MediaSessionCompat.fromMediaSession(
					getApplicationContext(), media_session), start_intent);
		return super.onStartCommand(start_intent, flags, start_id);
	}

	@Override
	public void onDestroy()
	{
		Log.d(TAG, "onDestroy");
		// Service is being killed, so make sure we release our resources
		handleStopRequest();

		delayed_stop_handler.removeCallbacksAndMessages(null);

		// Always release the MediaSession to clean up resources
		// and notify associated MediaController(s).
		media_session.release();
	}

	@Override
	public BrowserRoot onGetRoot(String client_package_name, int client_uid, Bundle root_hints)
	{
		return new BrowserRoot(MEDIA_ID_ROOT, null);
	}

	@Override
	public void onLoadChildren(final String parent_media_id, final Result<List<MediaItem>> result)
	{
		Log.d(TAG, "onLoadChildren(" + parent_media_id + ")");

		if (!provider.hasMenuFor(parent_media_id)) {
			// this lets us call result.sendResult from another thread
			result.detach();

			provider.retrieveMediaAsync(parent_media_id, new AutoPlexMusicProvider.Callback()
			{
				@Override
				public void onMusicCatalogReady(boolean success)
				{
					loadChildren(parent_media_id, result);
					if (!success)
						updatePlaybackState("Error: No Metadata");
				}
			});
		} else {
			Log.d(TAG, "Menu cached, calling loadChildren(" + parent_media_id + ", result) directly");
			loadChildren(parent_media_id, result);
		}
	}

	private void loadChildren(String parent, final Result<List<MediaItem>> result)
	{
		Log.d(TAG, "loadChildren(" + parent + ", result)");
		List<MediaItem> l = provider.getMenu(parent);

		if (!l.isEmpty()) {
			Log.d(TAG, "Calling result.sendResult(provider.getMenu())");
			Log.d(TAG, "Menu length(): " + l.size());
			result.sendResult(l);
		}
	}

	private final class AutoPlexMediaSessionCallback extends MediaSession.Callback
	{

		@Override
		public void onPlay()
		{
			Log.d(TAG, "onPlay()");
			if (current_media != null)
				handlePlayRequest();
		}

		@Override
		public void onSeekTo(long position)
		{
			Log.d(TAG, "onSeekTo(" + position + ")");
			player.seekTo((int) position);
		}

		@Override
		public void onPlayFromMediaId(String media_id, Bundle extras)
		{
			Log.i(TAG, "onPlayFromMediaId(" + media_id + ", " + extras + ")");

			if ((current_media = provider.getMusic(media_id)) != null) {
				handlePlayRequest();
			}
		}

		@Override
		public void onPause()
		{
			Log.d(TAG, "onPause()");
			handlePauseRequest();
		}

		@Override
		public void onStop()
		{
			Log.d(TAG, "onStop()");
			handleStopRequest();
		}

		/*
		@Override
		public boolean onMediaButtonEvent(Intent media_button)
		{
			Log.d(TAG, "Got media_button press of some kind");
			return false;
		}
		*/
	}

	private void handlePlayRequest()
	{
		Log.d(TAG, "handlePlayRequest() state=" + player.getState());

		if (current_media == null)
			return;

		delayed_stop_handler.removeCallbacksAndMessages(null);
		if (!service_started) {
			Log.v(TAG, "Starting Service");

			startService(new Intent(getApplicationContext(), AutoPlexMusicService.class));
			service_started = true;
		}

		if (!media_session.isActive())
			media_session.setActive(true);

		updateMetadata();
		player.play(current_media);
	}

	private void handlePauseRequest()
	{
		Log.d(TAG, "handlePauseRequest() state=" + player.getState());

		player.pause();

		delayed_stop_handler.removeCallbacksAndMessages(null);
		delayed_stop_handler.sendEmptyMessageDelayed(STOP_CMD, STOP_DELAY);
	}

	private void handleStopRequest()
	{
		Log.d(TAG, "handleStopRequest() state=" + player.getState());

		player.stop();

		delayed_stop_handler.removeCallbacksAndMessages(null);
		delayed_stop_handler.sendEmptyMessage(STOP_CMD);

		updatePlaybackState(null);
	}

	private void updateMetadata()
	{
		media_session.setMetadata(current_media.getMetadata());

		/*
		if (current_media.getDescription().getIconBitmap() == null &&
				current_media.getDescription().getIconUri() != null) {
			fetchArtwork();
			postNotification();
		}
		*/
	}

	private void updatePlaybackState(String error)
	{
		Log.d(TAG, "updatePlaybackState(" + error + ") playback state=" + player.getState());

		long pos = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
		if (player != null && player.isConnected())
			pos = player.getCurrentStreamPosition();

		long playback_actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;
		if (player.isPlaying())
			playback_actions |= PlaybackState.ACTION_PAUSE;

		PlaybackState.Builder state_builder = new PlaybackState.Builder().setActions(playback_actions);

		playback_state = player.getState();

		if (error != null) {
			state_builder.setErrorMessage(error);
			playback_state = STATE_ERROR;
		}

		state_builder.setState(playback_state, pos, 1.0f, SystemClock.elapsedRealtime());

		if (current_media != null)
			state_builder.setActiveQueueItemId(new MediaSession.QueueItem(current_media.getDescription(), current_media.hashCode()).getQueueId());

		media_session.setPlaybackState(state_builder.build());

		if (playback_state == STATE_PLAYING) {
			Notification notification = postNotification();
			startForeground(NOTIFICATION_ID, notification);
		} else {
			if (playback_state == STATE_PAUSED)
				postNotification();
			else
				notification_manager.cancel(NOTIFICATION_ID);

			stopForeground(false);
		}
	}

	@Nullable
	private Notification postNotification()
	{
		Notification notification = MediaNotificationHelper.createNotification(this, media_session);
		if (notification == null)
			return null;

		notification_manager.notify(NOTIFICATION_ID, notification);
		return notification;
	}
}
