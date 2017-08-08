package com.spuriouslabs.apps.autoplex.plex.utils;


import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

/**
 * Created by omalleym on 7/19/17.
 */

public class BrowsableMenuItem extends MediaBrowserCompat.MediaItem
{
	protected final String title;
	protected final String key;
	protected final String icon_uri;

	public BrowsableMenuItem(final String title, final String key, final String icon_uri)
	{
		this(title, key, icon_uri, FLAG_BROWSABLE);
	}

	protected BrowsableMenuItem(final String title, final String key, final String icon_uri, int flag)
	{
		super((new MediaDescriptionCompat.Builder()).setTitle(title).setMediaId(key).build(), flag);

		this.title = title;
		this.key = key;
		this.icon_uri = icon_uri;
	}

	public boolean equals(BrowsableMenuItem other)
	{
		return (this.title.equals(other.title) &&
				this.key.equals(other.key) &&
				this.icon_uri.equals(other.icon_uri));
	}

	public String getTitle()
	{
		return title;
	}

	public String getIconUri()
	{
		return icon_uri;
	}
}
