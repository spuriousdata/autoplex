package com.spuriouslabs.apps.autoplex;

import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.media.MediaBrowserService;
import android.util.Log;

import com.spuriouslabs.apps.autoplex.plex.Player;
import com.spuriouslabs.apps.autoplex.plex.PlexConnector;
import com.spuriouslabs.apps.autoplex.plex.PlexMusicProvider;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

	private MediaSession media_session;
	private PlexConnector connector;
	private boolean service_started;

	private Player player;
	private PlexMusicProvider provider;

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

		media_session = new MediaSession(this, "AutoPlexMusicService");
		setSessionToken(media_session.getSessionToken());
		media_session.setCallback(new MediaSessionCallback());
		media_session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
				MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

		connector = PlexConnector.getInstance(this);
		provider = new PlexMusicProvider(connector);
		player = new Player(this, provider);
	}

	@Override
	public void onDestroy()
	{
		media_session.release();
	}

	@Override
	public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints)
	{
		return new BrowserRoot("root", null);
	}

	@Override
	public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result)
	{
		result.sendResult(connector.getMenu(parentMediaId));
	}

	private final class MediaSessionCallback extends MediaSession.Callback
	{
		/*
		@Override
		public void onPlay()
		{
			Log.i("autoplex", "onPlay()");
		}

		@Override
		public void onSkipToQueueItem(long queueId)
		{
		}

		@Override
		public void onSeekTo(long position)
		{
		}*/

		@Override
		public void onPlayFromMediaId(String mediaId, Bundle extras)
		{
			Log.i("autoplex", "onPlayFromMediaId("+mediaId+")");
			connector.getMediaUri(mediaId);
		}

		/*
		@Override
		public void onPause()
		{
		}

		@Override
		public void onStop()
		{
		}

		@Override
		public void onSkipToNext()
		{
		}

		@Override
		public void onSkipToPrevious()
		{
		}
		*/

		@Override
		public void onCustomAction(String action, Bundle extras)
		{
			Log.i("autoplex", "invoked action: " + action);
		}

		@Override
		public void onPlayFromSearch(final String query, final Bundle extras)
		{
		}
	}
}
