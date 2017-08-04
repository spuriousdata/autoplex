package com.spuriouslabs.apps.autoplex.plex.utils;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.support.annotation.IntDef;
import android.view.MenuItem;

import static android.media.browse.MediaBrowser.MediaItem.FLAG_BROWSABLE;
import static android.media.browse.MediaBrowser.MediaItem.FLAG_PLAYABLE;

/**
 * Created by omalleym on 7/19/17.
 */

public class BrowsableMenuItem extends MediaBrowser.MediaItem
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
		super((new MediaDescription.Builder()).setTitle(title).setMediaId(key).build(), flag);

		this.title = title;
		this.key = key;
		this.icon_uri = icon_uri;
	}

	public boolean equals(BrowsableMenuItem other)
	{
		return (this.title.equals(other.title) && this.key.equals(other.key));
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
