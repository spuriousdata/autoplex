package com.spuriouslabs.apps.autoplex.plex.utils;

import android.support.annotation.IntDef;

import java.util.List;

import static android.media.browse.MediaBrowser.MediaItem.FLAG_BROWSABLE;
import static android.media.browse.MediaBrowser.MediaItem.FLAG_PLAYABLE;

/**
 * Created by omalleym on 7/19/17.
 */

public class MenuItem {
	private final String title;
	private final String key;
	private final int flag;

	@IntDef(flag=true, value = { FLAG_BROWSABLE, FLAG_PLAYABLE })
	public @interface Flags {}

	public MenuItem(final String title, final String key, @Flags int flag)
	{
		this.title = title;
		this.key = key;
		this.flag = flag;
	}

	public boolean equals(MenuItem other)
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

	public @Flags int getFlag()
	{
		return flag;
	}
}
