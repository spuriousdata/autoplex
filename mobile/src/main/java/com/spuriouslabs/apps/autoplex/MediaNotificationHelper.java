package com.spuriouslabs.apps.autoplex;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.support.v4.media.session.MediaButtonReceiver;

/**
 * Created by omalleym on 7/28/17.
 *
 * this was copied directly from the example code
 */

public class MediaNotificationHelper
{
	private MediaNotificationHelper() {
		// Helper utility class; do not instantiate.
	}

	public static Notification createNotification(Context context,
												  MediaSession mediaSession)
	{
		MediaController controller = mediaSession.getController();
		MediaMetadata mMetadata = controller.getMetadata();
		PlaybackState mPlaybackState = controller.getPlaybackState();

		if (mMetadata == null || mPlaybackState == null) {
			return null;
		}

		boolean isPlaying = mPlaybackState.getState() == PlaybackState.STATE_PLAYING;
		Notification.Action action = isPlaying
				? new Notification.Action(R.drawable.ic_pause_white_24dp,
				context.getString(R.string.label_pause),
				MediaButtonReceiver.buildMediaButtonPendingIntent(context,
						PlaybackState.ACTION_PAUSE))
				: new Notification.Action(R.drawable.ic_play_arrow_white_24dp,
				context.getString(R.string.label_play),
				MediaButtonReceiver.buildMediaButtonPendingIntent(context,
						PlaybackState.ACTION_PLAY));

		MediaDescription description = mMetadata.getDescription();
		Bitmap art = description.getIconBitmap();
		if (art == null) {
			// use a placeholder art while the remote art is being downloaded.
			art = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ic_default_art);
		}

		Notification.Builder notificationBuilder = new Notification.Builder(context);
		notificationBuilder
				.setStyle(new Notification.MediaStyle()
						// show only play/pause in compact view.
						.setShowActionsInCompactView(new int[]{0})
						.setMediaSession(mediaSession.getSessionToken()))
				.addAction(action)
				.setSmallIcon(R.drawable.ic_notification)
				.setShowWhen(false)
				.setContentIntent(controller.getSessionActivity())
				.setContentTitle(description.getTitle())
				.setContentText(description.getSubtitle())
				.setLargeIcon(art)
				.setVisibility(Notification.VISIBILITY_PUBLIC);

		return notificationBuilder.build();
	}
}
