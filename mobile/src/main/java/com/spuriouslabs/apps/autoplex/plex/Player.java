package com.spuriouslabs.apps.autoplex.plex;

import android.content.Context;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.spuriouslabs.apps.autoplex.AutoPlexMusicService;
import com.spuriouslabs.apps.autoplex.plex.utils.PlayableMenuItem;

import java.io.IOException;

import static android.media.MediaPlayer.OnCompletionListener;
import static android.media.MediaPlayer.OnErrorListener;
import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.MediaPlayer.OnSeekCompleteListener;

/**
 * Created by omalleym on 7/28/17.
 *
 * A lof of this was copied from https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediabrowserservice/Playback.java
 */

public class Player implements AudioManager.OnAudioFocusChangeListener, OnCompletionListener,
		OnErrorListener, OnPreparedListener, OnSeekCompleteListener
{
	private static final String TAG = Player.class.getSimpleName();

	public interface Callback {
		void onCompletion();

		void onPlaybackStatusChanged(int state);

		void onError(String error);
	}

	// The volume we set the media player to when we lose audio focus, but are
	// allowed to reduce the volume instead of stopping playback.
	public static final float VOLUME_DUCK = 0.2f;
	// The volume we set the media player when we have audio focus.
	public static final float VOLUME_NORMAL = 1.0f;

	// we don't have audio focus, and can't duck (play at a low volume)
	private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
	// we don't have focus, but can duck (play at a low volume)
	private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
	// we have full audio focus
	private static final int AUDIO_FOCUSED = 2;

	private final AutoPlexMusicService music_service;
	private final AutoPlexMusicProvider provider;
	private int state = PlaybackState.STATE_NONE;
	private boolean play_on_focus_gain;
	private Callback callback;
	private Context ctx;

	private volatile int current_position;
	private volatile String current_media_id;

	private int audio_focus = AUDIO_NO_FOCUS_NO_DUCK;
	private AudioManager audio_manager;
	private MediaPlayer media_player;

	public Player(AutoPlexMusicService service, AutoPlexMusicProvider provider)
	{
		ctx = service.getApplicationContext();
		this.music_service = service;
		this.provider = provider;
		this.audio_manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
	}

	public void stop()
	{
		state = PlaybackState.STATE_STOPPED;
		if (callback != null)
			callback.onPlaybackStatusChanged(state);

		current_position = getCurrentStreamPosition();

		giveUpAudioFocus();

		relaxResources(true);
	}

	public int getState()
	{
		return state;
	}

	public boolean isConnected()
	{
		return true;
	}

	public boolean isPlaying()
	{
		return play_on_focus_gain || (media_player != null && media_player.isPlaying());
	}

	public int getCurrentStreamPosition()
	{
		return media_player != null ? media_player.getCurrentPosition() : current_position;
	}

	public void play(PlayableMenuItem track)
	{
		play_on_focus_gain = true;
		tryToGetAudioFocus();
		boolean media_has_changed = !TextUtils.equals(track.getMediaId(), current_media_id);
		if (media_has_changed) {
			state = PlaybackState.STATE_NONE;
			current_media_id = track.getMediaId();
		}

		if (state == PlaybackState.STATE_PAUSED
				&& !media_has_changed && media_player != null) {
			configMediaPlayerState();
		} else {
			state = PlaybackState.STATE_STOPPED;
			relaxResources(false); // release everything except MediaPlayer

			String source = provider.getUrlForMedia(track);
			try {
				createMediaPlayerIfNeeded();

				state = PlaybackState.STATE_BUFFERING;

				media_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				Log.d(TAG, "Setting media datasource to " + source);
				media_player.setDataSource(ctx, Uri.parse(source), provider.getPlexTokenHeaders());

				// Starts preparing the media player in the background. When
				// it's done, it will call our OnPreparedListener (that is,
				// the onPrepared() method on this class, since we set the
				// listener to 'this'). Until the media player is prepared,
				// we *cannot* call start() on it!
				media_player.prepareAsync();


				if (callback != null) {
					callback.onPlaybackStatusChanged(state);
				}

			} catch (IOException ioException) {
				Log.e(TAG, "Exception playing song", ioException);
				if (callback != null) {
					callback.onError(ioException.getMessage());
				}
			}
		}
	}

	public void pause()
	{
		Log.d(TAG, "Player.pause()");
		if (state == PlaybackState.STATE_PLAYING) {
			// Pause media player and cancel the 'foreground service' state.
			if (media_player != null && media_player.isPlaying()) {
				media_player.pause();
				current_position = media_player.getCurrentPosition();
			}
			// while paused, retain the MediaPlayer but give up audio focus
			relaxResources(false);
		}
		state = PlaybackState.STATE_PAUSED;
		if (callback != null) {
			callback.onPlaybackStatusChanged(state);
		}
	}

	public void seekTo(int position)
	{
		Log.d(TAG, "seekTo called with " + position);

		if (media_player == null) {
			// If we do not have a current media player, simply update the current position.
			current_position = position;
		} else {
			if (media_player.isPlaying()) {
				state = PlaybackState.STATE_BUFFERING;
			}
			media_player.seekTo(position);
			if (callback != null) {
				callback.onPlaybackStatusChanged(state);
			}
		}
	}

	public void setCallback(Callback callback)
	{
		this.callback = callback;
	}

	/**
	 * Try to get the system audio focus.
	 */
	private void tryToGetAudioFocus()
	{
		Log.d(TAG, "tryToGetAudioFocus");
		int result = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		audio_focus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
				? AUDIO_FOCUSED : AUDIO_NO_FOCUS_NO_DUCK;
	}

	/**
	 * Give up the audio focus.
	 */
	private void giveUpAudioFocus()
	{
		Log.d(TAG, "giveUpAudioFocus");
		if (audio_manager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			audio_focus = AUDIO_NO_FOCUS_NO_DUCK;
		}
	}

	/**
	 * Reconfigures MediaPlayer according to audio focus settings and
	 * starts/restarts it. This method starts/restarts the MediaPlayer
	 * respecting the current audio focus state. So if we have focus, it will
	 * play normally; if we don't have focus, it will either leave the
	 * MediaPlayer paused or set it to a low volume, depending on what is
	 * allowed by the current focus settings. This method assumes mPlayer !=
	 * null, so if you are calling it, you have to do so from a context where
	 * you are sure this is the case.
	 */
	private void configMediaPlayerState()
	{
		Log.d(TAG, "configMediaPlayerState. audio_focus=" + audio_focus);
		if (audio_focus == AUDIO_NO_FOCUS_NO_DUCK) {
			// If we don't have audio focus and can't duck, we have to pause,
			if (state == PlaybackState.STATE_PLAYING) {
				pause();
			}
		} else {  // we have audio focus:
			if (audio_focus == AUDIO_NO_FOCUS_CAN_DUCK) {
				media_player.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
			} else {
				if (media_player != null) {
					media_player.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
				} // else do something for remote client.
			}
			// If we were playing when we lost focus, we need to resume playing.
			if (play_on_focus_gain) {
				if (media_player != null && !media_player.isPlaying()) {
					Log.d(TAG, "configMediaPlayerState startMediaPlayer. seeking to "
							+ current_position);
					if (current_position == media_player.getCurrentPosition()) {
						media_player.start();
						state = PlaybackState.STATE_PLAYING;
					} else {
						media_player.seekTo(current_position);
						state = PlaybackState.STATE_BUFFERING;
					}
				}
				play_on_focus_gain = false;
			}
		}
		if (callback != null) {
			callback.onPlaybackStatusChanged(state);
		}
	}

	/**
	 * Called by AudioManager on audio focus changes.
	 * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}.
	 */
	@Override
	public void onAudioFocusChange(int focus_change)
	{
		Log.d(TAG, "onAudioFocusChange. focus_change=" + focus_change);
		if (focus_change == AudioManager.AUDIOFOCUS_GAIN) {
			// We have gained focus:
			audio_focus = AUDIO_FOCUSED;

		} else if (focus_change == AudioManager.AUDIOFOCUS_LOSS
				|| focus_change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
				|| focus_change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
			// We have lost focus. If we can duck (low playback volume), we can keep playing.
			// Otherwise, we need to pause the playback.
			boolean canDuck = focus_change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
			audio_focus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

			// If we are playing, we need to reset media player by calling configMediaPlayerState
			// with audio_focus properly set.
			if (state == PlaybackState.STATE_PLAYING && !canDuck) {
				// If we don't have audio focus and can't duck, we save the information that
				// we were playing, so that we can resume playback once we get the focus back.
				play_on_focus_gain = true;
			}
		} else {
			Log.e(TAG, "onAudioFocusChange: Ignoring unsupported focus_change: " + focus_change);
		}
		configMediaPlayerState();
	}

	/**
	 * Called when MediaPlayer has completed a seek.
	 *
	 * @see android.media.MediaPlayer.OnSeekCompleteListener
	 */
	@Override
	public void onSeekComplete(MediaPlayer player)
	{
		Log.d(TAG, "onSeekComplete from MediaPlayer:" + player.getCurrentPosition());
		current_position = player.getCurrentPosition();
		if (state == PlaybackState.STATE_BUFFERING) {
			media_player.start();
			state = PlaybackState.STATE_PLAYING;
		}
		if (callback != null) {
			callback.onPlaybackStatusChanged(state);
		}
	}

	/**
	 * Called when media player is done playing current song.
	 *
	 * @see android.media.MediaPlayer.OnCompletionListener
	 */
	@Override
	public void onCompletion(MediaPlayer player)
	{
		Log.d(TAG, "onCompletion from MediaPlayer");
		// The media player finished playing the current song, so we go ahead
		// and start the next.
		if (callback != null) {
			callback.onCompletion();
		}
	}

	/**
	 * Called when media player is done preparing.
	 *
	 * @see android.media.MediaPlayer.OnPreparedListener
	 */
	@Override
	public void onPrepared(MediaPlayer player)
	{
		Log.d(TAG, "onPrepared from MediaPlayer");
		// The media player is done preparing. That means we can start playing if we
		// have audio focus.
		configMediaPlayerState();
	}

	/**
	 * Called when there's an error playing media. When this happens, the media
	 * player goes to the Error state. We warn the user about the error and
	 * reset the media player.
	 *
	 * @see android.media.MediaPlayer.OnErrorListener
	 */
	@Override
	public boolean onError(MediaPlayer player, int what, int extra)
	{
		Log.e(TAG, "Media player error: what=" + what + ", extra=" + extra);
		if (callback != null) {
			callback.onError("MediaPlayer error " + what + " (" + extra + ")");
		}
		return true; // true indicates we handled the error
	}

	/**
	 * Makes sure the media player exists and has been reset. This will create
	 * the media player if needed, or reset the existing media player if one
	 * already exists.
	 */
	private void createMediaPlayerIfNeeded()
	{
		Log.d(TAG, "createMediaPlayerIfNeeded. needed? " + (media_player == null));
		if (media_player == null) {
			media_player = new MediaPlayer();

			// Make sure the media player will acquire a wake-lock while
			// playing. If we don't do that, the CPU might go to sleep while the
			// song is playing, causing playback to stop.
			media_player.setWakeMode(music_service.getApplicationContext(),
					PowerManager.PARTIAL_WAKE_LOCK);

			// we want the media player to notify us when it's ready preparing,
			// and when it's done playing:
			media_player.setOnPreparedListener(this);
			media_player.setOnCompletionListener(this);
			media_player.setOnErrorListener(this);
			media_player.setOnSeekCompleteListener(this);
		} else {
			media_player.reset();
		}
	}

	/**
	 * Releases resources used by the service for playback. This includes the
	 * "foreground service" status, the wake locks and possibly the MediaPlayer.
	 *
	 * @param release_media_player Indicates whether the Media Player should also
	 *                           be released or not.
	 */
	private void relaxResources(boolean release_media_player)
	{
		Log.d(TAG, "relaxResources. release_media_player=" + release_media_player);

		music_service.stopForeground(true);

		// stop and release the Media Player, if it's available
		if (release_media_player && media_player != null) {
			media_player.reset();
			media_player.release();
			media_player = null;
		}

	}
}
