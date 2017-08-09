package com.spuriouslabs.apps.autoplex.plex.utils;

import android.support.v4.media.session.MediaSessionCompat;

import com.spuriouslabs.apps.autoplex.plex.AutoPlexMusicProvider;

import java.util.List;

/**
 * Created by omalleym on 8/9/17.
 */

public class QueueHelper
{
	private static final String TAG = QueueHelper.class.getSimpleName();

	public static boolean indexIsPlayable(int index, List<MediaSessionCompat.QueueItem> queue)
	{
		return (queue != null && index >= 0 && index < queue.size());
	}
}
