package com.spuriouslabs.apps.autoplex.plex.utils;

import android.support.annotation.IntDef;

import static android.media.browse.MediaBrowser.MediaItem.FLAG_BROWSABLE;
import static android.media.browse.MediaBrowser.MediaItem.FLAG_PLAYABLE;

/**
 * Created by omalleym on 7/19/17.
 */

public class BrowsableMenuItem
{
	protected final String title;
	protected final String key;
	protected final String icon_uri;

	@IntDef(flag=true, value = { FLAG_BROWSABLE, FLAG_PLAYABLE })
	public @interface Flags {}

	public BrowsableMenuItem(final String title, final String key, final String icon_uri)
	{
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

	public String getKey()
	{
		return key;
	}

	public String getIconUri()
	{
		return icon_uri;
	}

	public @Flags int getFlag()
	{
		return FLAG_BROWSABLE;
	}
}
