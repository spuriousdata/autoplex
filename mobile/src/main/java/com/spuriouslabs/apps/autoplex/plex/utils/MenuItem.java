package com.spuriouslabs.apps.autoplex.plex.utils;

/**
 * Created by omalleym on 7/19/17.
 */

public class MenuItem {
	private final String title;
	private final String key;

	public MenuItem(final String title, final String key)
	{
		this.title = title;
		this.key = key;
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
}
